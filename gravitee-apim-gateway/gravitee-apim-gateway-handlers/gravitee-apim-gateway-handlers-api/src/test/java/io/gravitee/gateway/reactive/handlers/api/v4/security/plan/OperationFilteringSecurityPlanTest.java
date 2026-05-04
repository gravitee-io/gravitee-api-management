/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.handlers.api.v4.security.plan;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_DIAGNOSTIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.handlers.api.ReactableApiProduct.ApiProductOperation;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.gravitee.gateway.reactive.handlers.api.security.SecurityChainDiagnostic;
import io.gravitee.gateway.reactive.handlers.api.security.plan.SecurityPlanContext;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.helpers.NOPLogger;

/**
 * Tests for {@link OperationFilteringSecurityPlan}.
 *
 * <p>All paths use pathInfo() format (relative to context path, e.g. "/posts" not "/my-api/posts").
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OperationFilteringSecurityPlanTest {

    @Mock
    private SecurityPlanContext planContext;

    @Mock
    private HttpSecurityPolicy policy;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private HttpPlainRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(planContext.planId()).thenReturn("plan-id");
        lenient().when(planContext.planName()).thenReturn("Test Plan");
        // withLogger(log) is used for debug logging — return a no-op logger so calls don't NPE
        lenient().when(ctx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);
        SecurityChainDiagnostic diagnostic = new SecurityChainDiagnostic(false);
        lenient().when(ctx.getInternalAttribute(ATTR_INTERNAL_SECURITY_DIAGNOSTIC)).thenReturn(diagnostic);
    }

    private OperationFilteringSecurityPlan plan(List<ApiProductOperation> ops) {
        return new OperationFilteringSecurityPlan(planContext, policy, ops);
    }

    @Nested
    class CanExecute {

        @Test
        void should_return_false_when_method_does_not_match() {
            when(request.pathInfo()).thenReturn("/posts");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.GET);

            var result = plan(List.of(op("/posts", "POST"))).canExecute(ctx).blockingGet();

            assertThat(result).isFalse();
            verify(policy, never()).extractSecurityToken(any());
        }

        @Test
        void should_return_false_when_path_does_not_match() {
            when(request.pathInfo()).thenReturn("/comments");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.POST);

            var result = plan(List.of(op("/posts", "POST"))).canExecute(ctx).blockingGet();

            assertThat(result).isFalse();
            verify(policy, never()).extractSecurityToken(any());
        }

        @Test
        void should_delegate_to_super_when_operation_is_allowed() {
            when(request.pathInfo()).thenReturn("/posts");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.POST);
            // Super.canExecute calls extractSecurityToken — return empty to simulate "no token"
            when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.empty());

            var result = plan(List.of(op("/posts", "POST"))).canExecute(ctx).blockingGet();

            assertThat(result).isFalse(); // no token → false from super
            verify(policy).extractSecurityToken(ctx);
        }

        @Test
        void should_match_wildcard_method() {
            when(request.pathInfo()).thenReturn("/posts");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.DELETE);
            when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.empty());

            plan(List.of(op("/posts", "*"))).canExecute(ctx).blockingGet();

            // Operation IS allowed (wildcard), so super.canExecute runs (returns false here due to no token)
            verify(policy).extractSecurityToken(ctx);
        }

        @Test
        void should_match_path_with_param_placeholder() {
            when(request.pathInfo()).thenReturn("/posts/42");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.GET);
            when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.empty());

            plan(List.of(op("/posts/{id}", "GET"))).canExecute(ctx).blockingGet();

            verify(policy).extractSecurityToken(ctx);
        }

        @Test
        void should_return_false_when_multiple_ops_none_match() {
            when(request.pathInfo()).thenReturn("/posts");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.DELETE);

            var result = plan(List.of(op("/posts", "GET"), op("/posts", "POST"))).canExecute(ctx).blockingGet();

            assertThat(result).isFalse();
            verify(policy, never()).extractSecurityToken(any());
        }

        @Test
        void should_allow_when_one_of_multiple_ops_matches() {
            when(request.pathInfo()).thenReturn("/posts");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.POST);
            when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.empty());

            plan(List.of(op("/posts", "GET"), op("/posts", "POST"))).canExecute(ctx).blockingGet();

            verify(policy).extractSecurityToken(ctx);
        }

        @Test
        void should_match_root_path_when_pathInfo_is_empty() {
            // AbstractRequest.pathInfo() returns "" when request hits the context root exactly
            when(request.pathInfo()).thenReturn("");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.GET);
            when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.empty());

            plan(List.of(op("/", "GET"))).canExecute(ctx).blockingGet();

            verify(policy).extractSecurityToken(ctx);
        }

        @Test
        void should_not_include_context_path_in_match() {
            // pathInfo() strips context path — /posts should NOT match against full path /my-api/posts
            when(request.pathInfo()).thenReturn("/posts");
            when(request.method()).thenReturn(io.gravitee.common.http.HttpMethod.GET);

            var result = plan(List.of(op("/my-api/posts", "GET"))).canExecute(ctx).blockingGet();

            assertThat(result).isFalse();
            verify(policy, never()).extractSecurityToken(any());
        }
    }

    private static ApiProductOperation op(String path, String method) {
        return new ApiProductOperation(path, method);
    }
}
