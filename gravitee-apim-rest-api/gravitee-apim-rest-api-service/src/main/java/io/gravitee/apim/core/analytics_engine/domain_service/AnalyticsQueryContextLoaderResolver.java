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
package io.gravitee.apim.core.analytics_engine.domain_service;

import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsScope;
import io.gravitee.apim.core.audit.model.AuditInfo;
import java.util.Map;

/**
 * A resolver is required because the analytics use cases are shared across the Management and Portal
 * surfaces and so cannot select their {@link AnalyticsQueryContextLoader} by injection type.
 */
public class AnalyticsQueryContextLoaderResolver {

    private final Map<AnalyticsScope, AnalyticsQueryContextLoader> loadersByScope;

    public AnalyticsQueryContextLoaderResolver(Map<AnalyticsScope, AnalyticsQueryContextLoader> loadersByScope) {
        this.loadersByScope = Map.copyOf(loadersByScope);
    }

    public AnalyticsQueryContext load(AuditInfo auditInfo, AnalyticsScope scope) {
        var loader = loadersByScope.get(scope);
        if (loader == null) {
            throw new IllegalStateException("No analytics query context loader available for scope " + scope);
        }
        return loader.load(auditInfo);
    }
}
