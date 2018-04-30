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
package io.gravitee.management.model.parameters;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public enum Key {
    COMPANY_NAME("company.name"),

    PORTAL_TITLE("portal.title"),
    PORTAL_ENTRYPOINT("portal.entrypoint"),
    PORTAL_APIKEY_HEADER("portal.apikey.header"),
    PORTAL_SUPPORT_ENABLED("portal.support.enabled"),
    PORTAL_RATING_ENABLED("portal.rating.enabled"),
    PORTAL_DEVMODE_ENABLED("portal.devMode.enabled"),
    PORTAL_USERCREATION_ENABLED("portal.userCreation.enabled"),
    PORTAL_ANALYTICS_ENABLED("portal.analytics.enabled"),
    PORTAL_ANALYTICS_TRACKINGID("portal.analytics.trackingId"),
    PORTAL_APIS_TILESMODE_ENABLED("portal.apis.tilesMode.enabled"),
    PORTAL_DASHBOARD_WIDGETS("portal.dashboard.widgets"),

    MANAGEMENT_TITLE("management.title"),

    THEME_NAME("theme.name"),
    THEME_LOGO("theme.logo"),
    THEME_LOADER("theme.loader"),
    THEME_CSS("theme.css"),

    AUTHENTICATION_FORCELOGIN_ENABLED("authentication.forceLogin.enabled"),
    AUTHENTICATION_LOCALLOGIN_ENABLED("authentication.localLogin.enabled"),
    AUTHENTICATION_GOOGLE_CLIENTID("authentication.google.clientId"),
    AUTHENTICATION_GITHUB_CLIENTID("authentication.github.clientId"),
    AUTHENTICATION_OAUTH2_CLIENTID("authentication.oauth2.clientId"),
    AUTHENTICATION_OAUTH2_NAME("authentication.oauth2.name"),
    AUTHENTICATION_OAUTH2_COLOR("authentication.oauth2.color"),
    AUTHENTICATION_OAUTH2_AUTHORIZATION_ENDPOINT("authentication.oauth2.authorization.endpoint"),
    AUTHENTICATION_OAUTH2_USER_LOGOUT_ENDPOINT("authentication.oauth2.user.logout.endpoint"),
    AUTHENTICATION_OAUTH2_SCOPE("authentication.oauth2.scopes"),

    SCHEDULER_TASKS("scheduler.tasks"),
    SCHEDULER_NOTIFICATIONS("scheduler.notifications"),

    DOCUMENTATION_URL("documentation.url")
    ;

    String key;

    Key(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
