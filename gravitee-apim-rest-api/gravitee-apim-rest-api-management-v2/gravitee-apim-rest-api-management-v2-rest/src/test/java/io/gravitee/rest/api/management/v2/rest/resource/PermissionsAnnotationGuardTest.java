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

import io.gravitee.rest.api.rest.annotation.Permissions;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Fails when a new HTTP resource method ships without Permissions.
 * Methods without the annotation are listed in ALLOWLIST with a reason.
 */
class PermissionsAnnotationGuardTest {

    /**
     * Class#verb path keys allowed to lack Permissions.
     * A reviewed entry names the check. "pre-existing, not reviewed yet" is a backlog, not a sign-off.
     * Prefer adding Permissions over growing this map.
     */
    private static final Map<String, String> ALLOWLIST = Map.ofEntries(
        Map.entry("AnalyticsComputationResource#POST /facets", "pre-existing, not reviewed yet"),
        Map.entry("AnalyticsComputationResource#POST /measures", "pre-existing, not reviewed yet"),
        Map.entry("AnalyticsComputationResource#POST /time-series", "pre-existing, not reviewed yet"),
        Map.entry("ApiProductMembersResource#GET /members/permissions", "permission checked inside the method"),
        Map.entry("ApiResource#GET /{apiId}", "pre-existing, not reviewed yet"),
        Map.entry("ApiResource#POST /{apiId}/_duplicate", "permission checked inside the method"),
        Map.entry("ApiServicesResource#GET /plugins/api-services/{apiServiceId}/schema", "pre-existing, not reviewed yet"),
        Map.entry("ClusterResource#GET /{clusterId}/permissions", "permission checked inside the method"),
        Map.entry("DashboardResource#GET /{dashboardId}", "pre-existing, not reviewed yet"),
        Map.entry("DashboardsResource#GET /dashboards", "pre-existing, not reviewed yet"),
        Map.entry("EndpointsResource#GET /plugins/endpoints", "plugin catalog / schema read"),
        Map.entry("EndpointsResource#GET /plugins/endpoints/{endpointId}", "plugin catalog / schema read"),
        Map.entry("EndpointsResource#GET /plugins/endpoints/{endpointId}/documentation", "plugin catalog / schema read"),
        Map.entry("EndpointsResource#GET /plugins/endpoints/{endpointId}/more-information", "plugin catalog / schema read"),
        Map.entry("EndpointsResource#GET /plugins/endpoints/{endpointId}/schema", "plugin catalog / schema read"),
        Map.entry("EndpointsResource#GET /plugins/endpoints/{endpointId}/shared-configuration-schema", "plugin catalog / schema read"),
        Map.entry("EntrypointsResource#GET /plugins/entrypoints", "plugin catalog / schema read"),
        Map.entry("EntrypointsResource#GET /plugins/entrypoints/{entrypointId}", "plugin catalog / schema read"),
        Map.entry("EntrypointsResource#GET /plugins/entrypoints/{entrypointId}/documentation", "plugin catalog / schema read"),
        Map.entry("EntrypointsResource#GET /plugins/entrypoints/{entrypointId}/more-information", "plugin catalog / schema read"),
        Map.entry("EntrypointsResource#GET /plugins/entrypoints/{entrypointId}/schema", "plugin catalog / schema read"),
        Map.entry("EntrypointsResource#GET /plugins/entrypoints/{entrypointId}/subscription-schema", "plugin catalog / schema read"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics/error-keys", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics/request-response-time", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics/response-status-overtime", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics/response-status-ranges", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics/response-time-over-time", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics/top-apps-by-request-count", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics/top-failed-apis", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics/top-hits", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentResource#GET /{envId}", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentsResource#GET /environments", "pre-existing, not reviewed yet"),
        Map.entry("GraviteeLicenseResource#GET /license", "pre-existing, not reviewed yet"),
        Map.entry("GroupsResource#GET /environments/{envId}/groups", "pre-existing, not reviewed yet"),
        Map.entry(
            "GroupsResource#GET /environments/{envId}/groups/{groupId}/members",
            "permission checked inside the method (GROUP_MEMBER or ENVIRONMENT_GROUP READ)"
        ),
        Map.entry("GroupsResource#GET /environments/{envId}/groups/{groupId}/permissions", "permission checked inside the method"),
        Map.entry("GroupsResource#GET /groups", "pre-existing, not reviewed yet"),
        Map.entry(
            "GroupsResource#GET /groups/{groupId}/members",
            "permission checked inside the method (GROUP_MEMBER or ENVIRONMENT_GROUP READ)"
        ),
        Map.entry("GroupsResource#GET /groups/{groupId}/permissions", "permission checked inside the method"),
        Map.entry("GroupsResource#POST /environments/{envId}/groups/_search", "pre-existing, not reviewed yet"),
        Map.entry("GroupsResource#POST /groups/_search", "pre-existing, not reviewed yet"),
        Map.entry("IntegrationResource#GET /{integrationId}/permissions", "permission checked inside the method"),
        Map.entry("LogsSearchResource#POST /search", "pre-existing, not reviewed yet"),
        Map.entry("ManagementUIResource#GET /ui/bootstrap", "pre-existing, not reviewed yet"),
        Map.entry("ManagementUIResource#GET /ui/customization", "pre-existing, not reviewed yet"),
        Map.entry("ObservabilityFilterLabelsResource#POST /filters/resolve", "pre-existing, not reviewed yet"),
        Map.entry("ObservabilityFilterValuesResource#GET /filters/{filterName}/values", "pre-existing, not reviewed yet"),
        Map.entry("ObservabilityFiltersDefinitionResource#GET /filters/definition", "pre-existing, not reviewed yet"),
        Map.entry("OpenAPIResource#GET /", "public OpenAPI / docs surface"),
        Map.entry("OpenAPIResource#GET /index-{type}.html", "public OpenAPI / docs surface"),
        Map.entry("OpenAPIResource#GET /openapi-{type}.yaml", "public OpenAPI / docs surface"),
        Map.entry("OrganizationResource#GET /organizations/{orgId}", "pre-existing, not reviewed yet"),
        Map.entry("OrganizationResource#GET /organizations/{orgId}/license", "pre-existing, not reviewed yet"),
        Map.entry("PoliciesResource#GET /plugins/policies", "plugin catalog / schema read"),
        Map.entry("PoliciesResource#GET /plugins/policies/{policyId}", "plugin catalog / schema read"),
        Map.entry("PoliciesResource#GET /plugins/policies/{policyId}/documentation", "plugin catalog / schema read"),
        Map.entry("PoliciesResource#GET /plugins/policies/{policyId}/documentation-ext", "plugin catalog / schema read"),
        Map.entry("PoliciesResource#GET /plugins/policies/{policyId}/schema", "plugin catalog / schema read"),
        Map.entry(
            "PromotionsResource#POST /promotions/{promotionId}/_process",
            "permission checked inside the method (ENVIRONMENT_API CREATE or UPDATE on the target)"
        ),
        Map.entry("ResourcesResource#GET /plugins/resources", "plugin catalog / schema read"),
        Map.entry("ResourcesResource#GET /plugins/resources/{resourceId}", "plugin catalog / schema read"),
        Map.entry("ResourcesResource#GET /plugins/resources/{resourceId}/schema", "plugin catalog / schema read")
    );

    private static final List<Class<? extends Annotation>> HTTP_METHODS = List.of(
        GET.class,
        POST.class,
        PUT.class,
        DELETE.class,
        PATCH.class,
        HEAD.class
    );

    @Test
    void http_resource_methods_must_declare_permissions_or_be_allowlisted() throws Exception {
        List<Class<?>> classes = findClasses("io.gravitee.rest.api.management.v2.rest.resource");
        assertThat(liveEndpointKeys(classes))
            .as("HTTP methods missing @Permissions (add annotation or allowlist with a reason)")
            .isEqualTo(ALLOWLIST.keySet());
        assertThat(ALLOWLIST.keySet()).allSatisfy(key -> assertThat(key).matches(".+#(GET|POST|PUT|DELETE|PATCH|HEAD) /\\S*"));
        assertThat(ALLOWLIST.values()).allSatisfy(reason -> assertThat(reason).isNotBlank());
    }

    private static boolean isResourceClass(Class<?> clazz) {
        if (clazz.isInterface() || clazz.isEnum() || clazz.isAnnotation() || Modifier.isAbstract(clazz.getModifiers())) {
            return false;
        }
        String name = clazz.getSimpleName();
        return name.endsWith("Resource") || name.endsWith("Resources");
    }

    private static boolean hasHttpMethod(Method method) {
        return HTTP_METHODS.stream().anyMatch(method::isAnnotationPresent);
    }

    private static List<Class<?>> findClasses(String basePackage) throws IOException, ClassNotFoundException {
        String path = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);
        List<Class<?>> classes = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue;
            }
            Path root = Path.of(URI.create(resource.toString()));
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path classFile : walk.filter(p -> p.toString().endsWith(".class")).collect(Collectors.toList())) {
                    String relative = root.relativize(classFile).toString().replace('/', '.').replace('\\', '.');
                    if (relative.contains("$")) {
                        continue;
                    }
                    String className = basePackage + '.' + relative.substring(0, relative.length() - ".class".length());
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }

    private static Set<String> liveEndpointKeys(List<Class<?>> classes) {
        Set<String> live = new TreeSet<>();
        for (Class<?> resourceClass : classes) {
            if (!isResourceClass(resourceClass) || resourceClass.isAnnotationPresent(Permissions.class)) {
                continue;
            }
            for (Method method : resourceClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic() || !hasHttpMethod(method)) {
                    continue;
                }
                if (method.isAnnotationPresent(Permissions.class)) {
                    continue;
                }
                live.addAll(endpointKeys(resourceClass, method, classes));
            }
        }
        return live;
    }

    private static Set<String> endpointKeys(Class<?> resourceClass, Method method, List<Class<?>> classes) {
        Set<String> keys = new TreeSet<>();
        for (String root : resourceRoots(resourceClass, classes)) {
            String path = joinPath(root, methodPath(method));
            for (Class<? extends Annotation> verb : HTTP_METHODS) {
                if (method.isAnnotationPresent(verb)) {
                    keys.add(resourceClass.getSimpleName() + "#" + verb.getSimpleName() + " " + path);
                }
            }
        }
        return keys;
    }

    private static Set<String> resourceRoots(Class<?> type, List<Class<?>> classes) {
        Set<String> roots = new TreeSet<>();
        String declared = declaredClassPath(type);
        if (!declared.isEmpty()) {
            roots.add(declared);
        }
        for (Class<?> owner : classes) {
            if (owner.equals(type)) {
                continue;
            }
            for (Method method : owner.getDeclaredMethods()) {
                if (method.isSynthetic() || !type.equals(method.getReturnType())) {
                    continue;
                }
                jakarta.ws.rs.Path path = method.getAnnotation(jakarta.ws.rs.Path.class);
                if (path != null) {
                    roots.add(normalizePath(path.value()));
                }
            }
        }
        if (roots.isEmpty()) {
            roots.add("/");
        }
        return roots;
    }

    private static String declaredClassPath(Class<?> type) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            jakarta.ws.rs.Path path = current.getAnnotation(jakarta.ws.rs.Path.class);
            if (path != null) {
                return normalizePath(path.value());
            }
        }
        return "";
    }

    private static String methodPath(Method method) {
        jakarta.ws.rs.Path path = method.getAnnotation(jakarta.ws.rs.Path.class);
        return path == null ? "" : path.value();
    }

    private static String joinPath(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        if (normalizedLeft.isEmpty() || "/".equals(normalizedLeft)) {
            return normalizePath(normalizedRight.isEmpty() ? "/" : normalizedRight);
        }
        if (normalizedRight.isEmpty() || "/".equals(normalizedRight)) {
            return normalizePath(normalizedLeft);
        }
        if (normalizedLeft.endsWith("/")) {
            normalizedLeft = normalizedLeft.substring(0, normalizedLeft.length() - 1);
        }
        if (!normalizedRight.startsWith("/")) {
            normalizedRight = "/" + normalizedRight;
        }
        return normalizePath(normalizedLeft + normalizedRight);
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/{2,}", "/");
    }
}
