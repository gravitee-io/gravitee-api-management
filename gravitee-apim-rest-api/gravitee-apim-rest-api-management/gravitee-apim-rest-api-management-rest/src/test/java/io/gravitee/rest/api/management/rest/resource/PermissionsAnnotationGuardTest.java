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
        "AlertsResource#getAlertMetrics",
        "ApiMediaResource#getApiMediaImage",
        "ApiMembersResource#getApiMembersPermissions",
        "ApiNotificationSettingsResource#createApiNotificationSettings",
        "ApiPageResource#getApiPage",
        "ApiPageResource#getApiPageContent",
        "ApiPagesResource#getApiPages",
        "ApiPlansResource#getApiPlan",
        "ApiPlansResource#getApiPlans",
        "ApiRatingResource#getApiRating",
        "ApiRatingResource#getApiRatingByApiAndUser",
        "ApiRatingResource#getApiRatingSummaryByApi",
        "ApiResource#getApi",
        "ApiResource#getApiBackground",
        "ApiResource#getApiPicture",
        "ApiResource#getApiQualityMetrics",
        "ApiResource#getPortalApiHeaders",
        "ApiResource#isApiSynchronized",
        "ApiSubscribersResource#getApiSubscribers",
        "ApiSubscribersResource#getApiSubscribersPaged",
        "ApisResource#getApiHooks",
        "ApisResource#getApis",
        "ApisResource#searchApis",
        "ApisResource#searchPagedApis",
        "ApplicationMembersResource#getApplicationMemberPermissions",
        "ApplicationNotificationSettingsResource#createApplicationNotificationSettings",
        "ApplicationResource#restoreApplication",
        "ApplicationsResource#getApplicationHooks",
        "CategoriesResource#getCategories",
        "CategoryResource#getCategory",
        "CategoryResource#getCategoryBackground",
        "CategoryResource#getCategoryPicture",
        "CockpitAuthenticationResource#tokenExchange",
        "ConsoleResource#getConsoleConfig",
        "CurrentUserResource#deleteCurrentUser",
        "CurrentUserResource#getCurrentUser",
        "CurrentUserResource#getCurrentUserPicture",
        "CurrentUserResource#getUserShardingTags",
        "CurrentUserResource#getUserTasks",
        "CurrentUserResource#login",
        "CurrentUserResource#logoutCurrentUser",
        "CurrentUserResource#updateCurrentUser",
        "CustomUserFieldsResource#getCustomUserFields",
        "DashboardsResource#getDashboards",
        "EnvironmentAnalyticsResource#getPlatformAnalytics",
        "EnvironmentConfigurationResource#getConfigurationHooks",
        "EnvironmentConfigurationResource#getEnabledApplicationTypes",
        "EnvironmentConfigurationResource#getGrammar",
        "EnvironmentResource#getEnvironment",
        "EnvironmentResource#getEnvironmentPermissions",
        "EnvironmentsResource#getEnvironments",
        "ExternalAuthenticationResource#tokenExchange",
        "FetcherResource#getFetcher",
        "FetcherResource#getFetcherSchema",
        "FetchersResource#getFetchers",
        "FlowsResource#getConfigurationSchemaForm",
        "FlowsResource#getPlatformFlowSchemaForm",
        "FlowsResource#hasPolicies",
        "NewsletterResource#getTaglines",
        "NewsletterResource#subscribeNewsletterToCurrentUser",
        "NotificationConfigsResource#createGenericNotificationSetting",
        "OAuth2AuthenticationResource#exchangeAuthorizationCode",
        "OAuth2AuthenticationResource#tokenExchange",
        "OpenAPIResource#getOpenApi",
        "PasswordPolicyResource#getPasswordPolicy",
        "PlansResource#getPlansConfiguration",
        "PlatformTicketsResource#createPlatformTicket",
        "PlatformTicketsResource#getTicket",
        "PlatformTicketsResource#getTickets",
        "PortalApisResource#searchPortalApis",
        "PortalEntrypointsResource#getPortalEntrypoints",
        "PortalMediaResource#getPortalMedia",
        "PortalRedirectResource#redirectToPortal",
        "PortalResource#getPortalConfig",
        "PromotionResource#processPromotion",
        "PromotionTargetsResource#getPromotionTargets",
        "PromotionsResource#searchPromotions",
        "QualityRulesResource#getQualityRules",
        "RestrictedDomainsResource#getRestrictedDomains",
        "RoleScopesResource#getRoleScopes",
        "SocialIdentityProvidersResource#getSocialIdentityProviders",
        "SubscriptionsResource#getUserSubscriptions",
        "TagsResource#getTags",
        "TenantsResource#getTenants",
        "ThemeResource#getFavicon",
        "ThemeResource#getLogoLight",
        "ThemeResource#getThemeBackground",
        "ThemeResource#getThemeLogo",
        "TokensResource#createTokens",
        "TokensResource#getTokens",
        "TokensResource#revokeAllTokens",
        "TokensResource#revokeToken",
        "UserNotificationsResource#deleteAllUserNotifications",
        "UserNotificationsResource#deleteUserNotification",
        "UserNotificationsResource#getUserNotifications",
        "UserResource#finalizeResetPassword",
        "UserResource#getUserAvatar",
        "UsersRegistrationResource#finalizeUserRegistration",
        "UsersRegistrationResource#registerUser"
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

        for (Class<?> resourceClass : findClasses("io.gravitee.rest.api.management.rest.resource")) {
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
