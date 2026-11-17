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
package io.gravitee.rest.api.management.v2.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permissions;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Every resource method that requires {@code API_SUBSCRIPTION[READ]}, through {@link Permissions} or by hand, also refuses a member
 * who cannot manage the API, so an endpoint added later cannot leave the API's subscriptions to a member who only reads the API.
 */
class ApiSubscriptionReadGuardTest {

    private static final Set<String> GUARDS = Set.of("requireCanManageApi", "findManageableApi");

    /** {@code Class#method} requiring {@code API_SUBSCRIPTION[READ]} that may skip the guard, with the reason. */
    private static final Map<String, String> EXCEPTIONS = Map.of();

    private static final JavaClasses RESOURCES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("io.gravitee.rest.api.management.v2.rest.resource");

    @Test
    void should_refuse_the_api_subscriptions_to_a_member_who_cannot_manage_the_api_on_every_endpoint_reading_them() {
        Set<JavaMethod> subscriptionReads = RESOURCES.stream()
            .flatMap(javaClass -> javaClass.getMethods().stream())
            .filter(method -> requiresSubscriptionReadByAnnotation(method) || requiresSubscriptionReadByHand(method))
            .collect(Collectors.toSet());

        Set<String> unguarded = subscriptionReads
            .stream()
            .filter(method ->
                method
                    .getMethodCallsFromSelf()
                    .stream()
                    .noneMatch(call -> GUARDS.contains(call.getTarget().getName()))
            )
            .map(ApiSubscriptionReadGuardTest::name)
            .collect(Collectors.toSet());

        assertThat(subscriptionReads).as("the scan finds the endpoints reading an API's subscriptions").isNotEmpty();
        assertThat(unguarded)
            .as("endpoints requiring API_SUBSCRIPTION[READ] without calling one of %s", GUARDS)
            .isEqualTo(EXCEPTIONS.keySet());
    }

    private static boolean requiresSubscriptionReadByAnnotation(JavaMethod method) {
        return method
            .tryGetAnnotationOfType(Permissions.class)
            .stream()
            .flatMap(permissions -> Arrays.stream(permissions.value()))
            .anyMatch(
                permission ->
                    permission.value() == RolePermission.API_SUBSCRIPTION &&
                    Arrays.asList(permission.acls()).contains(RolePermissionAction.READ)
            );
    }

    private static boolean requiresSubscriptionReadByHand(JavaMethod method) {
        return (
            readsConstant(method, RolePermission.class, RolePermission.API_SUBSCRIPTION.name()) &&
            readsConstant(method, RolePermissionAction.class, RolePermissionAction.READ.name())
        );
    }

    private static boolean readsConstant(JavaMethod method, Class<?> owner, String name) {
        return method
            .getFieldAccesses()
            .stream()
            .anyMatch(access -> access.getTarget().getOwner().isEquivalentTo(owner) && access.getTarget().getName().equals(name));
    }

    private static String name(JavaMethod method) {
        return method.getOwner().getSimpleName() + "#" + method.getName();
    }
}
