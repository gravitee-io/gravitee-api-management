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
package io.gravitee.management.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.management.model.annotations.ParameterKey;
import io.gravitee.management.model.parameters.Key;

import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(value={ "baseURL" })
public class PortalConfigEntity {

    private Company company;
    private Management management;
    private Portal portal;
    private Authentication authentication;
    private Scheduler scheduler;
    private Documentation documentation;
    private Theme theme;
    private Plan plan;

    public PortalConfigEntity() {
        company = new Company();
        management = new Management();
        portal = new Portal();
        authentication = new Authentication();
        scheduler = new Scheduler();
        documentation = new Documentation();
        theme = new Theme();
        plan = new Plan();
    }

    public Company getCompany() {
        return company;
    }
    public void setCompany(Company company) {
        this.company = company;
    }

    public Portal getPortal() {
        return portal;
    }
    public void setPortal(Portal portal) {
        this.portal = portal;
    }

    public Management getManagement() {
        return management;
    }
    public void setManagement(Management management) {
        this.management = management;
    }

    public Authentication getAuthentication() {
        return authentication;
    }
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Documentation getDocumentation() {
        return documentation;
    }
    public void setDocumentation(Documentation documentation) {
        this.documentation = documentation;
    }

    public Theme getTheme() {
        return theme;
    }
    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public class Company {

        @ParameterKey(Key.COMPANY_NAME)
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public class Portal {
        @ParameterKey(Key.PORTAL_TITLE)
        private String title;
        @ParameterKey(Key.PORTAL_ENTRYPOINT)
        private String entrypoint;
        @ParameterKey(Key.PORTAL_APIKEY_HEADER)
        private String apikeyHeader;
        @ParameterKey(Key.PORTAL_SUPPORT_ENABLED)
        private Enabled support;
        @ParameterKey(Key.PORTAL_DEVMODE_ENABLED)
        private Enabled devMode;
        @ParameterKey(Key.PORTAL_USERCREATION_ENABLED)
        private Enabled userCreation;

        private PortalApis apis;
        private PortalAnalytics analytics;
        private PortalDashboard dashboard;
        private PortalRating rating;

        public Portal() {
            apis = new PortalApis();
            analytics = new PortalAnalytics();
            dashboard = new PortalDashboard();
            rating = new PortalRating();
        }

        public Enabled isDevMode() {
            return devMode;
        }

        public void setDevMode(Enabled devMode) {
            this.devMode = devMode;
        }

        public Enabled isUserCreation() {
            return userCreation;
        }

        public void setUserCreation(Enabled userCreation) {
            this.userCreation = userCreation;
        }

        public String getEntrypoint() {
            return entrypoint;
        }

        public void setEntrypoint(String entrypoint) {
            this.entrypoint = entrypoint;
        }

        public String getApikeyHeader() {
            return apikeyHeader;
        }

        public void setApikeyHeader(String apikeyHeader) {
            this.apikeyHeader = apikeyHeader;
        }

        public PortalApis getApis() {
            return apis;
        }

        public void setApis(PortalApis apis) {
            this.apis = apis;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Enabled getSupport() {
            return support;
        }

        public void setSupport(Enabled support)  {
            this.support = support;
        }

        public Enabled getDevMode() {
            return devMode;
        }

        public Enabled getUserCreation() {
            return userCreation;
        }

        public PortalAnalytics getAnalytics() {
            return analytics;
        }

        public void setAnalytics(PortalAnalytics analytics) {
            this.analytics = analytics;
        }

        public PortalDashboard getDashboard() {
            return dashboard;
        }

        public void setDashboard(PortalDashboard dashboard) {
            this.dashboard = dashboard;
        }

        public PortalRating getRating() {
            return rating;
        }

        public void setRating(PortalRating rating) {
            this.rating = rating;
        }

        public class PortalRating {
            @ParameterKey(Key.PORTAL_RATING_ENABLED)
            private Boolean enabled;

            private RatingComment comment;

            public PortalRating() {
                comment = new RatingComment();
            }

            public Boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public RatingComment getComment() {
                return comment;
            }

            public void setComment(RatingComment comment) {
                this.comment = comment;
            }

            public class RatingComment {
                @ParameterKey(Key.PORTAL_RATING_COMMENT_MANDATORY)
                private Boolean mandatory;

                public Boolean isMandatory() {
                    return mandatory;
                }

                public void setMandatory(Boolean mandatory) {
                    this.mandatory = mandatory;
                }
            }
        }
    }

    public class Management {
        @ParameterKey(Key.MANAGEMENT_TITLE)
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public class Theme {
        @ParameterKey(Key.THEME_NAME)
        private String name;
        @ParameterKey(Key.THEME_LOGO)
        private String logo;
        @ParameterKey(Key.THEME_LOADER)
        private String loader;
        @ParameterKey(Key.THEME_CSS)
        private String css;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLogo() {
            return logo;
        }

        public void setLogo(String logo) {
            this.logo = logo;
        }

        public String getLoader() {
            return loader;
        }

        public void setLoader(String loader) {
            this.loader = loader;
        }

        public String getCss() {
            return css;
        }

        public void setCss(String css) {
            this.css = css;
        }
    }

    public class Authentication {
        @ParameterKey(Key.AUTHENTICATION_FORCELOGIN_ENABLED)
        private Enabled forceLogin;
        @ParameterKey(Key.AUTHENTICATION_LOCALLOGIN_ENABLED)
        private Enabled localLogin;
        private GoogleAuthentication google;
        private GithubAuthentication github;
        private OAuth2Authentication oauth2;

        public Authentication() {
            google = new GoogleAuthentication();
            github = new GithubAuthentication();
            oauth2 = new OAuth2Authentication();
        }

        public Enabled getForceLogin() {
            return forceLogin;
        }

        public void setForceLogin(Enabled forceLogin) {
            this.forceLogin = forceLogin;
        }

        public Enabled getLocalLogin() {
            return localLogin;
        }

        public void setLocalLogin(Enabled localLogin) {
            this.localLogin = localLogin;
        }

        public GoogleAuthentication getGoogle() {
            return google;
        }

        public void setGoogle(GoogleAuthentication google) {
            this.google = google;
        }

        public GithubAuthentication getGithub() {
            return github;
        }

        public void setGithub(GithubAuthentication github) {
            this.github = github;
        }

        public OAuth2Authentication getOauth2() {
            return oauth2;
        }

        public void setOauth2(OAuth2Authentication oauth2) {
            this.oauth2 = oauth2;
        }
    }

    public static class GoogleAuthentication {
//        @ParameterKey(Key.AUTHENTICATION_GOOGLE_CLIENTID)
        private String clientId;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }

    public static class GithubAuthentication {
//        @ParameterKey(Key.AUTHENTICATION_GITHUB_CLIENTID)
        private String clientId;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAuth2Authentication {
//        @ParameterKey(Key.AUTHENTICATION_OAUTH2_CLIENTID)
        private String clientId;
        @ParameterKey(Key.AUTHENTICATION_OAUTH2_NAME)
        private String name;
        @ParameterKey(Key.AUTHENTICATION_OAUTH2_COLOR)
        private String color;
        @ParameterKey(Key.AUTHENTICATION_OAUTH2_AUTHORIZATION_ENDPOINT)
        private String authorizationEndpoint;
        @ParameterKey(Key.AUTHENTICATION_OAUTH2_USER_LOGOUT_ENDPOINT)
        private String userLogoutEndpoint;
        @ParameterKey(Key.AUTHENTICATION_OAUTH2_SCOPE)
        private List<String> scope;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getAuthorizationEndpoint() {
            return authorizationEndpoint;
        }

        public void setAuthorizationEndpoint(String authorizationEndpoint) {
            this.authorizationEndpoint = authorizationEndpoint;
        }

        public String getUserLogoutEndpoint() {
            return userLogoutEndpoint;
        }

        public void setUserLogoutEndpoint(String userLogoutEndpoint) {
            this.userLogoutEndpoint = userLogoutEndpoint;
        }

        public List<String> getScope() {
            return scope;
        }

        public void setScope(List<String> scope) {
            this.scope = scope;
        }
    }

    public static class Enabled {
        private boolean enabled;

        Enabled() {}
        public Enabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public static class PortalAnalytics {

        @ParameterKey(Key.PORTAL_ANALYTICS_ENABLED)
        private Boolean enabled;
        @ParameterKey(Key.PORTAL_ANALYTICS_TRACKINGID)
        private String trackingId;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
        }

        public String getTrackingId() {
            return trackingId;
        }

    }

    public class Scheduler {
        @JsonProperty("tasks")
        @ParameterKey(Key.SCHEDULER_TASKS)
        private Integer tasksInSeconds;

        @JsonProperty("notifications")
        @ParameterKey(Key.SCHEDULER_NOTIFICATIONS)
        private Integer notificationsInSeconds;

        public Integer getTasksInSeconds() {
            return tasksInSeconds;
        }

        public void setTasksInSeconds(Integer tasksInSeconds) {
            this.tasksInSeconds = tasksInSeconds;
        }

        public Integer getNotificationsInSeconds() {
            return notificationsInSeconds;
        }

        public void setNotificationsInSeconds(Integer notificationsInSeconds) {
            this.notificationsInSeconds = notificationsInSeconds;
        }
    }

    public static class PortalApis {
        @ParameterKey(Key.PORTAL_APIS_TILESMODE_ENABLED)
        private Enabled tilesMode;

        @ParameterKey(Key.PORTAL_APIS_VIEW_ENABLED)
        private Enabled viewMode;

        public Enabled getTilesMode() {
            return tilesMode;
        }

        public void setTilesMode(Enabled tilesMode) {
            this.tilesMode = tilesMode;
        }

        public Enabled getViewMode() {
            return viewMode;
        }

        public void setViewMode(Enabled viewMode) {
            this.viewMode = viewMode;
        }
    }

    public static class PortalDashboard {
        @ParameterKey(Key.PORTAL_DASHBOARD_WIDGETS)
        //available values:  "geo_country","geo_city"
        private List<String> widgets;

        public List<String> getWidgets() {
            return widgets;
        }

        public void setWidgets(List<String> widgets) {
            this.widgets = widgets;
        }
    }

    public class Documentation {
        @ParameterKey(Key.DOCUMENTATION_URL)
        String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public class Plan {
        private PlanSecurity security;
        public Plan() {
            security = new PlanSecurity();
        }

        public PlanSecurity getSecurity() {
            return security;
        }
        public void setSecurity(PlanSecurity security) {
            this.security = security;
        }
    }

    public static class PlanSecurity {
        @ParameterKey(Key.PLAN_SECURITY_APIKEY_ENABLED)
        private Enabled apikey;

        @ParameterKey(Key.PLAN_SECURITY_OAUTH2_ENABLED)
        private Enabled oauth2;

        @ParameterKey(Key.PLAN_SECURITY_KEYLESS_ENABLED)
        private Enabled keyless;

        @ParameterKey(Key.PLAN_SECURITY_JWT_ENABLED)
        private Enabled jwt;

        public Enabled getApikey() {
            return apikey;
        }

        public void setApikey(Enabled apikey) {
            this.apikey = apikey;
        }

        public Enabled getOauth2() {
            return oauth2;
        }

        public void setOauth2(Enabled oauth2) {
            this.oauth2 = oauth2;
        }

        public Enabled getKeyless() {
            return keyless;
        }

        public void setKeyless(Enabled keyless) {
            this.keyless = keyless;
        }

        public Enabled getJwt() {
            return jwt;
        }

        public void setJwt(Enabled jwt) {
            this.jwt = jwt;
        }
    }
}
