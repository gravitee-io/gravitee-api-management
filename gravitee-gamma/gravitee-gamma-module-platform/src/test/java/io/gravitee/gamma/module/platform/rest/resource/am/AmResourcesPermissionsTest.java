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
package io.gravitee.gamma.module.platform.rest.resource.am;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// These resources call Access Management with the organization's service-account token, so the
// caller's own rights are the only gate. PermissionsFilter reads the annotation off the matched
// resource method; a method without it is open to every authenticated user.
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AmResourcesPermissionsTest {

    private static final List<Class<?>> AM_RESOURCES = List.of(
        AmConnectionResource.class,
        EnvironmentsResource.class,
        DomainsResource.class,
        DomainResource.class,
        DomainEntrypointsResource.class
    );

    static Stream<Method> httpMethods() {
        return AM_RESOURCES.stream()
            .flatMap(c -> Stream.of(c.getDeclaredMethods()))
            .filter(
                m ->
                    m.isAnnotationPresent(GET.class) ||
                    m.isAnnotationPresent(POST.class) ||
                    m.isAnnotationPresent(PUT.class) ||
                    m.isAnnotationPresent(PATCH.class) ||
                    m.isAnnotationPresent(DELETE.class)
            );
    }

    static Stream<Method> getMethods() {
        return httpMethods().filter(m -> m.isAnnotationPresent(GET.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("httpMethods")
    void every_endpoint_requires_the_am_configuration_permission(Method method) {
        var permissions = method.getDeclaredAnnotation(Permissions.class);

        assertThat(permissions).as("@Permissions on %s", method).isNotNull();
        assertThat(permissions.value()).extracting(Permission::value).containsOnly(RolePermission.ENVIRONMENT_AM_CONFIGURATION);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getMethods")
    void read_endpoints_require_the_read_action(Method method) {
        var permissions = method.getDeclaredAnnotation(Permissions.class);

        assertThat(permissions).isNotNull();
        assertThat(permissions.value()).allSatisfy(p -> assertThat(p.acls()).containsExactly(RolePermissionAction.READ));
    }

    @Test
    void every_resource_exposes_at_least_one_endpoint() {
        var covered = httpMethods().map(Method::getDeclaringClass).collect(Collectors.toSet());

        assertThat(covered).containsExactlyInAnyOrderElementsOf(AM_RESOURCES);
    }
}
