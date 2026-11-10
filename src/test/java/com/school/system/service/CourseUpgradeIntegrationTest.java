package com.school.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.school.system.common.BusinessException;
import com.school.system.dto.CourseDTO;
import com.school.system.dto.CourseSelectionDTO;
import com.school.system.dto.SelectableCourse;
import com.school.system.entity.SysUser;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "UPGRADE_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=${UPGRADE_TEST_DB_URL}",
        "spring.datasource.username=${UPGRADE_TEST_DB_USERNAME:root}",
        "spring.datasource.password=${UPGRADE_TEST_DB_PASSWORD:}",
        "spring.redis.port=${UPGRADE_TEST_REDIS_PORT:6379}",
        "app.cache.enabled=${UPGRADE_TEST_CACHE:false}",
        "app.cache.selectable-home-enabled=true",
        "app.cache.environment=upgrade-test"
})
class CourseUpgradeIntegrationTest {
    private static final String HOME_KEY = "school:upgrade-test:v1:" + StudentCatalogCache.HOME_PAGE;
    private static final String SEMESTER = "春季学期";
    private static final String YEAR = "2025-2026";

    @Autowired private JdbcTemplate db;
    @Autowired private CourseSelectionService selections;
    @Autowired private CourseService courses;
    @Autowired private StudentCatalogService catalog;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ObjectMapper json;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private TestRestTemplate http;
    @LocalServerPort private int port;

    private final String prefix = "it" + UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void cleanup() {
        db.update("DELETE FROM course WHERE course_code LIKE ?", prefix + "%");
        db.update("DELETE FROM sys_user WHERE username LIKE ?", prefix + "%");
    }

    @Test
    void oneThousandStudentsCanFillOnlyFiftySeats() throws Exception {
        long course = createCourse("many", 50);
        List<Long> users = createStudents(1000);
        List<Integer> results = concurrently(users.stream().<Callable<Integer>>map(user ->
                () -> select(user, course, YEAR)).toList());
        assertEquals(50, results.stream().filter(status -> status == 200).count());
        assertEquals(950, results.stream().filter(status -> status == 409).count());
        assertSeatCount(course, 50);
    }

    @Test
    void duplicateSelectionAndDropChangeSeatCountOnceAndFailedInsertRollsBack() throws Exception {
        long course = createCourse("duplicates", 10);
        long user = createStudents(1).get(0);
        assertThrows(RuntimeException.class, () -> select(user, course, "value-too-long-for-year"));
        assertSeatCount(course, 0);

        assertEquals(List.of(200, 409), concurrently(List.of(
                () -> select(user, course, YEAR), () -> select(user, course, YEAR))).stream().sorted().toList());
        assertSeatCount(course, 1);
        long selection = db.queryForObject("SELECT selection_id FROM course_selection WHERE course_id = ?", Long.class, course);
        Callable<Integer> drop = () -> asUser(user, false, () -> selections.dropCourse(selection));
        assertEquals(List.of(200, 409), concurrently(List.of(drop, drop)).stream().sorted().toList());
        assertSeatCount(course, 0);
        assertEquals(200, select(user, course, YEAR));
        assertSeatCount(course, 1);
    }

    @Test
    void overlappingCoursesForTheSameStudentAreSerialized() throws Exception {
        long first = createCourse("slot1", 10);
        long second = createCourse("slot2", 10);
        for (long course : new long[]{first, second}) {
            db.update("INSERT INTO course_schedule (course_id, day_of_week, start_section, end_section, start_week, end_week) "
                    + "VALUES (?, 1, 1, 2, 1, 16)", course);
        }
        long user = createStudents(1).get(0);
        List<Integer> results = concurrently(List.of(() -> select(user, first, YEAR), () -> select(user, second, YEAR)));
        assertEquals(List.of(200, 409), results.stream().sorted().toList());
        assertSeatCount(first, results.get(0) == 200 ? 1 : 0);
        assertSeatCount(second, results.get(1) == 200 ? 1 : 0);
    }

    @Test
    void concurrentCapacityReductionAndSelectionRespectSeatCount() throws Exception {
        long course = createCourse("capacity", 2);
        List<Long> users = createStudents(2);
        assertEquals(200, select(users.get(0), course, YEAR));
        CourseDTO edit = edit(course, "capacity", 1);
        List<Integer> results = concurrently(List.of(
                () -> select(users.get(1), course, YEAR),
                () -> asUser(0, true, () -> courses.updateCourse(edit))));
        assertEquals(List.of(200, 409), results.stream().sorted().toList());
        int count = results.get(0) == 200 ? 2 : 1;
        assertSeatCount(course, count);
        assertEquals(count, db.queryForObject("SELECT capacity FROM course WHERE course_id = ?", Integer.class, course));
        assertEquals(200, asUser(0, true, () -> courses.updateCourse(edit(course, "capacity", null))));
        assertEquals(count, db.queryForObject("SELECT capacity FROM course WHERE course_id = ?", Integer.class, course));
    }

    @Test
    void selectablePageFiltersAndCountsTheSameRows() {
        long first = createCourse("filter%1", 50);
        long second = createCourse("filter%2", 50);
        long hidden = createCourse("filter%3", 50);
        createCourse("filterX4", 50);
        long unapproved = createCourse("filter%5", 50);
        long highCredits = createCourse("filter%6", 50);
        long otherDepartment = createCourse("filter%7", 50);
        db.update("UPDATE course SET status = 0 WHERE course_id = ?", hidden);
        db.update("UPDATE course SET audit_status = 0 WHERE course_id = ?", unapproved);
        db.update("UPDATE course SET credits = 4 WHERE course_id = ?", highCredits);
        db.update("UPDATE course SET dept_id = (SELECT MAX(dept_id) FROM department) WHERE course_id = ?", otherDepartment);
        db.update("UPDATE course SET create_time = '2025-01-01 12:00:00' WHERE course_code LIKE ?", prefix + "%");
        long department = department();
        var page = catalog.selectableCourses(1, 1, prefix + "filter%", department, BigDecimal.ONE, BigDecimal.valueOf(3));
        assertEquals(2L, page.getTotal());
        assertEquals(List.of(second), page.getRecords().stream().map(SelectableCourse::getCourseId).toList());
        assertEquals(first, catalog.selectableCourses(2, 1, prefix + "filter%", department, BigDecimal.ONE, BigDecimal.valueOf(3))
                .getRecords().get(0).getCourseId());
        assertTrue(catalog.selectableCourses(Integer.MAX_VALUE, 100, prefix, null, null, null).getRecords().isEmpty());
    }

    @Test
    void studentHttpRoutesCheckRoleAndReturnPagedData() {
        long course = createCourse("http", 5);
        long student = createStudents(1).get(0);
        HttpEntity<Void> studentSession = loginAs(student, "STUDENT");
        HttpEntity<Void> adminSession = loginAs(createUser("admin"), "ADMIN");

        var page = http.exchange(api("/course/selectable?courseName=" + prefix), HttpMethod.GET, studentSession, JsonNode.class);
        assertEquals(200, page.getStatusCodeValue());
        assertEquals(1, page.getBody().at("/data/total").asInt());
        assertEquals(course, page.getBody().at("/data/records/0/courseId").asLong());
        assertEquals(400, http.exchange(api("/course/selectable?size=1000"), HttpMethod.GET, studentSession, JsonNode.class)
                .getStatusCodeValue());
        assertEquals(403, http.exchange(api("/course/selectable"), HttpMethod.GET, adminSession, JsonNode.class)
                .getStatusCodeValue());
        assertEquals(403, http.exchange(api("/department/list"), HttpMethod.GET, studentSession, JsonNode.class)
                .getStatusCodeValue());
        var options = http.exchange(api("/department/options"), HttpMethod.GET, studentSession, JsonNode.class);
        assertEquals(200, options.getStatusCodeValue());
        assertEquals(2, options.getBody().at("/data/0").size());

        var select = http.postForEntity(api("/selection/select"), new HttpEntity<>(
                Map.of("courseId", course, "semester", SEMESTER, "year", YEAR), studentSession.getHeaders()), JsonNode.class);
        assertEquals(200, select.getStatusCodeValue());
        assertSeatCount(course, 1);
        long studentId = db.queryForObject("SELECT student_id FROM student WHERE user_id = ?", Long.class, student);
        var selected = http.exchange(api("/selection/student/" + studentId), HttpMethod.GET, studentSession, JsonNode.class);
        assertEquals(200, selected.getStatusCodeValue());
        long selectionId = selected.getBody().at("/data/0/selectionId").asLong();
        assertTrue(selectionId > 0);
        assertEquals(200, http.exchange(api("/selection/drop/" + selectionId), HttpMethod.DELETE, studentSession, JsonNode.class)
                .getStatusCodeValue());
        assertEquals(409, http.exchange(api("/selection/drop/" + selectionId), HttpMethod.DELETE, studentSession, JsonNode.class)
                .getStatusCodeValue());
        assertSeatCount(course, 0);
    }

    @Test
    void teacherAndAdminManageCourseWithoutBypassingCatalogVisibility() {
        long teacher = createUser("teacher");
        db.update("INSERT INTO teacher (user_id, dept_id) VALUES (?, ?)", teacher, department());
        HttpEntity<Void> teacherSession = loginAs(teacher, "TEACHER");
        HttpEntity<Void> adminSession = loginAs(createUser("admin"), "ADMIN");
        CourseDTO draft = edit(0, "managed", 5);
        draft.setCourseId(null);
        postCourse("/course/add", draft, teacherSession);
        long course = db.queryForObject("SELECT course_id FROM course WHERE course_code = ?", Long.class, prefix + "managed");
        assertFalse(isSelectable());
        assertEquals(403, http.postForEntity(api("/course/approve/" + course), teacherSession, JsonNode.class).getStatusCodeValue());
        postCourse("/course/approve/" + course, null, adminSession);
        assertTrue(isSelectable());

        CourseDTO update = edit(course, "managed", 7);
        update.setStatus(0);
        postCourse("/course/update", update, teacherSession);
        assertEquals(7, db.queryForObject("SELECT capacity FROM course WHERE course_id = ?", Integer.class, course));
        assertTrue(isSelectable());

        postCourse("/course/status/" + course + "?status=0", null, adminSession);
        assertFalse(isSelectable());
        postCourse("/course/status/" + course + "?status=1", null, adminSession);
        assertTrue(isSelectable());
        postCourse("/course/reject/" + course, null, adminSession);
        assertFalse(isSelectable());

        var deleted = http.exchange(api("/course/delete/" + course), HttpMethod.DELETE, teacherSession, JsonNode.class);
        assertEquals(200, deleted.getStatusCodeValue());
        assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM course WHERE course_id = ?", Integer.class, course));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "UPGRADE_TEST_CACHE", matches = "true")
    void committedChangeInvalidatesHomeCacheButRollbackPreservesIt() {
        long course = createCourse("cache", 5);
        redis.delete(HOME_KEY);
        try {
            catalog.selectableCourses(1, 10, null, null, null, null);
            assertNotNull(redis.opsForValue().get(HOME_KEY));
            new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                courses.updateCourseStatus(course, 0);
                tx.setRollbackOnly();
            });
            assertTrue(Boolean.TRUE.equals(redis.hasKey(HOME_KEY)));
            courses.updateCourseStatus(course, 0);
            assertFalse(Boolean.TRUE.equals(redis.hasKey(HOME_KEY)));
            assertFalse(catalog.selectableCourses(1, 10, null, null, null, null).getRecords().stream()
                    .anyMatch(row -> row.getCourseId() == course));
        } finally {
            redis.delete(HOME_KEY);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "UPGRADE_TEST_CACHE", matches = "true")
    void formerLockOwnerCannotOverwriteOrUnlockTheReplacement() throws Exception {
        String key = "school:" + prefix + ":v1:" + StudentCatalogCache.DEPARTMENTS;
        String lock = key + ":lock";
        var first = new StudentCatalogCache(redis, json, new SimpleMeterRegistry(), true, true, prefix);
        TypeReference<String> type = new TypeReference<>() {};
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> previous = pool.submit(() -> first.get(StudentCatalogCache.DEPARTMENTS, type, 60, 0, () -> {
                loaded.countDown();
                await(finish);
                return "old";
            }));
            await(loaded);
            redis.opsForValue().set(lock, "new-owner", Duration.ofSeconds(8));
            redis.opsForValue().set(key, "\"new\"", Duration.ofSeconds(60));
            finish.countDown();
            assertEquals("old", previous.get(10, TimeUnit.SECONDS));
            assertEquals("\"new\"", redis.opsForValue().get(key));
            assertEquals("new-owner", redis.opsForValue().get(lock));
        } finally {
            finish.countDown();
            pool.shutdownNow();
            redis.delete(List.of(key, lock));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "UPGRADE_TEST_CACHE", matches = "true")
    void twoInstancesShareOneRefillAndWaitingHasABudget() throws Exception {
        String key = "school:" + prefix + ":v1:" + StudentCatalogCache.DEPARTMENTS;
        var first = new StudentCatalogCache(redis, json, new SimpleMeterRegistry(), true, true, prefix);
        var second = new StudentCatalogCache(redis, json, new SimpleMeterRegistry(), true, true, prefix);
        TypeReference<String> type = new TypeReference<>() {};
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<String> refill = pool.submit(() -> first.get(StudentCatalogCache.DEPARTMENTS, type, 60, 0, () -> {
                loaded.countDown();
                await(finish);
                return "loaded";
            }));
            await(loaded);
            assertEquals(503, assertThrows(BusinessException.class, () -> second.get(
                    StudentCatalogCache.DEPARTMENTS, type, 60, 0, () -> fail("The second instance must wait"))).getStatus());
            finish.countDown();
            assertEquals("loaded", refill.get(10, TimeUnit.SECONDS));
            assertEquals("loaded", second.get(StudentCatalogCache.DEPARTMENTS, type, 60, 0,
                    () -> fail("The second instance should read the cached value")));
        } finally {
            finish.countDown();
            pool.shutdownNow();
            redis.delete(List.of(key, key + ":lock"));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "UPGRADE_TEST_CACHE", matches = "true")
    void redisConnectionFailureFallsBackAndOpensCircuit() {
        var client = LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(150))
                .shutdownTimeout(Duration.ZERO).build();
        var factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", 1), client);
        factory.afterPropertiesSet();
        try {
            var meters = new SimpleMeterRegistry();
            var cache = new StudentCatalogCache(new StringRedisTemplate(factory), json, meters, true, true, prefix);
            var calls = new AtomicInteger();
            TypeReference<Integer> type = new TypeReference<>() {};
            for (int i = 1; i <= 4; i++) {
                assertEquals(i, cache.get(StudentCatalogCache.DEPARTMENTS, type, 60, 0, calls::incrementAndGet));
            }
            assertEquals(1.0, meters.get("school.catalog.cache").tag("outcome", "circuit_open").counter().count());
        } finally {
            factory.destroy();
        }
    }

    private long department() {
        return db.queryForObject("SELECT MIN(dept_id) FROM department", Long.class);
    }

    private String api(String path) {
        return "http://127.0.0.1:" + port + "/api" + path;
    }

    private long createUser(String suffix) {
        db.update("INSERT INTO sys_user (username, password, real_name) VALUES (?, 'unused', 'Integration test')", prefix + suffix);
        return db.queryForObject("SELECT user_id FROM sys_user WHERE username = ?", Long.class, prefix + suffix);
    }

    private void postCourse(String path, CourseDTO course, HttpEntity<Void> session) {
        var response = http.postForEntity(api(path), new HttpEntity<>(course, session.getHeaders()), JsonNode.class);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(200, response.getBody().path("code").asInt());
    }

    private boolean isSelectable() {
        return catalog.selectableCourses(1, 10, prefix + "managed", null, null, null).getTotal() == 1;
    }

    private HttpEntity<Void> loginAs(long user, String role) {
        String password = "integration-test";
        db.update("UPDATE sys_user SET password = SHA2(?, 256) WHERE user_id = ?", password, user);
        db.update("INSERT INTO sys_user_role (user_id, role_id) SELECT ?, role_id FROM sys_role WHERE role_code = ?", user, role);
        String username = db.queryForObject("SELECT username FROM sys_user WHERE user_id = ?", String.class, user);
        var login = http.postForEntity(api("/auth/login"), Map.of("username", username, "password", password), JsonNode.class);
        assertEquals(200, login.getBody().path("code").asInt());
        String cookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(cookie);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, cookie.split(";", 2)[0]);
        return new HttpEntity<>(headers);
    }

    private long createCourse(String name, int capacity) {
        db.update("INSERT INTO course (course_name, course_code, credits, hours, dept_id, audit_status, capacity) "
                + "VALUES (?, ?, 2, 16, ?, 1, ?)", prefix + name, prefix + name, department(), capacity);
        return db.queryForObject("SELECT course_id FROM course WHERE course_code = ?", Long.class, prefix + name);
    }

    private CourseDTO edit(long course, String name, Integer capacity) {
        CourseDTO dto = new CourseDTO();
        dto.setCourseId(course);
        dto.setCourseName(prefix + name);
        dto.setCourseCode(prefix + name);
        dto.setHours(16);
        dto.setCredits(BigDecimal.valueOf(2));
        dto.setDeptId(department());
        dto.setCapacity(capacity);
        return dto;
    }

    private List<Long> createStudents(int count) {
        List<Object[]> users = IntStream.range(0, count).mapToObj(i -> new Object[]{prefix + i}).toList();
        db.batchUpdate("INSERT INTO sys_user (username, password, real_name) VALUES (?, 'unused', 'Integration test')", users);
        long dept = department();
        db.update("INSERT INTO student (user_id, dept_id, student_no) "
                + "SELECT user_id, ?, username FROM sys_user WHERE username LIKE ?", dept, prefix + "%");
        return db.queryForList("SELECT user_id FROM sys_user WHERE username LIKE ?", Long.class, prefix + "%");
    }

    private int select(long user, long course, String year) {
        CourseSelectionDTO request = new CourseSelectionDTO();
        request.setCourseId(course);
        request.setSemester(SEMESTER);
        request.setYear(year);
        return asUser(user, false, () -> selections.selectCourse(request));
    }

    private int asUser(long user, boolean admin, Runnable action) {
        SysUser principal = new SysUser();
        principal.setUserId(user);
        Subject subject = mock(Subject.class);
        when(subject.getPrincipal()).thenReturn(principal);
        when(subject.hasRole("ADMIN")).thenReturn(admin);
        ThreadContext.bind(subject);
        try {
            action.run();
            return 200;
        } catch (BusinessException e) {
            return e.getStatus();
        } finally {
            ThreadContext.unbindSubject();
        }
    }

    private void assertSeatCount(long course, int expected) {
        assertEquals(expected, db.queryForObject("SELECT selected_count FROM course WHERE course_id = ?", Integer.class, course));
        assertEquals(expected, db.queryForObject(
                "SELECT COUNT(*) FROM course_selection WHERE course_id = ? AND status = 1", Integer.class, course));
    }

    private static List<Integer> concurrently(List<Callable<Integer>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> call : calls) {
                futures.add(pool.submit(() -> {
                    await(start);
                    return call.call();
                }));
            }
            start.countDown();
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
