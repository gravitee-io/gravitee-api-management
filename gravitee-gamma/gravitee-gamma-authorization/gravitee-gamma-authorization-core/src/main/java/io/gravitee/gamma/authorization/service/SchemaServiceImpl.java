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

import io.gravitee.gamma.authorization.api.SchemaAdminApi;
import io.gravitee.gamma.repository.authorization.api.AuthorizationEntityRepository;
import io.gravitee.gamma.repository.authorization.api.AuthorizationPolicyRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
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

public class SchemaServiceImpl implements SchemaAdminApi {

    private static final String EMPTY_SCHEMA = "// No entities or policies defined yet.\n";

    private final AuthorizationEntityRepository entityRepository;
    private final AuthorizationPolicyRepository policyRepository;
    private final ConcurrentMap<String, String> cacheByEnv = new ConcurrentHashMap<>();

    public SchemaServiceImpl(AuthorizationEntityRepository entityRepository, AuthorizationPolicyRepository policyRepository) {
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
        List<AuthorizationEntity> entities = entityRepository.findAllByEnvironmentId(environmentId);
        List<AuthorizationPolicy> policies = policyRepository.findAllByEnvironmentId(environmentId);

        Map<String, List<AuthorizationEntity>> bucketsByType = new LinkedHashMap<>();
        for (AuthorizationEntity e : entities) {
            bucketsByType.computeIfAbsent(typeNameFor(e), k -> new java.util.ArrayList<>()).add(e);
        }
        for (AuthorizationPolicy p : policies) {
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
        for (Map.Entry<String, List<AuthorizationEntity>> bucket : bucketsByType.entrySet()) {
            if (!first) out.append('\n');
            first = false;
            out.append("entity ").append(bucket.getKey());
            renderAttributes(bucket.getValue(), out);
            out.append(";\n");
        }
        return out.toString();
    }

    private static String typeNameFor(AuthorizationEntity e) {
        String entityId = e.entityId();
        int firstDot = entityId.indexOf('.');
        if (firstDot > 0) {
            return capitalise(entityId.substring(0, firstDot));
        }
        return capitalise(e.kind().name());
    }

    private static String typeNameFor(AuthorizationPolicy p) {
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

    /**
     * Normalise a raw uid-prefix or kind enum name to PascalCase using a fixed
     * (locale-independent) lowercase fold so {@code typeNameFor(AuthorizationEntity)} and
     * {@code typeNameFor(AuthorizationPolicy)} bucket consistently regardless of input casing.
     */
    private static String capitalise(String s) {
        if (s.isEmpty()) {
            return s;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void renderAttributes(List<AuthorizationEntity> bucket, StringBuilder out) {
        TreeMap<String, String> typeByAttribute = new TreeMap<>();
        for (AuthorizationEntity e : bucket) {
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

    /**
     * Maps a Java attribute value to a primitive type literal accepted by the
     * GAPL grammar (AuthzSchemaLang.g4 typeRef): {@code String}, {@code Long},
     * {@code Bool}, {@code decimal}. Anything else (null, list, map, custom)
     * falls back to {@code String} — emitting an unrecognised identifier would
     * be silently treated as a reference to a non-existent entity type and
     * break {@code PolicyValidator}.
     */
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
