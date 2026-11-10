package com.school.system.service;

import com.school.system.common.BusinessException;
import com.school.system.entity.SysUser;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.shiro.SecurityUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Component
@EnableConfigurationProperties(StudentTrafficLimits.Settings.class)
public class StudentTrafficLimits {
    private final Budget catalog;
    private final Budget departments;
    private final Budget selectionRead;
    private final Budget selectionWrite;
    private final Set<Long> submitting = ConcurrentHashMap.newKeySet();
    private final MeterRegistry metrics;

    public StudentTrafficLimits(Settings settings, MeterRegistry metrics) {
        this.metrics = metrics;
        catalog = new Budget("catalog", settings.catalogConcurrency(), settings.catalogPerSecond());
        departments = new Budget("departments", settings.departmentsConcurrency(), settings.departmentsPerSecond());
        selectionRead = new Budget("selection_read", settings.selectionReadConcurrency(), settings.selectionReadPerSecond());
        selectionWrite = new Budget("selection_write", settings.selectionWriteConcurrency(), settings.selectionWritePerSecond());
    }

    public <T> T catalog(Supplier<T> request) {
        return catalog.run(request);
    }

    public <T> T departments(Supplier<T> request) {
        return departments.run(request);
    }

    public <T> T selectionRead(Supplier<T> request) {
        return selectionRead.run(request);
    }

    public void selectionWrite(Runnable request) {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (!(principal instanceof SysUser user)) {
            throw new BusinessException(401, "未登录");
        }
        Long userId = user.getUserId();
        if (!submitting.add(userId)) {
            throw reject(selectionWrite.name, 429, "正在处理您的选退课请求，请稍后重试");
        }
        try {
            selectionWrite.run(() -> {
                request.run();
                return null;
            });
        } finally {
            submitting.remove(userId);
        }
    }

    private BusinessException reject(String category, int status, String message) {
        metrics.counter("school.student.rejected", "category", category, "status", String.valueOf(status)).increment();
        return new BusinessException(status, message);
    }

    private class Budget {
        private final String name;
        private final Semaphore permits;
        private final int perSecond;
        private long windowStart = System.nanoTime();
        private int used;

        Budget(String name, int concurrency, int perSecond) {
            if (concurrency < 1 || perSecond < 0) {
                throw new IllegalArgumentException("无效的学生请求预算：" + name);
            }
            this.name = name;
            this.permits = new Semaphore(concurrency);
            this.perSecond = perSecond;
        }

        <T> T run(Supplier<T> request) {
            if (!allowRequest()) {
                throw reject(name, 429, "请求过于频繁，请稍后重试");
            }
            if (!permits.tryAcquire()) {
                throw reject(name, 503, "系统繁忙，请稍后重试");
            }
            try {
                SecurityUtils.getSubject().checkRole("STUDENT");
                return request.get();
            } finally {
                permits.release();
            }
        }

        private synchronized boolean allowRequest() {
            if (perSecond == 0) {
                return true;
            }
            long now = System.nanoTime();
            if (now - windowStart >= 1_000_000_000L) {
                windowStart = now;
                used = 0;
            }
            if (used >= perSecond) {
                return false;
            }
            used++;
            return true;
        }
    }

    @ConstructorBinding
    @ConfigurationProperties("app.limits")
    public record Settings(int catalogConcurrency, int catalogPerSecond, int departmentsConcurrency,
                           int departmentsPerSecond, int selectionReadConcurrency, int selectionReadPerSecond,
                           int selectionWriteConcurrency, int selectionWritePerSecond) {
    }
}
