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
package io.gravitee.gamma.authorization.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthzPermissionContractTest {

    private static final Set<Class<? extends Annotation>> JAXRS_HTTP_METHOD_ANNOTATIONS = Set.of(
        GET.class,
        POST.class,
        PUT.class,
        DELETE.class
    );

    private static final List<Class<?>> AUTHZ_REST_RESOURCES = List.of(
        AuthzPoliciesResource.class,
        AuthzEntitiesResource.class,
        AuthzSchemaResource.class,
        AuthzAmUserSyncResource.class
    );

    @Test
    @DisplayName("every JAX-RS endpoint on every authz REST resource carries @Permissions")
    void every_jaxrs_endpoint_carries_permissions_annotation() {
        for (Class<?> resource : AUTHZ_REST_RESOURCES) {
            for (Method method : resource.getDeclaredMethods()) {
                if (!isJaxRsEndpoint(method)) {
                    continue;
                }
                assertThat(method.getAnnotation(Permissions.class))
                    .as("Resource %s.%s is a JAX-RS endpoint but lacks @Permissions", resource.getSimpleName(), method.getName())
                    .isNotNull();
            }
        }
    }

    @Test
    @DisplayName("every @Permissions on authz resources references RolePermission.ENVIRONMENT_AUTHORIZATION")
    void every_permission_targets_environment_authorization() {
        for (Class<?> resource : AUTHZ_REST_RESOURCES) {
            for (Method method : resource.getDeclaredMethods()) {
                Permissions annotation = method.getAnnotation(Permissions.class);
                if (annotation == null) {
                    continue;
                }
                for (Permission permission : annotation.value()) {
                    assertThat(permission.value())
                        .as("Resource %s.%s pins @Permission to a non-authz RolePermission", resource.getSimpleName(), method.getName())
                        .isEqualTo(RolePermission.ENVIRONMENT_AUTHORIZATION);
                }
            }
        }
    }

    private static final Map<String, Set<RolePermissionAction>> EXPECTED_ACLS_BY_METHOD;

    static {
        Map<String, Set<RolePermissionAction>> m = new LinkedHashMap<>();
        m.put("AuthzPoliciesResource.create", Set.of(RolePermissionAction.CREATE));
        m.put("AuthzPoliciesResource.list", Set.of(RolePermissionAction.READ));
        m.put("AuthzPoliciesResource.findById", Set.of(RolePermissionAction.READ));
        m.put("AuthzPoliciesResource.update", Set.of(RolePermissionAction.UPDATE));
        m.put("AuthzPoliciesResource.deploy", Set.of(RolePermissionAction.UPDATE));
        m.put("AuthzPoliciesResource.disable", Set.of(RolePermissionAction.UPDATE));
        m.put("AuthzPoliciesResource.delete", Set.of(RolePermissionAction.DELETE));
        m.put("AuthzEntitiesResource.upsert", Set.of(RolePermissionAction.CREATE, RolePermissionAction.UPDATE));
        m.put("AuthzEntitiesResource.list", Set.of(RolePermissionAction.READ));
        m.put("AuthzEntitiesResource.findByEntityId", Set.of(RolePermissionAction.READ));
        m.put("AuthzEntitiesResource.update", Set.of(RolePermissionAction.UPDATE));
        m.put("AuthzEntitiesResource.delete", Set.of(RolePermissionAction.DELETE));
        m.put("AuthzEntitiesResource.migrateEntityTypes", Set.of(RolePermissionAction.UPDATE));
        m.put("AuthzSchemaResource.currentSchema", Set.of(RolePermissionAction.READ));
        m.put("AuthzSchemaResource.parsedSchema", Set.of(RolePermissionAction.READ));
        m.put("AuthzSchemaResource.validateSchema", Set.of(RolePermissionAction.READ));
        m.put("AuthzSchemaResource.updateSchema", Set.of(RolePermissionAction.UPDATE));
        m.put("AuthzSchemaResource.deleteSchema", Set.of(RolePermissionAction.DELETE));
        m.put("AuthzAmUserSyncResource.sync", Set.of(RolePermissionAction.CREATE, RolePermissionAction.UPDATE));
        m.put("AuthzAmUserSyncResource.status", Set.of(RolePermissionAction.READ));
        EXPECTED_ACLS_BY_METHOD = Map.copyOf(m);
    }

    @Test
    @DisplayName("each authz endpoint declares the exact RolePermissionAction set the contract requires")
    void each_endpoint_has_exact_expected_acls() {
        for (Class<?> resource : AUTHZ_REST_RESOURCES) {
            for (Method method : resource.getDeclaredMethods()) {
                if (!isJaxRsEndpoint(method)) {
                    continue;
                }
                String key = resource.getSimpleName() + "." + method.getName();
                Set<RolePermissionAction> expected = EXPECTED_ACLS_BY_METHOD.get(key);
                assertThat(expected).as("Endpoint %s is not registered in EXPECTED_ACLS_BY_METHOD — add it explicitly", key).isNotNull();

                Permissions annotation = method.getAnnotation(Permissions.class);
                Set<RolePermissionAction> actual = Arrays.stream(annotation.value())
                    .flatMap(p -> Arrays.stream(p.acls()))
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

                assertThat(actual).as("Endpoint %s does not declare the expected ACL set", key).isEqualTo(expected);
            }
        }
    }

    @Test
    @DisplayName("EXPECTED_ACLS_BY_METHOD only references endpoints that still exist")
    void expected_acls_map_has_no_stale_entries() {
        Set<String> liveEndpoints = AUTHZ_REST_RESOURCES.stream()
            .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
            .filter(AuthzPermissionContractTest::isJaxRsEndpoint)
            .map(m -> m.getDeclaringClass().getSimpleName() + "." + m.getName())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        assertThat(liveEndpoints)
            .as("EXPECTED_ACLS_BY_METHOD contains a key that no longer maps to a live endpoint")
            .containsAll(EXPECTED_ACLS_BY_METHOD.keySet());
    }

    private static boolean isJaxRsEndpoint(Method method) {
        return Arrays.stream(method.getAnnotations()).anyMatch(a -> JAXRS_HTTP_METHOD_ANNOTATIONS.contains(a.annotationType()));
    }

    private static String httpVerbOf(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            HttpMethod httpMethod = annotation.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
                return httpMethod.value();
            }
        }
        return "";
    }
}
