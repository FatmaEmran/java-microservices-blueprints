package com.fatmaemran.blueprints.catalog.api;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsOptimisticLockingFailuresToConflict() {
        ProblemDetail problem = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("stale product version")
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Concurrent modification");
        assertThat(problem.getDetail()).contains("modified by another request");
    }
}
