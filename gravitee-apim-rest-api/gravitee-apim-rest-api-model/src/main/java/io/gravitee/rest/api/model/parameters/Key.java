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
package io.gravitee.rest.api.model.parameters;

import static io.gravitee.rest.api.model.parameters.KeyScope.*;
import static java.util.Collections.singletonList;

import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Schema(enumAsRef = true)
public enum Key {
    COMPANY_NAME("company.name", "Gravitee.io", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    PORTAL_TOP_APIS("portal.top-apis", List.class, false, new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_ENTRYPOINT("portal.entrypoint", "https://api.company.com", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_TCP_PORT("portal.tcpPort", "4082", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_APIKEY_HEADER("portal.apikey.header", "X-Gravitee-Api-Key", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_SUPPORT_ENABLED("portal.support.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_RATING_ENABLED("portal.rating.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_RATING_COMMENT_MANDATORY(
        "portal.rating.comment.mandatory",
        "false",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_USERCREATION_ENABLED("portal.userCreation.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_USERCREATION_AUTOMATICVALIDATION_ENABLED(
        "portal.userCreation.automaticValidation.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_ANALYTICS_ENABLED("portal.analytics.enabled", "false", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_ANALYTICS_TRACKINGID("portal.analytics.trackingId", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_APIS_TILESMODE_ENABLED("portal.apis.tilesMode.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_APIS_CATEGORY_ENABLED(
        "portal.apis.categoryMode.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_APIS_PROMOTED_API_ENABLED("portal.apis.promotedApiMode.enabled", "true", Set.of(ENVIRONMENT)),
    PORTAL_APIS_SHOW_TAGS_IN_APIHEADER(
        "portal.apis.apiheader.showtags.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_APIS_SHOW_CATEGORIES_IN_APIHEADER(
        "portal.apis.apiheader.showcategories.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_UPLOAD_MEDIA_ENABLED("portal.uploadMedia.enabled", "false", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_UPLOAD_MEDIA_MAXSIZE(
        "portal.uploadMedia.maxSizeInOctet",
        "1000000",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_URL("portal.url", new HashSet<>(Arrays.asList(ENVIRONMENT, SYSTEM))),
    PORTAL_HOMEPAGE_TITLE("portal.homepageTitle", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_SITE_TITLE("portal.next.siteTitle", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_HOMEPAGE_BANNER_TITLE("portal.next.bannerTitle", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_HOMEPAGE_BANNER_SUBTITLE("portal.next.bannerSubtitle", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_ACCESS_ENABLED("portal.next.access.enabled", "false", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_THEME_COLOR_PRIMARY("portal.next.theme.color.primary", "#613CB0", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_THEME_COLOR_SECONDARY("portal.next.theme.color.secondary", "#958BA9", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_THEME_COLOR_TERTIARY("portal.next.theme.color.tertiary", "#B7818F", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_THEME_COLOR_ERROR("portal.next.theme.color.error", "#EC6152", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_THEME_COLOR_BACKGROUND_PAGE(
        "portal.next.theme.color.background.page",
        "#F7F8FD",
        new HashSet<>(singletonList(ENVIRONMENT))
    ),
    PORTAL_NEXT_THEME_COLOR_BACKGROUND_CARD(
        "portal.next.theme.color.background.card",
        "#FFFFFF",
        new HashSet<>(singletonList(ENVIRONMENT))
    ),
    PORTAL_NEXT_THEME_CUSTOM_CSS("portal.next.theme.customCss", new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_NEXT_THEME_FONT_FAMILY("portal.next.theme.font.family", "Roboto", new HashSet<>(singletonList(ENVIRONMENT))),

    MANAGEMENT_TITLE("management.title", "Gravitee.io Management", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    MANAGEMENT_URL("management.url", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),

    PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED(
        "portal.authentication.forceLogin.enabled",
        "false",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_AUTHENTICATION_LOCALLOGIN_ENABLED(
        "portal.authentication.localLogin.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),

    PORTAL_SCHEDULER_TASKS("portal.scheduler.tasks", "10", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_SCHEDULER_NOTIFICATIONS("portal.scheduler.notifications", "10", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    DOCUMENTATION_URL(
        "documentation.url",
        "https://documentation.gravitee.io/apim",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),

    PLAN_SECURITY_JWT_ENABLED("plan.security.jwt.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PLAN_SECURITY_OAUTH2_ENABLED("plan.security.oauth2.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PLAN_SECURITY_APIKEY_ENABLED("plan.security.apikey.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED(
        "plan.security.apikey.allowCustom.enabled",
        "false",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PLAN_SECURITY_APIKEY_SHARED_ALLOWED("plan.security.apikey.allowShared.enabled", "false", Set.of(ENVIRONMENT, SYSTEM)),
    PLAN_SECURITY_KEYLESS_ENABLED("plan.security.keyless.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PLAN_SECURITY_SUBSCRIPTION_ENABLED(
        "plan.security.subscription.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PLAN_SECURITY_PUSH_ENABLED("plan.security.push.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    OPEN_API_DOC_TYPE_SWAGGER_ENABLED(
        "open.api.doc.type.swagger.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    OPEN_API_DOC_TYPE_REDOC_ENABLED(
        "open.api.doc.type.redoc.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    OPEN_API_DOC_TYPE_DEFAULT("open.api.doc.type.default", "Swagger", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    API_QUALITY_METRICS_ENABLED("api.quality.metrics.enabled", "false", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    API_QUALITY_METRICS_FUNCTIONAL_DOCUMENTATION_WEIGHT(
        "api.quality.metrics.functional.documentation.weight",
        "0",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    API_QUALITY_METRICS_TECHNICAL_DOCUMENTATION_WEIGHT(
        "api.quality.metrics.technical.documentation.weight",
        "0",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    API_QUALITY_METRICS_HEALTHCHECK_WEIGHT(
        "api.quality.metrics.healthcheck.weight",
        "0",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    API_QUALITY_METRICS_DESCRIPTION_WEIGHT(
        "api.quality.metrics.description.weight",
        "0",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH(
        "api.quality.metrics.description.min.length",
        "100",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    API_QUALITY_METRICS_LOGO_WEIGHT(
        "api.quality.metrics.logo.weight",
        "0",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    API_QUALITY_METRICS_CATEGORIES_WEIGHT(
        "api.quality.metrics.categories.weight",
        "0",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    API_QUALITY_METRICS_LABELS_WEIGHT(
        "api.quality.metrics.labels.weight",
        "0",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),

    ALERT_ENABLED("alert.enabled", "true", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),

    LOGGING_DEFAULT_MAX_DURATION("logging.default.max.duration", "0", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    LOGGING_AUDIT_ENABLED("logging.audit.enabled", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    LOGGING_AUDIT_TRAIL_ENABLED("logging.audit.trail.enabled", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    LOGGING_USER_DISPLAYED("logging.user.displayed", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    LOGGING_MESSAGE_SAMPLING_COUNT_DEFAULT("logging.messageSampling.count.default", "100", Set.of(ENVIRONMENT, ORGANIZATION, SYSTEM)),
    LOGGING_MESSAGE_SAMPLING_COUNT_LIMIT("logging.messageSampling.count.limit", "10", Set.of(ENVIRONMENT, ORGANIZATION, SYSTEM)),
    LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_DEFAULT(
        "logging.messageSampling.probabilistic.default",
        "0.01",
        Set.of(ENVIRONMENT, ORGANIZATION, SYSTEM)
    ),
    LOGGING_MESSAGE_SAMPLING_PROBABILISTIC_LIMIT(
        "logging.messageSampling.probabilistic.limit",
        "0.5",
        Set.of(ENVIRONMENT, ORGANIZATION, SYSTEM)
    ),
    LOGGING_MESSAGE_SAMPLING_TEMPORAL_DEFAULT(
        "logging.messageSampling.temporal.default",
        "PT1S",
        Set.of(ENVIRONMENT, ORGANIZATION, SYSTEM)
    ),
    LOGGING_MESSAGE_SAMPLING_TEMPORAL_LIMIT("logging.messageSampling.temporal.limit", "PT1S", Set.of(ENVIRONMENT, ORGANIZATION, SYSTEM)),

    ANALYTICS_CLIENT_TIMEOUT("analytics.client.timeout", "30000", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    APPLICATION_TYPE_SIMPLE_ENABLED(
        "application.types.simple.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    APPLICATION_TYPE_BROWSER_ENABLED(
        "application.types.browser.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    APPLICATION_TYPE_WEB_ENABLED("application.types.web.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    APPLICATION_TYPE_NATIVE_ENABLED(
        "application.types.native.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    APPLICATION_TYPE_BACKEND_TO_BACKEND_ENABLED(
        "application.types.backend_to_backend.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    APPLICATION_REGISTRATION_ENABLED(
        "application.registration.enabled",
        "false",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),

    API_REVIEW_ENABLED("api.review.enabled", "false", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    MAINTENANCE_MODE_ENABLED("maintenance.enabled", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    NEWSLETTER_ENABLED("newsletter.enabled", "true", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),

    PORTAL_RECAPTCHA_ENABLED("portal.reCaptcha.enabled", "false", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PORTAL_RECAPTCHA_SITE_KEY("portal.reCaptcha.siteKey", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    PORTAL_HTTP_CORS_ALLOW_ORIGIN(
        "http.api.portal.cors.allow-origin",
        "*",
        List.class,
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_HTTP_CORS_ALLOW_HEADERS(
        "http.api.portal.cors.allow-headers",
        "Cache-Control;Pragma;Origin;Authorization;Content-Type;X-Requested-With;If-Match;X-Xsrf-Token;X-Recaptcha-Token",
        List.class,
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_HTTP_CORS_ALLOW_METHODS(
        "http.api.portal.cors.allow-methods",
        "OPTIONS;GET;POST;PUT;DELETE;PATCH",
        List.class,
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_HTTP_CORS_EXPOSED_HEADERS(
        "http.api.portal.cors.exposed-headers",
        "ETag;X-Xsrf-Token",
        List.class,
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_HTTP_CORS_MAX_AGE("http.api.portal.cors.max-age", "1728000", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    EMAIL_ENABLED("email.enabled", "false", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_HOST("email.host", "smtp.my.domain", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_PORT("email.port", "587", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_USERNAME("email.username", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_PASSWORD("email.password", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_PROTOCOL("email.protocol", "smtp", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_SUBJECT("email.subject", "[Gravitee.io] %s", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_FROM("email.from", "noreply@my.domain", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_PROPERTIES_AUTH_ENABLED("email.properties.auth", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),
    EMAIL_PROPERTIES_STARTTLS_ENABLE(
        "email.properties.starttls.enable",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)),
        true
    ),
    EMAIL_PROPERTIES_SSL_TRUST("email.properties.ssl.trust", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM)), true),

    API_LABELS_DICTIONARY("api.labelsDictionary", List.class, new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    API_PRIMARY_OWNER_MODE(
        "api.primary.owner.mode",
        ApiPrimaryOwnerMode.HYBRID.name(),
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),

    CONSOLE_AUTHENTICATION_LOCALLOGIN_ENABLED(
        "console.authentication.localLogin.enabled",
        "true",
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_SCHEDULER_TASKS("console.scheduler.tasks", "10", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_SCHEDULER_NOTIFICATIONS("console.scheduler.notifications", "10", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_RECAPTCHA_ENABLED("console.reCaptcha.enabled", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_RECAPTCHA_SITE_KEY("console.reCaptcha.siteKey", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_HTTP_CORS_ALLOW_ORIGIN(
        "http.api.management.cors.allow-origin",
        "*",
        List.class,
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_HTTP_CORS_ALLOW_HEADERS(
        "http.api.management.cors.allow-headers",
        "Cache-Control;Pragma;Origin;Authorization;Content-Type;X-Requested-With;If-Match;X-Xsrf-Token;X-Recaptcha-Token",
        List.class,
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_HTTP_CORS_ALLOW_METHODS(
        "http.api.management.cors.allow-methods",
        "OPTIONS;GET;POST;PUT;DELETE;PATCH",
        List.class,
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_HTTP_CORS_EXPOSED_HEADERS(
        "http.api.management.cors.exposed-headers",
        "ETag;X-Xsrf-Token",
        List.class,
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_HTTP_CORS_MAX_AGE("http.api.management.cors.max-age", "1728000", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_USERCREATION_ENABLED("console.userCreation.enabled", "true", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_USERCREATION_AUTOMATICVALIDATION_ENABLED(
        "console.userCreation.automaticValidation.enabled",
        "true",
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_SUPPORT_ENABLED("console.support.enabled", "true", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_DASHBOARDS_API_STATUS(
        "console.dashboards.apiStatus.enabled",
        "true",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    CONSOLE_SYSTEM_ROLE_EDITION_ENABLED("console.systemRoleEdition.enabled", "false", Set.of(SYSTEM)),

    CONSOLE_ANALYTICS_PENDO_ENABLED("console.analytics.pendo.enabled", "false", Set.of(SYSTEM)),
    CONSOLE_ANALYTICS_PENDO_API_KEY("console.analytics.pendo.apiKey", "", Set.of(SYSTEM)),
    CONSOLE_ANALYTICS_PENDO_ACCOUNT_ID("console.analytics.pendo.account.id", (String) null, Set.of(SYSTEM)),
    CONSOLE_ANALYTICS_PENDO_ACCOUNT_HRID("console.analytics.pendo.account.hrid", (String) null, Set.of(SYSTEM)),
    CONSOLE_ANALYTICS_PENDO_ACCOUNT_TYPE("console.analytics.pendo.account.type", (String) null, Set.of(SYSTEM)),

    CONSOLE_CUSTOMIZATION_TITLE("console.customization.title", (String) null, Set.of(SYSTEM)),
    CONSOLE_CUSTOMIZATION_FAVICON("console.customization.favicon", (String) null, Set.of(SYSTEM)),
    CONSOLE_CUSTOMIZATION_LOGO("console.customization.logo", (String) null, Set.of(SYSTEM)),
    CONSOLE_CUSTOMIZATION_THEME_MENUACTIVE("console.customization.theme.menuActive", (String) null, Set.of(SYSTEM)),
    CONSOLE_CUSTOMIZATION_THEME_MENUBACKGROUND("console.customization.theme.menuBackground", (String) null, Set.of(SYSTEM)),
    CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_CUSTOMEENTERPRISENAME(
        "console.customization.ctaConfiguration.customEnterpriseName",
        (String) null,
        Set.of(SYSTEM)
    ),
    CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TITLE("console.customization.ctaConfiguration.title", (String) null, Set.of(SYSTEM)),
    CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_HIDEDAYS("console.customization.ctaConfiguration.hideDays", "true", Set.of(SYSTEM)),
    CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALBUTTONLABEL(
        "console.customization.ctaConfiguration.trialButtonLabel",
        (String) null,
        Set.of(SYSTEM)
    ),
    CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALURL("console.customization.ctaConfiguration.trialURL", (String) null, Set.of(SYSTEM)),
    CONSOLE_LICENSE_EXPIRATION_NOTIFICATION_ENABLED("console.licenseExpirationNotification.enabled", "true", Set.of(SYSTEM)),

    V4_EMULATION_ENGINE_DEFAULT("api.v2.emulateV4Engine.default", "yes", Set.of(SYSTEM)),

    ALERT_ENGINE_ENABLED("alerts.alert-engine.enabled", "false", Set.of(SYSTEM)),

    INSTALLATION_TYPE("installation.type", "standalone", Set.of(SYSTEM)),
    TRIAL_INSTANCE("trialInstance.enabled", "false", Set.of(SYSTEM)),

    EXTERNAL_AUTH_ENABLED("auth.external.enabled", "false", Set.of(SYSTEM)),
    EXTERNAL_AUTH_ACCOUNT_DELETION_ENABLED("auth.external.allowAccountDeletion", "true", Set.of(SYSTEM));

    String key;
    String defaultValue;
    Class<?> type;
    boolean isOverridable = true;
    Set<KeyScope> scopes;
    boolean isHiddenForTrial = false;

    Key(String key, Set<KeyScope> scopes) {
        this.key = key;
        this.scopes = scopes;
    }

    Key(String key, String defaultValue, Class<?> type, Set<KeyScope> scopes) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        this.scopes = scopes;
    }

    Key(String key, Class<?> type, Set<KeyScope> scopes) {
        this.key = key;
        this.type = type;
        this.scopes = scopes;
    }

    Key(String key, Class<?> type, boolean isOverridable, Set<KeyScope> scopes) {
        this.key = key;
        this.type = type;
        this.isOverridable = isOverridable;
        this.scopes = scopes;
    }

    Key(String key, String defaultValue, Set<KeyScope> scopes) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.scopes = scopes;
    }

    Key(String key, String defaultValue, Set<KeyScope> scopes, boolean isHiddenForTrial) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.scopes = scopes;
        this.isHiddenForTrial = isHiddenForTrial;
    }

    Key(String key, Set<KeyScope> scopes, boolean isHiddenForTrial) {
        this.key = key;
        this.scopes = scopes;
        this.isHiddenForTrial = isHiddenForTrial;
    }

    public static Key findByKey(String value) {
        for (Key key : Key.values()) {
            if (key.key.equals(value)) {
                return key;
            }
        }
        throw new IllegalArgumentException(value + " is not a valid Key");
    }

    public String key() {
        return key;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public Class<?> type() {
        return type;
    }

    public boolean isOverridable() {
        return isOverridable;
    }

    public boolean isHiddenForTrial() {
        return isHiddenForTrial;
    }

    public Set<KeyScope> scopes() {
        return scopes;
    }
}
