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
package io.gravitee.rest.api.management.rest.resource;

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
        Map.entry("AlertsResource#GET /alerts/metrics", "pre-existing, not reviewed yet"),
        Map.entry("ApiMediaResource#GET /{api}/media/{hash}", "pre-existing, not reviewed yet"),
        Map.entry("ApiMembersResource#GET /members/permissions", "permission checked inside the method"),
        Map.entry("ApiNotificationSettingsResource#POST /notificationsettings", "permission checked inside the method (API_NOTIFICATION)"),
        Map.entry("ApiPageResource#GET /{page}", "pre-existing, not reviewed yet"),
        Map.entry("ApiPageResource#GET /{page}/content", "pre-existing, not reviewed yet"),
        Map.entry("ApiPagesResource#GET /pages", "pre-existing, not reviewed yet"),
        Map.entry("ApiPlansResource#GET /plans", "pre-existing, not reviewed yet"),
        Map.entry("ApiPlansResource#GET /plans/{plan}", "pre-existing, not reviewed yet"),
        Map.entry("ApiRatingResource#GET /ratings", "pre-existing, not reviewed yet"),
        Map.entry("ApiRatingResource#GET /ratings/current", "pre-existing, not reviewed yet"),
        Map.entry("ApiRatingResource#GET /ratings/summary", "pre-existing, not reviewed yet"),
        Map.entry("ApiResource#GET /{api}", "pre-existing, not reviewed yet"),
        Map.entry("ApiResource#GET /{api}/background", "pre-existing, not reviewed yet"),
        Map.entry("ApiResource#GET /{api}/headers", "pre-existing, not reviewed yet"),
        Map.entry("ApiResource#GET /{api}/picture", "pre-existing, not reviewed yet"),
        Map.entry("ApiResource#GET /{api}/quality", "pre-existing, not reviewed yet"),
        Map.entry("ApiResource#GET /{api}/state", "pre-existing, not reviewed yet"),
        Map.entry("ApiSubscribersResource#GET /subscribers", "pre-existing, not reviewed yet"),
        Map.entry("ApiSubscribersResource#GET /subscribers/_paged", "pre-existing, not reviewed yet"),
        Map.entry("ApisResource#GET /apis", "pre-existing, not reviewed yet"),
        Map.entry("ApisResource#GET /apis/_paged", "pre-existing, not reviewed yet"),
        Map.entry("ApisResource#GET /apis/hooks", "pre-existing, not reviewed yet"),
        Map.entry("ApisResource#POST /apis/_search", "pre-existing, not reviewed yet"),
        Map.entry("ApisResource#POST /apis/_search/_paged", "pre-existing, not reviewed yet"),
        Map.entry(
            "ApisResource#PUT /apis/import",
            "service checks API_DEFINITION[UPDATE] and refuses URL fetch without create/update rights"
        ),
        Map.entry("ApplicationMembersResource#GET /members/permissions", "permission checked inside the method"),
        Map.entry(
            "ApplicationNotificationSettingsResource#POST /notificationsettings",
            "permission checked inside the method (APPLICATION_NOTIFICATION)"
        ),
        Map.entry("ApplicationResource#POST /{application}/_restore", "permission checked inside the method via isAdmin()"),
        Map.entry("ApplicationsResource#GET /applications/hooks", "pre-existing, not reviewed yet"),
        Map.entry("CategoriesResource#GET /categories", "pre-existing, not reviewed yet"),
        Map.entry("CategoryResource#GET /{categoryId}", "portal / configuration read"),
        Map.entry("CategoryResource#GET /{categoryId}/background", "portal / configuration read"),
        Map.entry("CategoryResource#GET /{categoryId}/picture", "portal / configuration read"),
        Map.entry("CockpitAuthenticationResource#GET /auth/cockpit", "authentication / registration"),
        Map.entry("ConsoleResource#GET /console", "pre-existing, not reviewed yet"),
        Map.entry("CurrentUserResource#DELETE /user", "current-user self-service"),
        Map.entry("CurrentUserResource#GET /user", "current-user self-service"),
        Map.entry("CurrentUserResource#GET /user/avatar", "current-user self-service"),
        Map.entry("CurrentUserResource#GET /user/tags", "current-user self-service"),
        Map.entry("CurrentUserResource#GET /user/tasks", "current-user self-service"),
        Map.entry("CurrentUserResource#POST /user/login", "authentication / registration"),
        Map.entry("CurrentUserResource#POST /user/logout", "current-user self-service"),
        Map.entry("CurrentUserResource#PUT /user", "current-user self-service"),
        Map.entry("CustomUserFieldsResource#GET /custom-user-fields", "pre-existing, not reviewed yet"),
        Map.entry("DashboardsResource#GET /dashboards", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentAnalyticsResource#GET /analytics", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentConfigurationResource#GET /configuration/applications/types", "portal / configuration read"),
        Map.entry("EnvironmentConfigurationResource#GET /configuration/hooks", "portal / configuration read"),
        Map.entry("EnvironmentConfigurationResource#GET /configuration/spel/grammar", "portal / configuration read"),
        Map.entry("EnvironmentResource#GET /{envId}", "pre-existing, not reviewed yet"),
        Map.entry("EnvironmentResource#GET /{envId}/permissions", "permission checked inside the method"),
        Map.entry("EnvironmentsResource#GET /environments", "pre-existing, not reviewed yet"),
        Map.entry("ExternalAuthenticationResource#GET /auth/external", "authentication / registration"),
        Map.entry("FetcherResource#GET /{fetcher}", "pre-existing, not reviewed yet"),
        Map.entry("FetcherResource#GET /{fetcher}/schema", "pre-existing, not reviewed yet"),
        Map.entry("FetchersResource#GET /fetchers", "plugin catalog / schema read"),
        Map.entry("FlowsResource#GET /flows", "pre-existing, not reviewed yet"),
        Map.entry("FlowsResource#GET /flows/configuration-schema", "hooks / config metadata read"),
        Map.entry("FlowsResource#GET /flows/flow-schema", "hooks / config metadata read"),
        Map.entry("NewsletterResource#GET /newsletter/taglines", "pre-existing, not reviewed yet"),
        Map.entry("NewsletterResource#POST /newsletter/_subscribe", "pre-existing, not reviewed yet"),
        Map.entry(
            "NotificationConfigsResource#POST /notificationsettings",
            "permission checked inside the method (ENVIRONMENT_NOTIFICATION)"
        ),
        Map.entry("OAuth2AuthenticationResource#POST /auth/oauth2/{identity}", "authentication / registration"),
        Map.entry("OAuth2AuthenticationResource#POST /auth/oauth2/{identity}/exchange", "authentication / registration"),
        Map.entry("OpenAPIResource#GET /openapi.{type:json|yaml}", "public OpenAPI / docs surface"),
        Map.entry("PasswordPolicyResource#GET /password-policy", "authentication / registration"),
        Map.entry("PlansResource#GET /plans", "hooks / config metadata read"),
        Map.entry("PlatformTicketsResource#GET /tickets", "pre-existing, not reviewed yet"),
        Map.entry("PlatformTicketsResource#GET /tickets/{ticket}", "pre-existing, not reviewed yet"),
        Map.entry(
            "PlatformTicketsResource#POST /tickets",
            "permission checked inside the method (API_DEFINITION and APPLICATION_DEFINITION READ when set)"
        ),
        Map.entry("PortalApisResource#POST /apis/_search", "portal / configuration read"),
        Map.entry("PortalEntrypointsResource#GET /entrypoints", "portal / configuration read"),
        Map.entry("PortalMediaResource#GET /media/{hash}", "portal / configuration read"),
        Map.entry("PortalRedirectResource#GET /redirect", "portal / configuration read"),
        Map.entry("PortalResource#GET /portal", "portal / configuration read"),
        Map.entry(
            "PromotionResource#POST /{promotion}/_process",
            "permission checked in PromotionServiceImpl (ENVIRONMENT_API CREATE or UPDATE on the target)"
        ),
        Map.entry("PromotionTargetsResource#GET /promotion-targets", "pre-existing, not reviewed yet"),
        Map.entry("PromotionsResource#POST /promotions/_search", "permission checked inside the method (API_DEFINITION READ)"),
        Map.entry("QualityRulesResource#GET /quality-rules", "pre-existing, not reviewed yet"),
        Map.entry("RestrictedDomainsResource#GET /restrictedDomains", "pre-existing, not reviewed yet"),
        Map.entry("RoleScopesResource#GET /rolescopes", "pre-existing, not reviewed yet"),
        Map.entry("SearchUsersResource#GET /users", "permission checked inside the method via canManageMembers (APIM-15506 / #21492)"),
        Map.entry("SocialIdentityProvidersResource#GET /identities", "pre-existing, not reviewed yet"),
        Map.entry("SocialIdentityProvidersResource#GET /social-identities", "pre-existing, not reviewed yet"),
        Map.entry("SubscriptionsResource#GET /subscriptions", "pre-existing, not reviewed yet"),
        Map.entry("TagsResource#GET /tags", "pre-existing, not reviewed yet"),
        Map.entry("TenantsResource#GET /tenants", "pre-existing, not reviewed yet"),
        Map.entry("ThemeResource#GET /{themeId}/backgroundImage", "portal / configuration read"),
        Map.entry("ThemeResource#GET /{themeId}/favicon", "portal / configuration read"),
        Map.entry("ThemeResource#GET /{themeId}/logo", "portal / configuration read"),
        Map.entry("ThemeResource#GET /{themeId}/optionalLogo", "portal / configuration read"),
        Map.entry("TokensResource#DELETE /tokens", "current-user self-service"),
        Map.entry("TokensResource#DELETE /tokens/{token}", "current-user self-service"),
        Map.entry("TokensResource#GET /tokens", "current-user self-service"),
        Map.entry("TokensResource#POST /tokens", "current-user self-service"),
        Map.entry("UserNotificationsResource#DELETE /notifications", "current-user self-service"),
        Map.entry("UserNotificationsResource#DELETE /notifications/{notification}", "current-user self-service"),
        Map.entry("UserNotificationsResource#GET /notifications", "current-user self-service"),
        Map.entry("UserResource#GET /{userId}/avatar", "pre-existing, not reviewed yet"),
        Map.entry("UserResource#POST /{userId}/changePassword", "authentication / registration"),
        Map.entry("UsersRegistrationResource#POST /registration", "authentication / registration"),
        Map.entry("UsersRegistrationResource#POST /registration/finalize", "authentication / registration")
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
        List<Class<?>> classes = findClasses("io.gravitee.rest.api.management.rest.resource");
        assertThat(liveEndpointKeys(classes))
            .as("HTTP methods missing @Permissions (add annotation or allowlist with a reason)")
            .isEqualTo(ALLOWLIST.keySet());
        assertThat(ALLOWLIST.keySet()).allSatisfy(key -> assertThat(key).matches(".+#(GET|POST|PUT|DELETE|PATCH|HEAD) /\\S*"));
        assertThat(ALLOWLIST.values()).allSatisfy(reason -> assertThat(reason).isNotBlank());
        assertThat(ALLOWLIST).containsKeys("ApisResource#GET /apis", "ApisResource#GET /apis/_paged");
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
