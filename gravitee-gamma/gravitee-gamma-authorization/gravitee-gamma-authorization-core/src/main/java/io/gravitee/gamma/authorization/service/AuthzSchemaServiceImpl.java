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
package io.gravitee.gamma.authorization.service;

import io.gravitee.gamma.authorization.api.AuthzEntityRepository;
import io.gravitee.gamma.authorization.api.AuthzPolicyRepository;
import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AuthzSchemaServiceImpl implements AuthzSchemaAdminApi {

    private static final String EMPTY_SCHEMA = "// No entities or policies defined yet.\n";

    private final AuthzEntityRepository entityRepository;
    private final AuthzPolicyRepository policyRepository;
    private final ConcurrentMap<String, String> cacheByEnv = new ConcurrentHashMap<>();

    public AuthzSchemaServiceImpl(AuthzEntityRepository entityRepository, AuthzPolicyRepository policyRepository) {
        this.entityRepository = Objects.requireNonNull(entityRepository, "entityRepository must not be null");
        this.policyRepository = Objects.requireNonNull(policyRepository, "policyRepository must not be null");
    }

    @Override
    public String currentGaplSchema(String environmentId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        return cacheByEnv.computeIfAbsent(environmentId, this::build);
    }

    @Override
    public void invalidate(String environmentId) {
        if (environmentId != null) {
            cacheByEnv.remove(environmentId);
        }
    }

    public void invalidateAll() {
        cacheByEnv.clear();
    }

    private String build(String environmentId) {
        List<AuthzEntity> entities = entityRepository.findAll(environmentId);
        List<AuthzPolicy> policies = policyRepository.findAll(environmentId);

        Map<String, List<AuthzEntity>> bucketsByType = new LinkedHashMap<>();
        for (AuthzEntity e : entities) {
            bucketsByType.computeIfAbsent(typeNameFor(e), k -> new java.util.ArrayList<>()).add(e);
        }
        for (AuthzPolicy p : policies) {
            String typeName = typeNameFor(p);
            if (typeName != null) {
                bucketsByType.computeIfAbsent(typeName, k -> new java.util.ArrayList<>());
            }
        }

        if (bucketsByType.isEmpty()) {
            return EMPTY_SCHEMA;
        }

        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, List<AuthzEntity>> bucket : bucketsByType.entrySet()) {
            if (!first) out.append('\n');
            first = false;
            out.append("entity ").append(bucket.getKey());
            renderAttributes(bucket.getValue(), out);
            out.append(";\n");
        }
        return out.toString();
    }

    private static String typeNameFor(AuthzEntity e) {
        String entityId = e.entityId();
        int firstDot = entityId.indexOf('.');
        if (firstDot > 0) {
            return capitalise(entityId.substring(0, firstDot));
        }
        return capitalise(e.kind().name());
    }

    private static String typeNameFor(AuthzPolicy p) {
        String entityId = p.entityId();
        if (entityId == null) {
            return null;
        }
        int firstDot = entityId.indexOf('.');
        if (firstDot > 0) {
            return capitalise(entityId.substring(0, firstDot));
        }
        return null;
    }

    private static String capitalise(String s) {
        if (s.isEmpty()) {
            return s;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void renderAttributes(List<AuthzEntity> bucket, StringBuilder out) {
        TreeMap<String, String> typeByAttribute = new TreeMap<>();
        for (AuthzEntity e : bucket) {
            for (Map.Entry<String, Object> attr : e.attributes().entrySet()) {
                typeByAttribute.putIfAbsent(attr.getKey(), gaplTypeOf(attr.getValue()));
            }
        }
        if (typeByAttribute.isEmpty()) {
            out.append(" {}");
            return;
        }
        out.append(" {\n");
        int i = 0;
        for (Map.Entry<String, String> attr : typeByAttribute.entrySet()) {
            out.append("    ").append(attr.getKey()).append(": ").append(attr.getValue());
            if (++i < typeByAttribute.size()) out.append(',');
            out.append('\n');
        }
        out.append('}');
    }

    private static String gaplTypeOf(Object value) {
        if (value instanceof Boolean) return "Bool";
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) return "Long";
        if (value instanceof Float || value instanceof Double) return "decimal";
        return "String";
    }

    Set<String> cachedEnvironments() {
        return new LinkedHashSet<>(cacheByEnv.keySet());
    }
}
