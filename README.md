# 高校课程管理系统

基于 Spring Boot、Vue 3、MyBatis、Shiro 和 MySQL 的高校课程选课系统。当前重点是保障选课核心链路的权限边界、排课规则和并发一致性。

## 🎯 系统功能

### 1. 用户认证与权限管理
- **多角色登录**：学生、教师、管理员
- **基于Shiro的RBAC权限控制**：菜单级、按钮级权限控制
- **认证会话**：Shiro 会话管理，30 分钟过期
- **会话管理**：30分钟自动过期

### 2. 课程管理（新增审核机制）
- **课程发布**：教师可发布新课程
- **课程审核**：管理员审核课程，审核通过后学生方可选课
- **课程状态**：启用/停用状态管理
- **课程查询**：支持按名称、院系、学分筛选

### 3. 选课管理
- **身份归属**：后端从当前登录会话关联学生身份，不信任客户端传入的 `studentId`
- **容量一致性**：以数据库条件更新原子占座，退课软取消并在同一事务内释放名额
- **冲突检测**：按周次、单双周、星期和节次判定冲突
- **退课功能**：学生只能退选自己的课程，历史记录保留为“已取消”
- **选课记录**：查看个人选课历史

### 4. 成绩管理
- **成绩录入**：教师录入平时成绩、期末成绩
- **自动计算**：总评成绩自动计算（当前规则为平时 30% + 期末 70%）
- **成绩管理**：修改、删除成绩记录
- **成绩导出**：Excel格式导出成绩单

## 🛠️ 技术架构

### 后端技术栈
- **框架**：SpringBoot 2.7.14
- **安全框架**：Apache Shiro 1.10.1
- **ORM**：MyBatis 2.3.1
- **数据库**：MySQL 8.0
- **连接池**：Druid 1.2.18
- **文档处理**：Apache POI 5.2.3
- **开发工具**：Lombok

### 前端技术栈
- **框架**：Vue.js 3 + Element Plus
- **构建工具**：Vite 5.4.21
- **路由管理**：Vue Router 4
- **状态管理**：Vue reactive store
- **HTTP客户端**：Axios

## 📁 项目结构

```
schoolsystem/
├── src/main/java/com/school/system/     # 后端Java代码
│   ├── common/                          # 通用类
│   │   ├── Result.java                  # 统一响应格式
│   │   └── PageResult.java              # 分页响应格式
│   ├── config/                          # 配置类
│   │   ├── ShiroConfig.java             # Shiro安全配置
│   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   ├── controller/                      # 控制器层
│   │   ├── AuthController.java          # 认证相关接口
│   │   ├── CourseController.java        # 课程管理接口
│   │   ├── CourseSelectionController.java # 选课管理接口
│   │   ├── ScoreController.java         # 成绩管理接口
│   │   └── ExportController.java        # 导出接口
│   ├── dto/                             # 数据传输对象
│   │   ├── LoginDTO.java                # 登录DTO
│   │   ├── RegisterDTO.java             # 注册DTO
│   │   ├── CourseDTO.java               # 课程DTO
│   │   ├── CourseSelectionDTO.java      # 选课DTO
│   │   └── ScoreDTO.java                # 成绩DTO
│   ├── entity/                          # 实体类
│   │   ├── SysUser.java                 # 用户实体
│   │   ├── Course.java                  # 课程实体
│   │   ├── CourseSelection.java         # 选课记录实体
│   │   └── Score.java                   # 成绩实体
│   ├── mapper/                          # MyBatis Mapper接口
│   │   ├── CourseMapper.java            # 课程Mapper
│   │   ├── CourseSelectionMapper.java   # 选课Mapper
│   │   └── ScoreMapper.java             # 成绩Mapper
│   ├── service/                         # 服务层
│   │   ├── CourseService.java           # 课程服务
│   │   ├── CourseSelectionService.java  # 选课服务
│   │   ├── ScoreService.java            # 成绩服务
│   │   └── impl/                        # 服务实现
│   │       ├── CourseServiceImpl.java   # 课程服务实现
│   │       ├── CourseSelectionServiceImpl.java # 选课服务实现
│   │       └── ScoreServiceImpl.java    # 成绩服务实现
│   └── SchoolSystemApplication.java     # 启动类
├── src/main/resources/                  # 资源文件
│   ├── mapper/                          # MyBatis XML映射
│   │   ├── CourseMapper.xml             # 课程SQL映射
│   │   ├── CourseSelectionMapper.xml    # 选课SQL映射
│   │   └── ScoreMapper.xml              # 成绩SQL映射
│   ├── application.yml                  # 应用配置
│   └── -- 创建数据库.sql                 # 数据库初始化脚本
├── frontend/                            # 前端项目
│   ├── src/
│   │   ├── views/                       # 页面组件
│   │   │   ├── Home.vue                 # 主页面
│   │   │   ├── Login.vue                # 登录页
│   │   │   ├── course/                  # 课程管理页面
│   │   │   │   ├── CourseList.vue       # 课程列表
│   │   │   │   ├── AddCourse.vue        # 添加课程
│   │   │   │   ├── EditCourse.vue       # 编辑课程
│   │   │   │   └── CourseAudit.vue      # 课程审核（新增）
│   │   │   ├── selection/               # 选课管理页面
│   │   │   │   ├── SelectionList.vue    # 选课记录
│   │   │   │   └── SelectCourse.vue     # 选课中心
│   │   │   └── score/                   # 成绩管理页面
│   │   │       ├── ScoreList.vue        # 成绩列表
│   │   │       ├── AddScore.vue         # 添加成绩
│   │   │       └── EditScore.vue        # 编辑成绩
│   │   ├── router/                      # 路由配置
│   │   ├── api/                         # API接口
│   │   └── store/                       # 状态管理
│   └── package.json                     # 前端依赖配置
└── pom.xml                              # Maven配置
```

## 🚀 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Node.js 16+
- npm 8+

### 2. 数据库初始化

创建空数据库后，执行项目根目录的 [-- 创建数据库.sql](<E:/JAVA/schoolsystem/-- 创建数据库.sql>)。脚本会按外键依赖顺序创建表、排课数据与默认账号。

### 3. 后端启动

```bash
# 进入项目目录
cd e:\JAVA\schoolsystem

# 先配置数据库凭据（任选一种）
# 1. 设置 DB_URL、DB_USERNAME、DB_PASSWORD 环境变量
# 2. 将 src/main/resources/application-local.yml.example 复制为 application-local.yml 并填写（该文件不会提交）

# 运行测试
mvn test

# 启动后端服务
mvn spring-boot:run
```

后端服务启动后访问：http://localhost:8080/api

### 4. 前端启动

```bash
# 进入前端目录
cd e:\JAVA\schoolsystem\frontend

# 安装依赖
npm install

# 启动前端开发服务器
npm run dev
```

前端服务启动后访问：http://localhost:3000

## 🔐 默认账号

| 角色 | 用户名 | 密码 | 权限说明 |
|------|--------|------|----------|
| 管理员 | admin | 123456 | 所有权限，包括课程审核 |
| 教师 | teacher | 123456 | 课程管理、成绩管理 |
| 学生 | student | 123456 | 选课、查看成绩 |

## 📊 核心业务流程

### 课程发布与审核流程
1. **教师发布课程**：教师登录系统，添加新课程
2. **自动进入审核**：教师提交的课程状态为"待审核"
3. **管理员审核**：管理员在课程审核页面查看待审核课程
4. **审核结果**：通过（学生可看到）、拒绝（需修改后重新提交）
5. **学生选课**：仅可看到已审核通过的课程

### 成绩管理流程
1. **教师录入成绩**：录入平时成绩和期末成绩
2. **自动计算总评**：系统按权重自动计算总评成绩
3. **成绩修改**：教师可修改已录入的成绩
4. **成绩导出**：支持Excel格式导出

## 🔧 关键配置

### 数据库配置
```yaml
spring:
  datasource:
    url: ${DB_URL:...}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
```

`application.yml` 不含数据库密码。Druid 监控页默认关闭；若在受控内网启用，必须同时设置强凭据和网络访问边界。

### Shiro权限配置
- **匿名访问**：登录、注册页面
- **学生权限**：选课、查看成绩
- **教师权限**：课程管理、成绩管理
- **管理员权限**：所有权限，包括课程审核

## 🎨 界面特色

### 响应式设计
- 支持PC端和移动端访问
- Element Plus组件库，界面美观统一

### 用户体验优化
- 侧边栏二级菜单深色背景，文字清晰可读
- 操作反馈及时，错误提示明确
- 数据加载状态提示

## 📈 性能特性

### 后端可靠性
- **原子容量控制**：`selected_count < capacity` 是数据库更新条件，避免并发超卖
- **数据约束**：课程容量、选课人数、成绩范围和排课时间均有数据库约束
- **稳定分页**：课程和选课查询使用 `create_time DESC, id DESC` 的确定排序
- **链路定位**：每个响应返回 `X-Trace-Id`，日志也携带该值

### 前端性能
- **构建优化**：Vite快速构建
- **代码分割**：按需加载组件
- **资源压缩**：生产环境自动压缩

## 🐛 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查MySQL服务是否启动
   - 确认数据库配置正确

2. **权限问题**
   - 确认用户角色配置
   - 检查Shiro过滤器配置

3. **前端编译错误**
   - 清除node_modules重新安装依赖
   - 检查Node.js版本兼容性

### 日志查看
- 后端日志：控制台输出或logs目录
- 前端日志：浏览器开发者工具

## 📞 技术支持

如有问题请联系系统管理员或查看项目文档。

---

**系统版本**：v1.1.0  
**最后更新**：2026-08-02  
**开发团队**：高校课程管理系统开发组
