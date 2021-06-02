/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum Key {
    COMPANY_NAME("company.name", "Gravitee.io", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    PORTAL_TOP_APIS("portal.top-apis", List.class, false, new HashSet<>(singletonList(ENVIRONMENT))),
    PORTAL_ENTRYPOINT("portal.entrypoint", "https://api.company.com", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
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

    MANAGEMENT_TITLE("management.title", "Gravitee.io Management", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    MANAGEMENT_URL("management.url", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),

    THEME_NAME("theme.name", "default", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    THEME_LOGO("theme.logo", "themes/assets/gravitee-logo.svg", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    THEME_LOADER("theme.loader", "assets/gravitee_logo_anim.gif", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    THEME_CSS("theme.css", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),

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

    DOCUMENTATION_URL("documentation.url", "https://docs.gravitee.io", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    PLAN_SECURITY_JWT_ENABLED("plan.security.jwt.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PLAN_SECURITY_OAUTH2_ENABLED("plan.security.oauth2.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PLAN_SECURITY_APIKEY_ENABLED("plan.security.apikey.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED(
        "plan.security.apikey.allowCustom.enabled",
        "false",
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PLAN_SECURITY_KEYLESS_ENABLED("plan.security.keyless.enabled", "true", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

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

    ALERT_ENABLED("alert.enabled", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),

    LOGGING_DEFAULT_MAX_DURATION("logging.default.max.duration", "0", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    LOGGING_AUDIT_ENABLED("logging.audit.enabled", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    LOGGING_AUDIT_TRAIL_ENABLED("logging.audit.trail.enabled", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    LOGGING_USER_DISPLAYED("logging.user.displayed", "false", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),

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
        "portal.http.cors.allow-origin",
        "*",
        List.class,
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_HTTP_CORS_ALLOW_HEADERS(
        "portal.http.cors.allow-headers",
        "Cache-Control;Pragma;Origin;Authorization;Content-Type;X-Requested-With;If-Match;X-Xsrf-Token;X-Recaptcha-Token",
        List.class,
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_HTTP_CORS_ALLOW_METHODS(
        "portal.http.cors.allow-methods",
        "OPTIONS;GET;POST;PUT;DELETE;PATCH",
        List.class,
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_HTTP_CORS_EXPOSED_HEADERS(
        "portal.http.cors.exposed-headers",
        "ETag;X-Xsrf-Token",
        List.class,
        new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))
    ),
    PORTAL_HTTP_CORS_MAX_AGE("portal.http.cors.max-age", "1728000", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

    EMAIL_ENABLED("email.enabled", "false", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_HOST("email.host", "smtp.my.domain", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_PORT("email.port", "587", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_USERNAME("email.username", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_PASSWORD("email.password", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_PROTOCOL("email.protocol", "smtp", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_SUBJECT("email.subject", "[Gravitee.io] %s", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_FROM("email.from", "noreply@my.domain", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_PROPERTIES_AUTH_ENABLED("email.properties.auth", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_PROPERTIES_STARTTLS_ENABLE("email.properties.starttls.enable", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),
    EMAIL_PROPERTIES_SSL_TRUST("email.properties.ssl.trust", new HashSet<>(Arrays.asList(ENVIRONMENT, ORGANIZATION, SYSTEM))),

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
    CONSOLE_HTTP_CORS_ALLOW_ORIGIN("console.http.cors.allow-origin", "*", List.class, new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_HTTP_CORS_ALLOW_HEADERS(
        "console.http.cors.allow-headers",
        "Cache-Control;Pragma;Origin;Authorization;Content-Type;X-Requested-With;If-Match;X-Xsrf-Token;X-Recaptcha-Token",
        List.class,
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_HTTP_CORS_ALLOW_METHODS(
        "console.http.cors.allow-methods",
        "OPTIONS;GET;POST;PUT;DELETE;PATCH",
        List.class,
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_HTTP_CORS_EXPOSED_HEADERS(
        "console.http.cors.exposed-headers",
        "ETag;X-Xsrf-Token",
        List.class,
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_HTTP_CORS_MAX_AGE("console.http.cors.max-age", "1728000", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_USERCREATION_ENABLED("console.userCreation.enabled", "true", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))),
    CONSOLE_USERCREATION_AUTOMATICVALIDATION_ENABLED(
        "console.userCreation.automaticValidation.enabled",
        "true",
        new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM))
    ),
    CONSOLE_SUPPORT_ENABLED("console.support.enabled", "true", new HashSet<>(Arrays.asList(ORGANIZATION, SYSTEM)));

    String key;
    String defaultValue;
    Class<?> type;
    boolean isOverridable = true;
    Set<KeyScope> scopes;

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

    public Set<KeyScope> scopes() {
        return scopes;
    }

    public static Key findByKey(String value) {
        for (Key key : Key.values()) {
            if (key.key.equals(value)) {
                return key;
            }
        }
        throw new IllegalArgumentException(value + " is not a valid Key");
    }
}
