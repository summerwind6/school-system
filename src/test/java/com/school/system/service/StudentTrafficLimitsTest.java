package com.school.system.service;

import com.school.system.common.BusinessException;
import com.school.system.entity.SysUser;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentTrafficLimitsTest {
    @Test
    void busyCatalogCannotUseSelectionBudget() throws Exception {
        var limits = limits(0);
        var pool = Executors.newSingleThreadExecutor();
        var started = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        try {
            var first = pool.submit(() -> asStudent(() -> limits.catalog(() -> {
                started.countDown();
                try {
                    assertTrue(finish.await(5, TimeUnit.SECONDS));
                    return 1;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            })));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            asStudent(() -> {
                assertEquals(503, assertThrows(BusinessException.class,
                        () -> limits.catalog(() -> fail("Busy requests must not reach the database"))).getStatus());
                limits.selectionWrite(() -> {});
                return null;
            });
            finish.countDown();
            assertEquals(1, first.get(5, TimeUnit.SECONDS));
            assertEquals(2, asStudent(() -> limits.catalog(() -> 2)));
        } finally {
            finish.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void repeatedSubmissionForAnAccountIsRejectedAndPermitIsReleased() {
        var limits = limits(0);
        asStudent(() -> {
            limits.selectionWrite(() -> assertEquals(429, assertThrows(BusinessException.class,
                    () -> limits.selectionWrite(() -> fail("Concurrent requests for this student must be rejected"))).getStatus()));
            limits.selectionWrite(() -> {});
            return null;
        });
    }

    private StudentTrafficLimits limits(int rate) {
        return new StudentTrafficLimits(new StudentTrafficLimits.Settings(1, rate, 1, rate, 1, rate, 1, rate),
                new SimpleMeterRegistry());
    }

    private <T> T asStudent(Supplier<T> action) {
        Subject subject = mock(Subject.class);
        SysUser user = new SysUser();
        user.setUserId(10L);
        when(subject.getPrincipal()).thenReturn(user);
        ThreadContext.bind(subject);
        try {
            return action.get();
        } finally {
            ThreadContext.unbindSubject();
        }
    }
}
