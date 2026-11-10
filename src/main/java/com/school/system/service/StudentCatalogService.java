package com.school.system.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.school.system.common.BusinessException;
import com.school.system.common.PageResult;
import com.school.system.dto.DepartmentOption;
import com.school.system.dto.SelectableCourse;
import com.school.system.mapper.CourseMapper;
import com.school.system.mapper.DepartmentMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
public class StudentCatalogService {
    private static final TypeReference<PageResult<SelectableCourse>> PAGE_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<DepartmentOption>> DEPARTMENTS_TYPE = new TypeReference<>() {};

    private final CourseMapper courses;
    private final DepartmentMapper departments;
    private final StudentCatalogCache cache;
    private final MeterRegistry metrics;
    private final TransactionTemplate pageRead;

    public StudentCatalogService(CourseMapper courses, DepartmentMapper departments, StudentCatalogCache cache,
            MeterRegistry metrics, PlatformTransactionManager transactions) {
        this.courses = courses;
        this.departments = departments;
        this.cache = cache;
        this.metrics = metrics;
        pageRead = new TransactionTemplate(transactions);
        pageRead.setReadOnly(true);
        pageRead.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        pageRead.setTimeout(3);
    }

    public PageResult<SelectableCourse> selectableCourses(int current, int size, String courseName, Long deptId,
            BigDecimal minCredits, BigDecimal maxCredits) {
        if (current < 1 || size < 1 || size > 100) {
            throw BusinessException.badRequest("分页参数不合法：current >= 1，size 在 1 到 100 之间");
        }
        String name = courseName == null ? "" : courseName.strip();
        if (name.length() > 100 || (deptId != null && deptId < 1)
                || (minCredits != null && minCredits.signum() < 0)
                || (maxCredits != null && maxCredits.signum() < 0)
                || (minCredits != null && maxCredits != null && minCredits.compareTo(maxCredits) > 0)) {
            throw BusinessException.badRequest("课程筛选参数不合法");
        }
        String likeName = name.isEmpty() ? null : name.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        long offset = (current - 1L) * size;
        Supplier<PageResult<SelectableCourse>> load = () -> readDatabase("catalog", () -> pageRead.execute(status -> {
            long total = courses.countSelectable(likeName, deptId, minCredits, maxCredits);
            List<SelectableCourse> records = offset < total
                    ? courses.selectSelectable(offset, size, likeName, deptId, minCredits, maxCredits) : List.of();
            return PageResult.of(total, records, (long) current, (long) size);
        }));
        if (current == 1 && size == 10 && likeName == null && deptId == null && minCredits == null && maxCredits == null) {
            return cache.get(StudentCatalogCache.HOME_PAGE, PAGE_TYPE, 60, 15, load);
        }
        return load.get();
    }

    public List<DepartmentOption> departmentOptions() {
        return cache.get(StudentCatalogCache.DEPARTMENTS, DEPARTMENTS_TYPE, 1800, 60,
                () -> readDatabase("departments", departments::selectOptions));
    }

    private <T> T readDatabase(String category, Supplier<T> read) {
        try {
            return metrics.timer("school.catalog.database", "category", category).record(read);
        } catch (TransientDataAccessException | CannotGetJdbcConnectionException
                 | CannotCreateTransactionException | TransactionTimedOutException e) {
            log.warn("学生目录回源暂时不可用，类别 {}", category, e);
            throw new BusinessException(503, "课程目录暂时不可用，请稍后重试");
        }
    }
}
