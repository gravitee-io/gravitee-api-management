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
package io.gravitee.apim.rest.api.automation.spring;

import static io.gravitee.apim.rest.api.automation.spring.PermissionsFilter.HRID_CONTAINS_API_QUERY_UUID_PARAM;
import static io.gravitee.apim.rest.api.automation.spring.PermissionsFilter.HRID_CONTAINS_UUID_QUERY_PARAM;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.rest.api.automation.resource.ApiDocumentationResource;
import io.gravitee.apim.rest.api.automation.resource.ApiLinkResource;
import io.gravitee.apim.rest.api.automation.resource.ApiResource;
import io.gravitee.apim.rest.api.automation.resource.ApiSubscriptionResource;
import io.gravitee.apim.rest.api.automation.resource.ApiSubscriptionsResource;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The permission filter picks the flag that turns {@code apiHrid} into an API id from the resource class handling the
 * request. A resource it does not list is authorized on the HRID-derived id, so such a resource must not read an API id
 * flag of its own.
 */
class PermissionsFilterApiIdFlagsTest {

    private static final Set<String> API_ID_FLAGS = Set.of(HRID_CONTAINS_UUID_QUERY_PARAM, HRID_CONTAINS_API_QUERY_UUID_PARAM);

    // The classes PermissionsFilter#apiHridContainsUUID reads a flag for
    private static final Set<Class<?>> FLAG_AWARE_RESOURCES = Set.of(
        ApiResource.class,
        ApiSubscriptionsResource.class,
        ApiSubscriptionResource.class
    );

    @Test
    void should_walk_every_resource_under_the_api_path() {
        assertThat(resourcesUnder(ApiResource.class)).contains(
            ApiSubscriptionResource.class,
            ApiDocumentationResource.class,
            ApiLinkResource.class
        );
    }

    @Test
    void should_read_api_id_flags_only_in_resources_the_permission_filter_knows() {
        assertThat(resourcesUnder(ApiResource.class))
            .filteredOn(resource -> !FLAG_AWARE_RESOURCES.contains(resource))
            .filteredOn(PermissionsFilterApiIdFlagsTest::readsAnApiIdFlag)
            .isEmpty();
    }

    private static Set<Class<?>> resourcesUnder(Class<?> root) {
        Set<Class<?>> resources = new LinkedHashSet<>();
        collect(root, resources);
        return resources;
    }

    private static void collect(Class<?> resource, Set<Class<?>> resources) {
        if (!resources.add(resource)) {
            return;
        }
        Arrays.stream(resource.getMethods())
            .filter(PermissionsFilterApiIdFlagsTest::isSubResourceLocator)
            .forEach(locator -> collect(locator.getReturnType(), resources));
    }

    private static boolean isSubResourceLocator(Method method) {
        return method.isAnnotationPresent(Path.class) && !isResourceMethod(method);
    }

    private static boolean isResourceMethod(Method method) {
        return Arrays.stream(method.getAnnotations()).anyMatch(annotation ->
            annotation.annotationType().isAnnotationPresent(HttpMethod.class)
        );
    }

    private static boolean readsAnApiIdFlag(Class<?> resource) {
        Stream<AnnotatedElement> injectionPoints = Stream.concat(
            Arrays.stream(resource.getDeclaredFields()),
            Arrays.stream(resource.getMethods())
                .filter(PermissionsFilterApiIdFlagsTest::isResourceMethod)
                .flatMap(method -> Arrays.stream(method.getParameters()))
        );
        return injectionPoints
            .map(element -> element.getAnnotation(QueryParam.class))
            .anyMatch(queryParam -> queryParam != null && API_ID_FLAGS.contains(queryParam.value()));
    }
}
