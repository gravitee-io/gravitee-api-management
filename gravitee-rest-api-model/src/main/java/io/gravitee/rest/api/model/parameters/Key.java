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

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum Key {
    COMPANY_NAME("company.name", "Gravitee.io"),

    PORTAL_TOP_APIS("portal.top-apis"),
    PORTAL_TITLE("portal.title", "Gravitee.io Portal"),
    PORTAL_ENTRYPOINT("portal.entrypoint", "https://api.company.com"),
    PORTAL_APIKEY_HEADER("portal.apikey.header", "X-Gravitee-Api-Key"),
    PORTAL_SUPPORT_ENABLED("portal.support.enabled", "true"),
    PORTAL_RATING_ENABLED("portal.rating.enabled", "true"),
    PORTAL_RATING_COMMENT_MANDATORY("portal.rating.comment.mandatory", "false"),
    PORTAL_USERCREATION_ENABLED("portal.userCreation.enabled", "true"),
    PORTAL_ANALYTICS_ENABLED("portal.analytics.enabled", "false"),
    PORTAL_ANALYTICS_TRACKINGID("portal.analytics.trackingId"),
    PORTAL_APIS_TILESMODE_ENABLED("portal.apis.tilesMode.enabled", "true"),
    PORTAL_APIS_VIEW_ENABLED("portal.apis.viewMode.enabled", "true"),
    PORTAL_APIS_SHOW_TAGS_IN_APIHEADER("portal.apis.apiheader.showtags.enabled", "false"),
    PORTAL_APIS_SHOW_VIEWS_IN_APIHEADER("portal.apis.apiheader.showviews.enabled", "false"),
    PORTAL_UPLOAD_MEDIA_ENABLED("portal.uploadMedia.enabled", "false"),
    PORTAL_UPLOAD_MEDIA_MAXSIZE("portal.uploadMedia.maxSizeInOctet", "1000000"),
    PORTAL_URL("portal.url", ""),

    MANAGEMENT_TITLE("management.title", "Gravitee.io Management"),
    MANAGEMENT_URL("management.url", ""),

    THEME_NAME("theme.name", "default"),
    THEME_LOGO("theme.logo", "themes/assets/GRAVITEE_LOGO1-01.png"),
    THEME_LOADER("theme.loader", "assets/gravitee_logo_anim.gif"),
    THEME_CSS("theme.css"),

    AUTHENTICATION_FORCELOGIN_ENABLED("authentication.forceLogin.enabled", "false"),
    AUTHENTICATION_LOCALLOGIN_ENABLED("authentication.localLogin.enabled", "true"),
    AUTHENTICATION_GOOGLE_CLIENTID("authentication.google.clientId"),
    AUTHENTICATION_GITHUB_CLIENTID("authentication.github.clientId"),
    AUTHENTICATION_OAUTH2_CLIENTID("authentication.oauth2.clientId"),
    AUTHENTICATION_OAUTH2_NAME("authentication.oauth2.name"),
    AUTHENTICATION_OAUTH2_COLOR("authentication.oauth2.color", "#0076b4"),
    AUTHENTICATION_OAUTH2_AUTHORIZATION_ENDPOINT("authentication.oauth2.authorization.endpoint"),
    AUTHENTICATION_OAUTH2_USER_LOGOUT_ENDPOINT("authentication.oauth2.user.logout.endpoint"),
    AUTHENTICATION_OAUTH2_SCOPE("authentication.oauth2.scopes"),

    SCHEDULER_TASKS("scheduler.tasks", "10"),
    SCHEDULER_NOTIFICATIONS("scheduler.notifications", "10"),

    DOCUMENTATION_URL("documentation.url", "https://docs.gravitee.io"),

    PLAN_SECURITY_JWT_ENABLED("plan.security.jwt.enabled", "true"),
    PLAN_SECURITY_OAUTH2_ENABLED("plan.security.oauth2.enabled", "true"),
    PLAN_SECURITY_APIKEY_ENABLED("plan.security.apikey.enabled", "true"),
    PLAN_SECURITY_KEYLESS_ENABLED("plan.security.keyless.enabled", "true"),

    API_QUALITY_METRICS_ENABLED("api.quality.metrics.enabled", "false"),
    API_QUALITY_METRICS_FUNCTIONAL_DOCUMENTATION_WEIGHT("api.quality.metrics.functional.documentation.weight", "0"),
    API_QUALITY_METRICS_TECHNICAL_DOCUMENTATION_WEIGHT("api.quality.metrics.technical.documentation.weight", "0"),
    API_QUALITY_METRICS_HEALTHCHECK_WEIGHT("api.quality.metrics.healthcheck.weight", "0"),
    API_QUALITY_METRICS_DESCRIPTION_WEIGHT("api.quality.metrics.description.weight", "0"),
    API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH("api.quality.metrics.description.min.length", "100"),
    API_QUALITY_METRICS_LOGO_WEIGHT("api.quality.metrics.logo.weight", "0"),
    API_QUALITY_METRICS_VIEWS_WEIGHT("api.quality.metrics.views.weight", "0"),
    API_QUALITY_METRICS_LABELS_WEIGHT("api.quality.metrics.labels.weight", "0"),

    ALERT_ENABLED("alert.enabled", "false"),

    LOGGING_DEFAULT_MAX_DURATION("logging.default.max.duration", "0"),
    LOGGING_AUDIT_ENABLED("logging.audit.enabled", "false"),
    LOGGING_AUDIT_TRAIL_ENABLED("logging.audit.trail.enabled", "false"),
    LOGGING_USER_DISPLAYED("logging.user.displayed", "false"),

    ANALYTICS_CLIENT_TIMEOUT("analytics.client.timeout", "30000"),

    APPLICATION_TYPE_SIMPLE_ENABLED("application.types.simple.enabled", "true"),
    APPLICATION_TYPE_BROWSER_ENABLED("application.types.browser.enabled", "true"),
    APPLICATION_TYPE_WEB_ENABLED("application.types.web.enabled", "true"),
    APPLICATION_TYPE_NATIVE_ENABLED("application.types.native.enabled", "true"),
    APPLICATION_TYPE_BACKEND_TO_BACKEND_ENABLED("application.types.backend_to_backend.enabled", "true"),
    APPLICATION_REGISTRATION_ENABLED("application.registration.enabled", "false"),

    API_REVIEW_ENABLED("api.review.enabled", "false"),
    NEWSLETTER_ENABLED("newsletter.enabled", "true");

    String key;
    String defaultValue;

    Key(String key) {
        this.key = key;
    }

    Key(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public String defaultValue() {
        return defaultValue;
    }
}
