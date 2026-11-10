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
package io.gravitee.gamma.module.platform.rest.resource.credential;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

// PermissionsFilter reads @Permissions off the matched resource method; a method without it is open to every
// authenticated user. Each endpoint must ask for exactly the credential action it performs.
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CredentialResourcesPermissionsTest {

    private static final List<Class<?>> RESOURCES = List.of(CredentialsResource.class, CredentialResource.class);

    private static final Map<String, RolePermissionAction> EXPECTED = Map.of(
        "CredentialsResource.list",
        RolePermissionAction.READ,
        "CredentialsResource.create",
        RolePermissionAction.CREATE,
        "CredentialResource.get",
        RolePermissionAction.READ,
        "CredentialResource.update",
        RolePermissionAction.UPDATE,
        "CredentialResource.rotate",
        RolePermissionAction.UPDATE,
        "CredentialResource.delete",
        RolePermissionAction.DELETE,
        "CredentialResource.consumers",
        RolePermissionAction.READ
    );

    private static Stream<Method> httpMethods() {
        return RESOURCES.stream()
            .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
            .filter(
                m ->
                    m.isAnnotationPresent(GET.class) ||
                    m.isAnnotationPresent(POST.class) ||
                    m.isAnnotationPresent(PUT.class) ||
                    m.isAnnotationPresent(PATCH.class) ||
                    m.isAnnotationPresent(DELETE.class)
            );
    }

    private static String name(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    @Test
    void should_cover_exactly_the_expected_endpoints() {
        assertThat(httpMethods().map(CredentialResourcesPermissionsTest::name).collect(Collectors.toSet())).isEqualTo(EXPECTED.keySet());
    }

    @Test
    void should_ask_each_endpoint_for_its_credential_action_only() {
        httpMethods().forEach(method -> {
            var permissions = method.getDeclaredAnnotation(Permissions.class);
            assertThat(permissions).as("@Permissions on %s", name(method)).isNotNull();
            assertThat(permissions.value())
                .as(name(method))
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.value()).isEqualTo(RolePermission.ENVIRONMENT_CREDENTIAL);
                    assertThat(p.acls()).containsExactly(EXPECTED.get(name(method)));
                });
        });
    }

    @Test
    void should_not_take_a_permission_annotation_off_a_class_level_default() {
        RESOURCES.forEach(resource -> assertThat(resource.getDeclaredAnnotation(Permissions.class)).as(resource.getSimpleName()).isNull());
    }
}
