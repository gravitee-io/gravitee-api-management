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
package io.gravitee.apim.infra.zee.adapter;

import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.domain_service.RagSection;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic, data-driven {@link RagContextStrategy} implementation.
 *
 * <p>
 * Instead of writing a bespoke adapter class per resource type, callers
 * declare a list of {@link RagSection} descriptors. This adapter iterates
 * them, calls each fetcher, formats the results into markdown sections,
 * and handles errors with per-section graceful degradation.
 *
 * <p>
 * Adding a new resource type's RAG context is now ~5 lines of configuration
 * in {@link io.gravitee.apim.infra.zee.ZeeConfiguration} instead of a
 * full class + test.
 *
 * @author Derek Burger
 */
public class DeclarativeRagAdapter implements RagContextStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DeclarativeRagAdapter.class);

    private final ZeeResourceType resourceType;
    private final List<RagSection<?>> sections;

    public DeclarativeRagAdapter(ZeeResourceType resourceType, List<RagSection<?>> sections) {
        this.resourceType = resourceType;
        this.sections = List.copyOf(sections);
    }

    @Override
    public ZeeResourceType resourceType() {
        return resourceType;
    }

    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        var sb = new StringBuilder();

        for (var section : sections) {
            appendSection(sb, section, contextData);
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> void appendSection(StringBuilder sb, RagSection<T> section, Map<String, Object> contextData) {
        try {
            var items = section.fetcher().fetch(contextData);
            if (items == null || items.isEmpty()) {
                return;
            }

            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("### ").append(section.title()).append("\n");
            items
                .stream()
                .limit(section.maxItems())
                .forEach(item -> {
                    var line = section.formatter().apply(item);
                    sb.append("- ").append(line).append("\n");
                });
        } catch (Exception e) {
            LOG.warn("Failed to retrieve RAG section '{}' for resourceType={}: {}", section.title(), resourceType, e.getMessage());
        }
    }
}
