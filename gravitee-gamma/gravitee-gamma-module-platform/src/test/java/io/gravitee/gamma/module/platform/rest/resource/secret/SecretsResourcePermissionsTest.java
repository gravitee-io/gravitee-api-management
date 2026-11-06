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
package io.gravitee.gamma.module.platform.rest.resource.secret;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// PermissionsFilter reads the annotation off the matched resource method; a method without it is open to every authenticated user.
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretsResourcePermissionsTest {

    private static final Map<Class<? extends Annotation>, RolePermissionAction> ACTION_PER_VERB = Map.of(
        GET.class,
        RolePermissionAction.READ,
        POST.class,
        RolePermissionAction.CREATE,
        PUT.class,
        RolePermissionAction.UPDATE,
        DELETE.class,
        RolePermissionAction.DELETE
    );

    static Stream<Method> httpMethods() {
        return Stream.of(SecretsResource.class.getDeclaredMethods()).filter(m ->
            ACTION_PER_VERB.keySet()
                .stream()
                .anyMatch(verb -> m.isAnnotationPresent(verb))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("httpMethods")
    void every_endpoint_requires_the_organization_secret_permission_for_its_verb(Method method) {
        var expected = ACTION_PER_VERB.entrySet()
            .stream()
            .filter(e -> method.isAnnotationPresent(e.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow();

        var permissions = method.getDeclaredAnnotation(Permissions.class);

        assertThat(permissions).as("@Permissions on %s", method).isNotNull();
        assertThat(permissions.value())
            .singleElement()
            .satisfies(permission -> {
                assertThat(permission.value()).isEqualTo(RolePermission.ORGANIZATION_SECRET);
                assertThat(permission.acls()).containsExactly(expected);
            });
    }

    @Test
    void every_verb_has_an_endpoint() {
        assertThat(httpMethods().count()).isEqualTo(5);
    }
}
