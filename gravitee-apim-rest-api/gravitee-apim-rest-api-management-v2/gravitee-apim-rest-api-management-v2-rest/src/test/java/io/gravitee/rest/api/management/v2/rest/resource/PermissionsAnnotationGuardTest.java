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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Fails when a new HTTP resource method ships without Permissions.
 * Existing intentional exceptions live in ALLOWLIST.
 */
class PermissionsAnnotationGuardTest {

    /**
     * Class#method keys allowed to lack Permissions (catalog, OpenAPI, auth,
     * APIM-14627 scoring rulesets/functions, and other pre-existing open reads).
     */
    private static final Set<String> ALLOWLIST = Set.of(
        "AnalyticsComputationResource#computeFacets",
        "AnalyticsComputationResource#computeMeasures",
        "AnalyticsComputationResource#computeTimeSeries",
        "ApiProductMembersResource#getApiProductMemberPermissions",
        "ApiResource#duplicateAPI",
        "ApiResource#getApiById",
        "ApiServicesResource#getApiServiceSchema",
        "ApiSubscriptionsResource#deleteSubscriptionSpec",
        "ApiSubscriptionsResource#importSubscriptionSpec",
        "ClusterResource#getClusterPermissions",
        "DashboardResource#getDashboard",
        "DashboardsResource#listDashboards",
        "EndpointsResource#getEndpoint",
        "EndpointsResource#getEndpointDocumentation",
        "EndpointsResource#getEndpointMoreInformation",
        "EndpointsResource#getEndpointSchema",
        "EndpointsResource#getEndpointSharedConfigurationSchema",
        "EndpointsResource#getEndpoints",
        "EntrypointsResource#getEntrypoint",
        "EntrypointsResource#getEntrypointDocumentation",
        "EntrypointsResource#getEntrypointMoreInformation",
        "EntrypointsResource#getEntrypointSchema",
        "EntrypointsResource#getEntrypointSubscriptionSchema",
        "EntrypointsResource#getEntrypoints",
        "EnvironmentAnalyticsResource#getEnvironmentLogErrorKeys",
        "EnvironmentAnalyticsResource#getRequestResponseTime",
        "EnvironmentAnalyticsResource#getResponseStatusOvertime",
        "EnvironmentAnalyticsResource#getResponseStatusRanges",
        "EnvironmentAnalyticsResource#getResponseTimeOverTime",
        "EnvironmentAnalyticsResource#getTopAppsByRequestCount",
        "EnvironmentAnalyticsResource#getTopFailedApis",
        "EnvironmentAnalyticsResource#getTopHitsApis",
        "EnvironmentResource#getEnvironment",
        "EnvironmentScoringFunctionResource#deleteFunction",
        "EnvironmentScoringFunctionsResource#importFunction",
        "EnvironmentScoringFunctionsResource#listFunctions",
        "EnvironmentScoringRulesetResource#deleteRuleset",
        "EnvironmentScoringRulesetResource#getRulesetById",
        "EnvironmentScoringRulesetResource#updateRuleset",
        "EnvironmentScoringRulesetsResource#importRuleset",
        "EnvironmentScoringRulesetsResource#listRulesets",
        "EnvironmentsResource#getEnvironments",
        "GraviteeLicenseResource#get",
        "GroupsResource#getPermissions",
        "GroupsResource#listGroupMembers",
        "GroupsResource#listGroups",
        "GroupsResource#searchGroups",
        "IntegrationResource#getPermissions",
        "LogsSearchResource#searchLogs",
        "ManagementUIResource#bootstrap",
        "ManagementUIResource#getConsoleCustomization",
        "ObservabilityFilterLabelsResource#resolveFilterLabels",
        "ObservabilityFilterValuesResource#getFilterValues",
        "ObservabilityFiltersDefinitionResource#getFilterDefinitions",
        "OpenAPIResource#getIndexHtml",
        "OpenAPIResource#getOpenApi",
        "OpenAPIResource#getOpenApiDocumentation",
        "OrganizationResource#getOrganizationById",
        "OrganizationResource#getOrganizationLicense",
        "PoliciesResource#getPolicies",
        "PoliciesResource#getPolicy",
        "PoliciesResource#getPolicyDocumentation",
        "PoliciesResource#getPolicyDocumentationWithExtension",
        "PoliciesResource#getPolicySchema",
        "PromotionsResource#processPromotion",
        "ResourcesResource#getResource",
        "ResourcesResource#getResourceSchema",
        "ResourcesResource#getResources"
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
        Set<String> violations = new TreeSet<>();

        for (Class<?> resourceClass : findClasses("io.gravitee.rest.api.management.v2.rest.resource")) {
            if (!isResourceClass(resourceClass)) {
                continue;
            }
            boolean classLevel = resourceClass.isAnnotationPresent(Permissions.class);
            for (Method method : resourceClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || !hasHttpMethod(method)) {
                    continue;
                }
                String key = resourceClass.getSimpleName() + "#" + method.getName();
                if (classLevel || method.isAnnotationPresent(Permissions.class)) {
                    continue;
                }
                if (ALLOWLIST.contains(key)) {
                    continue;
                }
                violations.add(key);
            }
        }

        assertThat(violations).as("HTTP methods missing @Permissions (add annotation or allowlist with a reason)").isEmpty();
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
}
