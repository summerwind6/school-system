# 高校选课系统 Redis 优化与并发升级方案

状态：代码已实现，生产数据核验与容量压测待执行

适用范围：学生浏览课程、选课与退课，以及课程容量变更

## 1. 目标与现状

抢课高峰有两类负载。大量学生同时刷新页面会放大课程查询流量。大量学生提交同一门课会竞争 MySQL 中同一行的更新锁。Redis 用于削减第一类负载，选课结果由 MySQL 事务裁决。两条链路分别压测，以便判断瓶颈出在哪里。

| 现状 | 升级决策 |
| --- | --- |
| 学生选课页请求 `size=1000`，后端最大允许 `100`，前端还要过滤开放状态并自行分页。 | 增加学生可选课程分页查询，筛选和分页由 SQL 完成。 |
| `reserveSeat` 在条件 `selected_count < capacity` 成立时原子占座，选课记录与占座同事务提交。 | 沿用这条写入链路，通过真实 MySQL 并发测试验证。 |
| 建表脚本有容量 `CHECK`、人数范围 `CHECK` 和选课唯一键。课程编辑 SQL 可以直接尝试写入新容量。 | 核验已有部署实际执行这些约束。应用层增加原子降容条件和明确的冲突提示。 |
| 课表冲突检查发生在占座前。同一学生并发选两门冲突课，可能各自通过检查。 | 选退课事务先锁定该学生记录，再读取其选课状态。 |

现有入口：[学生选课页](../frontend/src/views/selection/SelectCourse.vue) · [课程服务](../src/main/java/com/school/system/service/impl/CourseServiceImpl.java) · [选退课服务](../src/main/java/com/school/system/service/impl/CourseSelectionServiceV2.java) · [课程 SQL](../src/main/resources/mapper/CourseMapper.xml) · [数据库初始化脚本](../--%20创建数据库.sql)

## 2. 学生课程查询

建议新增 `GET /api/course/selectable`，由 Shiro 校验学生角色。请求复用 `current` 和 `size`，支持课程名称、院系及学分区间筛选。SQL 固定加上 `audit_status = 1 AND status = 1`，按 `create_time DESC, course_id DESC` 排序，再执行分页。查询记录与总数使用相同的筛选条件。用代表性数据运行 `EXPLAIN`，再决定是否为状态与排序增加组合索引。

前端直接使用接口返回的 `records` 和 `total`。查询条件改变时回到第一页，翻页或改变页大小时请求对应数据。院系列表来自独立的字典查询，学生端只可读取允许展示的院系字段，沿用现有老师和管理员接口的权限边界。

缓存内容采用查询 DTO。课程页缓存基础字段和总数，当前页若需展示容量、已选人数或余量，则按课程 ID 一次批量查 MySQL，查询失败时将余量展示为未知。选课结果以提交接口响应为准。现有编辑页从数据库读取课程详情。

## 3. Redis 读缓存

### 3.1 接入与范围

项目的 Spring Boot 版本为 2.7.14。在 `pom.xml` 增加 `spring-boot-starter-data-redis`，使用默认 Lettuce 客户端。用 `spring.redis.*` 配置连接和超时，凭据由环境注入。用 `app.cache.enabled` 控制缓存读写，默认关闭。缓存 key 包含应用、环境和结构版本，值采用明确类型的 JSON DTO。

| 缓存项 | Key 示例 | 初始过期时间与范围 |
| --- | --- | --- |
| 院系字典 | `school:prod:v1:department:list` | 30 分钟，加少量随机抖动。仅当不同角色看到的字典范围相同时共用。 |
| 可选课程首页 | `school:prod:v1:course:selectable:page:1:10` | 60 秒，随机浮动 15 秒。先覆盖无筛选的默认首页，其他分页和搜索走有并发上限的数据库查询。 |
| 学生端课程详情 | `school:prod:v1:course:detail:123` | 如后续有学生详情入口且压测显示高复用，启用 5 分钟缓存，随机浮动 1 分钟。 |
| 课程详情空结果 | 与学生详情共用 key，值为显式空标记 | 15～30 秒，只在查询成功且确认为空时写入。 |

课程目录先修查询再测基线。院系字典用于试点，首页缓存根据命中率和数据库减压效果启用。空结果与详情缓存随学生详情入口启用。TTL 是压测起始值。

### 3.2 三类缓存故障

| 故障 | 防护 | 故障时的行为 |
| --- | --- | --- |
| 穿透 | 校验 ID 和分页参数，Shiro 鉴权后再读缓存。重复查询缺失课程时使用短 TTL 空标记。对不断变换 ID 的流量按账号和 IP 限流，给空标记写入设预算。 | 数据库超时或失败返回错误。超过空标记写入预算时跳过写入，回源仍受读请求并发预算限制。 |
| 击穿 | 开抢前分批预热已启用的热门 key。每个 key 用互斥锁合并回源，拿锁后再次读缓存。 | 未拿锁的请求带随机退避重读，超过等待预算后返回繁忙。 |
| 雪崩 | TTL 加抖动，Redis 使用短超时。连续失败时短暂熔断并控制数据库回源。 | 按读请求类别限流并限制数据库并发，读流量超过预算时快速失败，给选退课留出处理能力。Redis 恢复后分批预热。 |

### 3.3 热 key 回源互斥锁

1. 读取缓存。命中正常值或空标记时直接返回相应结果。
2. 未命中时尝试获得该 key 的回源锁。单实例可用 JVM 内按 key 合并请求，多实例使用 `SET <lockKey> <随机持有者标识> NX PX <租约毫秒数>`。锁 key 与缓存 key 一一对应。
3. 获取锁后再次读缓存。仍未命中时，在读并发预算内查 MySQL。回填前确认仍持有锁，使用 Redis Lua 原子检查持有者并写入，防止过期持有者覆盖后来请求的结果。
4. 在 `finally` 中用 Lua 原子比较持有者标识后删除锁。未拿锁的请求只在等待预算内重读，超时后返回繁忙。若 Redis 操作异常，则进入有并发上限的回源路径。

回源查询超时要短于锁租约，等待预算按接口延迟目标设置。锁因超时或故障切换丢失时仍可能出现重复回源，因此数据库读限额是最后一层保护。回源锁只协调缓存重建。

### 3.4 数据更新与故障降级

- 课程新增、编辑、删除、审核和上下架在 MySQL 提交后删除首页及受影响的详情 key。新增或重新开放课程时一并清理对应的空标记。可使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`，监听器记录 Redis 异常和失效失败。
- 删除失败或并发回填可能造成短暂的旧目录。短 TTL 限制残留时间，选课提交实时校验课程开放状态和容量。通过脚本修改院系后，维护脚本或操作流程负责清理院系字典 key。
- 读请求获取并发许可后才进入使用数据库连接的代码，限制等待时间并在完成后释放许可。各实例的读预算按实例数量折算，给提交、退课和其他业务预留连接。限流响应使用 `429`，过载或暂时无法服务时使用 `503`，前端提示稍后重试并避免自动密集重试。

## 4. 名额一致性与选退课

### 4.1 原子占座与事务边界

现有 `reserveSeat` 的核心条件如下。代码中的 `version` 更新也随占座一起执行。

```sql
UPDATE course
SET selected_count = selected_count + 1,
    version = version + 1
WHERE course_id = :course_id
  AND audit_status = 1
  AND status = 1
  AND selected_count < capacity;
```

只有影响一行才继续新增或恢复选课记录。`(student_id, course_id, semester, year)` 唯一键负责防重，后续写入失败使整笔事务回滚。退课先确认归属和有效状态，只对实际取消的记录释放一次名额，任一步失败都回滚。前端超时后可刷新“我的已选”确认状态，重复提交按当前数据库状态返回。

当前名额按 `course_id` 汇总，选课记录含学期和学年。验收按该课程的所有有效记录对账。如果业务要求同一个 `course_id` 每学期独立计容，需另行定义开课实例并将容量、计数和选课记录绑定到实例。

### 4.2 容量变更

初始化脚本已声明 `capacity > 0` 和 `0 <= selected_count <= capacity`。上线前用 `SHOW CREATE TABLE` 核验现有库实际拥有并执行这些约束和选课唯一键。对照有效选课记录核验已有计数，如发现偏差，在维护窗口核对并修复后再补齐约束。

课程编辑在同一条 `UPDATE` 中加入 `新容量 >= selected_count` 的条件，给拒绝降容的请求返回明确的业务冲突。编辑时未传容量则沿用数据库当前值。审核和上下架改用仅更新各自负责字段的 SQL，避免用此前读取的整行对象覆盖并发编辑的容量或其他字段。

### 4.3 同一学生的并发选退课

在 `CourseSelectionServiceV2` 的写事务内，用登录用户 ID 执行 `SELECT ... FOR UPDATE` 取得学生行，并把它放在事务中首次普通读取之前。拿锁后读取当前的选课记录和排课，避免在 MySQL 默认 `REPEATABLE READ` 下沿用等待前建立的读快照。选课和退课都采用先锁学生、再执行业务读写的顺序。锁由事务提交或回滚释放。

对选课高峰单独设置提交速率和并发预算，入口按账号限制重复点击，在连接池耗尽前拒绝超量请求。若修复和限流后，测得写入瓶颈仍集中在热门课程行锁，再评估按课程分流的有界队列。届时需区分“已受理”和“已选上”，并定义队列容量、去重及状态查询。

## 5. 实施顺序与回退

| 阶段 | 改动位置 | 验收后进入下一阶段 |
| --- | --- | --- |
| 1. 查询修复与基线 | `CourseController`、`CourseService`、`CourseMapper.xml`、`frontend/src/api/index.js`、`SelectCourse.vue` | 学生分页和筛选正确，拿到同一套压测流量的延迟、SQL 数量及连接池基线。 |
| 2. 事务并发保护 | `CourseSelectionServiceV2`、`StudentMapper.xml`、`CourseServiceImpl`、`CourseMapper.xml` | 真实 MySQL 下通过超售、重复选退课、课表冲突及并发降容验证。 |
| 3. 缓存试点与限流 | `pom.xml`、Redis 配置、院系字典查询服务、课程查询服务、事务提交后的缓存失效监听器 | 院系缓存通过故障演练。首页缓存先预热再小流量启用，观察读放大是否减少及提交是否受影响。 |

缓存回退时关闭开关，学生查询走原有的有界数据库回源逻辑。服务端分页、事务保护和入口限流继续生效。滚动上线时先部署后端分页接口，再切换前端调用。

## 6. 压测与验收

测试使用独立的真实 MySQL 和 Redis。以相同数据、并发模型和连接池配置比较缓存关闭、预热及故障场景。若生产有多实例，击穿和限流测试使用至少两个应用实例。

| 场景 | 必须满足 |
| --- | --- |
| 50 个空座，1,000 名不同学生并发请求同一门课 | 成功提交最多 50 人，`selected_count` 等于实际有效记录数。另测关闭限流、无故障且至少 50 个有效请求完成事务竞争时，可用座位全部占满。 |
| 同一学生重复选课或重复退课、占座后写记录失败 | 仅一次有效占座或释放，异常回滚后计数一致。 |
| 同一学生并发提交两门时间冲突的课 | 最多一门选中。不同学生可以正常并发。 |
| 抢课期间修改容量 | 更新与占座结束后始终满足 `0 <= selected_count <= capacity`，低于当前人数的降容有明确冲突提示。 |
| 热 key 过期并发刷新 | 正常租约窗口内，同一 key 由一个请求重建。模拟持有者超时，旧持有者无法覆盖或删除新持有者的结果。 |
| 请求不存在的 ID，或大量不同的缺失 ID | 重复 ID 命中空标记。不同 ID 的回源和空标记写入受预算控制。模拟 MySQL 超时，确认没有写入空标记。 |
| 批量 key 过期、Redis 断开和恢复 | 读回源并发受限，选退课仍可用且满足一致性约束。观察恢复期间的连接池等待和错误率。 |
| 课程更新提交、回滚或 Redis 删除失败 | 提交成功清缓存，回滚不触发失效。删除失败可观测，数据在配置的过期周期内重新读取。 |

记录课程查询和选课提交各自的吞吐、P95/P99 延迟、超时率和限流率，同时记录缓存命中、回源次数、回源锁等待、缓存失效失败、MySQL 活跃连接和行锁等待。业务不变量要求全部通过。性能门槛由基线与预期峰值确定，当前设计尚无压测结果。

## 7. 启用与复测

部署顺序为后端、前端、院系字典试点、可选课程首页试点。学生端通过 `GET /api/department/options` 获取院系 ID 和名称，通过 `GET /api/course/selectable` 获取分页结果。首页只展示课程基础信息。

| 配置 | 默认值 | 用途 |
| --- | --- | --- |
| `CACHE_ENABLED` | `false` | 总开关。设为 `true` 后启用院系字典缓存。 |
| `CACHE_SELECTABLE_HOME_ENABLED` | `false` | 总开关开启时，缓存每页 10 条、无筛选的第一页。 |
| `CACHE_ENVIRONMENT` | `local` | Redis key 的环境标识。同一环境的应用实例共用该标识。 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis 服务地址。ACL 凭据通过 `REDIS_USERNAME` 和 `REDIS_PASSWORD` 注入。 |
| `DB_POOL_MAX_WAIT_MS` | `1000` | Druid 获取连接的最长等待时间。 |

上线前用学生会话依次请求院系字典和已启用的首页，完成预热。Redis 恢复后可用相同步骤重新预热。通过 SQL 修改院系或影响目录展示的教师姓名后，分别删除 `school:<环境>:v1:department:list` 或 `school:<环境>:v1:course:selectable:page:1:10`，并在同一条 `DEL` 命令中带上对应的 `:lock` key。课程管理接口在事务提交后自动失效首页。回退时关闭总开关并重启应用实例。

学生读写预算通过 `app.limits.*` 配置。每实例的课程目录、院系、已选与冲突检查、选退课分别默认允许 6、2、2、6 个请求并行，包含 Shiro 角色查询。各类请求速率上限分别为每秒 120、60、60、60，`*-per-second=0` 可在隔离压测中停用相应速率上限。相同账号在单实例中同时只处理一笔选退课请求，跨实例一致性由 MySQL 学生行锁保证。

在独立测试库导入 [初始化脚本](../--%20创建数据库.sql)，再执行以下 PowerShell 命令。根据测试环境设置 `UPGRADE_TEST_DB_USERNAME`、`UPGRADE_TEST_DB_PASSWORD` 和 Redis 凭据。

```powershell
$env:UPGRADE_TEST_DB_URL = 'jdbc:mysql://127.0.0.1:13306/school_system?useSSL=false&allowPublicKeyRetrieval=true'
$env:UPGRADE_TEST_REDIS_PORT = '16379'
$env:UPGRADE_TEST_CACHE = 'true'
mvn test

$env:UPGRADE_TEST_CACHE = 'false'
mvn test
```

未设置测试库 URL 时，Maven 仅运行单元测试。集成测试使用 16 个工作线程提交 1,000 名不同学生的请求，检查 50 个名额全部占满并与有效记录对账。测试还覆盖选退课幂等冲突、失败回滚、冲突排课、并发降容、学生 HTTP 权限和 Redis 回源锁。两套缓存实例共享同一 Redis 的测试用于验证回源锁，生产多实例压测仍按实际部署拓扑执行。

发布前使用真实 MySQL 和 Redis 复测，缓存开启时 17 项通过，关闭时 13 项通过并跳过 4 项 Redis 专属测试。Chrome 上使用前端生产构建验证了管理员登录及容量修改，以及学生登录、翻页、搜索、选课、刷新、退课和名额对账。

部署前在目标库检查约束与计数，以下对账查询应返回空结果。

```sql
SHOW CREATE TABLE student;
SHOW CREATE TABLE course;
SHOW CREATE TABLE course_selection;

SELECT c.course_id, c.capacity, c.selected_count, COUNT(s.selection_id) AS actual_count
FROM course c
LEFT JOIN course_selection s ON s.course_id = c.course_id AND s.status = 1
GROUP BY c.course_id, c.capacity, c.selected_count
HAVING c.selected_count <> actual_count
    OR c.selected_count < 0 OR c.selected_count > c.capacity OR c.capacity <= 0;
```

在受控环境配置 `MANAGEMENT_ENDPOINTS=health,metrics` 可查看 `http.server.requests`、`school.catalog.cache`、`school.catalog.cache.wait`、`school.catalog.database` 和 `school.student.rejected`。MySQL 侧使用 Performance Schema 对照 SQL 执行量、活跃连接和行锁等待。使用目标数据量分别对无筛选首页及常见筛选执行 `EXPLAIN`，再依据结果决定是否增加索引。学生课程详情入口启用后再验收空值缓存及缺失 ID 的限流。

本机以 MySQL 8.0.22、Redis 7.4.2、2,008 门课程和 4 路并发，每种状态请求首页 200 次。各组请求均返回 HTTP 200，目录 SQL 统计只计入课程查询和总数查询。测试期间暂时关闭目录的每秒速率上限。

| Redis 状态 | 请求/秒 | P95 | P99 | 目录 SQL 次数 |
| --- | ---: | ---: | ---: | ---: |
| 关闭缓存 | 257.1 | 20.63 ms | 22.79 ms | 398 |
| 已预热 | 365.2 | 15.08 ms | 20.51 ms | 0 |
| Redis 断开 | 174.8 | 35.15 ms | 181.10 ms | 400 |
| 恢复并预热 | 361.1 | 17.43 ms | 19.29 ms | 0 |

本机首页 `EXPLAIN` 显示课程表扫描和排序。索引选择与生产峰值门槛仍需结合目标数据的状态分布、常用筛选和实际实例数复测。

## 参考资料

- [Spring Boot 2.7.14 Redis 配置](https://docs.spring.io/spring-boot/docs/2.7.14/reference/html/data.html#data.nosql.redis)
- [Redis Cache-Aside](https://redis.io/docs/latest/develop/use-cases/cache-aside/) · [Redis SET 命令](https://redis.io/docs/latest/commands/set/)
- [Spring 事务事件](https://docs.spring.io/spring-framework/docs/5.3.29/reference/html/data-access.html#transaction-event)
- [MySQL InnoDB 锁定读](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html) · [MySQL CHECK 约束](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html)
