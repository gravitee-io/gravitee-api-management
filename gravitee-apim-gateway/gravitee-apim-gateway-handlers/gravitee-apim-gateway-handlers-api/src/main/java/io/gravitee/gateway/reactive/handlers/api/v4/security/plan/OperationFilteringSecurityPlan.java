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

import io.gravitee.gateway.handlers.api.ReactableApiProduct.ApiProductOperation;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpSecurityPolicy;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.security.plan.SecurityPlanContext;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.regex.Pattern;
import lombok.CustomLog;

/**
 * Wraps an {@link HttpSecurityPlan} for an API Product plan and rejects the request immediately
 * (returns {@code canExecute = false}) if the incoming path+method is not listed in
 * {@code allowedOperations}. When {@code allowedOperations} is null this class is not used
 * (full access — the bare plan is used instead).
 *
 * <p>Path matching supports RFC-6570-style {@code {param}} segments (each segment is treated as
 * a single path-segment wildcard).  Method matching is case-insensitive; {@code "*"} matches all.
 */
@CustomLog
public class OperationFilteringSecurityPlan extends HttpSecurityPlan {

    private final List<ApiProductOperation> allowedOperations;

    public OperationFilteringSecurityPlan(
        SecurityPlanContext planContext,
        HttpSecurityPolicy policy,
        List<ApiProductOperation> allowedOperations
    ) {
        super(planContext, policy);
        this.allowedOperations = allowedOperations;
    }

    @Override
    public Single<Boolean> canExecute(HttpPlainExecutionContext ctx) {
        // pathInfo() is path relative to context path (e.g. /posts, not /my-api/posts).
        // When the request hits the context root exactly, pathInfo() returns "" — normalize to "/".
        String rawPathInfo = ctx.request().pathInfo();
        String requestPath = (rawPathInfo == null || rawPathInfo.isEmpty()) ? "/" : rawPathInfo;
        String requestMethod = ctx.request().method().name();
        if (!isOperationAllowed(requestPath, requestMethod)) {
            ctx
                .withLogger(log)
                .debug(
                    "Operation [{} {}] is not allowed by API Product operation filter — skipping plan [{}]",
                    requestMethod,
                    requestPath,
                    id()
                );
            return Single.just(false);
        }
        return super.canExecute(ctx);
    }

    private boolean isOperationAllowed(String path, String method) {
        for (ApiProductOperation op : allowedOperations) {
            if (methodMatches(op.getMethod(), method) && pathMatches(op.getPath(), path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean methodMatches(String allowed, String actual) {
        return "*".equals(allowed) || allowed.equalsIgnoreCase(actual);
    }

    private static boolean pathMatches(String template, String actual) {
        if (template.equals(actual)) {
            return true;
        }
        // Split template at {param} groups, quote literal parts, join with [^/]+ wildcards
        String[] parts = template.split("\\{[^}]+\\}", -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append("[^/]+");
            }
            regex.append(Pattern.quote(parts[i]));
        }
        return Pattern.matches(regex.toString(), actual);
    }
}
