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
package io.gravitee.rest.api.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(value = {"baseURL", "orgBaseURL", "envBaseURL"}, ignoreUnknown = true)
public class ConsoleConfigEntity {

    public static final String METADATA_READONLY = "readonly";

    private Company company;
    private Management management;
    private Portal portal;
    private Authentication authentication;
    private Scheduler scheduler;
    private Documentation documentation;
    private Theme theme;
    private Plan plan;
    private OpenAPIDocViewer openAPIDocViewer;
    private ApiQualityMetrics apiQualityMetrics;
    private ApiReview apiReview;
    private Logging logging;
    private Analytics analytics;
    private Application application;
    private Alert alert;
    private Maintenance maintenance;
    private Newsletter newsletter;
    private ReCaptcha reCaptcha;
    private Api api;
    private Cors cors;
    private Email email;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private MultiValueMap<String, String> metadata;

    public ConsoleConfigEntity() {
        company = new Company();
        management = new Management();
        portal = new Portal();
        authentication = new Authentication();
        scheduler = new Scheduler();
        documentation = new Documentation();
        theme = new Theme();
        plan = new Plan();
        openAPIDocViewer = new OpenAPIDocViewer();
        apiQualityMetrics = new ApiQualityMetrics();
        apiReview = new ApiReview();
        logging = new Logging();
        analytics = new Analytics();
        application = new Application();
        alert = new Alert();
        maintenance = new Maintenance();
        newsletter = new Newsletter();
        reCaptcha = new ReCaptcha();
        api = new Api();
        cors = new Cors();
        email = new Email();
        metadata = new LinkedMultiValueMap<>();
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

    public OpenAPIDocViewer getOpenAPIDocViewer() {
        return openAPIDocViewer;
    }

    public void setOpenAPIDocViewer(OpenAPIDocViewer openAPIDocViewer) {
        this.openAPIDocViewer = openAPIDocViewer;
    }

    public ApiQualityMetrics getApiQualityMetrics() {
        return apiQualityMetrics;
    }

    public void setApiQualityMetrics(ApiQualityMetrics apiQualityMetrics) {
        this.apiQualityMetrics = apiQualityMetrics;
    }

    public ApiReview getApiReview() {
        return apiReview;
    }

    public void setApiReview(ApiReview apiReview) {
        this.apiReview = apiReview;
    }

    public Logging getLogging() {
        return logging;
    }

    public void setLogging(Logging logging) {
        this.logging = logging;
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public Maintenance getMaintenance() {
        return maintenance;
    }

    public void setMaintenance(Maintenance maintenance) {
        this.maintenance = maintenance;
    }

    public Newsletter getNewsletter() {
        return newsletter;
    }

    public void setNewsletter(Newsletter newsletter) {
        this.newsletter = newsletter;
    }

    public ReCaptcha getReCaptcha() {
        return reCaptcha;
    }

    public void setReCaptcha(ReCaptcha reCaptcha) {
        this.reCaptcha = reCaptcha;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public MultiValueMap<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(MultiValueMap<String, String> metadata) {
        this.metadata = metadata;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Enabled {
        private boolean enabled;

        Enabled() {
        }

        public Enabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
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

        public String getTrackingId() {
            return trackingId;
        }

        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PortalApis {
        @ParameterKey(Key.PORTAL_APIS_TILESMODE_ENABLED)
        private Enabled tilesMode;

        @ParameterKey(Key.PORTAL_APIS_CATEGORY_ENABLED)
        private Enabled categoryMode;

        @ParameterKey(Key.PORTAL_APIS_SHOW_TAGS_IN_APIHEADER)
        private Enabled apiHeaderShowTags;

        @ParameterKey(Key.PORTAL_APIS_SHOW_CATEGORIES_IN_APIHEADER)
        private Enabled apiHeaderShowCategories;

        public Enabled getTilesMode() {
            return tilesMode;
        }

        public void setTilesMode(Enabled tilesMode) {
            this.tilesMode = tilesMode;
        }

        public Enabled getCategoryMode() {
            return categoryMode;
        }

        public void setCategoryMode(Enabled categoryMode) {
            this.categoryMode = categoryMode;
        }

        public Enabled getApiHeaderShowTags() {
            return apiHeaderShowTags;
        }

        public void setApiHeaderShowTags(Enabled apiHeaderShowTags) {
            this.apiHeaderShowTags = apiHeaderShowTags;
        }

        public Enabled getApiHeaderShowCategories() {
            return apiHeaderShowCategories;
        }

        public void setApiHeaderShowCategories(Enabled apiHeaderShowCategories) {
            this.apiHeaderShowCategories = apiHeaderShowCategories;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlanSecurity {
        @ParameterKey(Key.PLAN_SECURITY_APIKEY_ENABLED)
        private Enabled apikey;

        @ParameterKey(Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED)
        private Enabled customApiKey;

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

        public Enabled getCustomApiKey() {
            return customApiKey;
        }

        public void setCustomApiKey(Enabled customApiKey) {
            this.customApiKey = customApiKey;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Cors {
        @ParameterKey(Key.HTTP_CORS_ALLOW_ORIGIN)
        private List<String> allowOrigin;

        @ParameterKey(Key.HTTP_CORS_ALLOW_HEADERS)
        private List<String> allowHeaders;

        @ParameterKey(Key.HTTP_CORS_ALLOW_METHODS)
        private List<String> allowMethods;

        @ParameterKey(Key.HTTP_CORS_EXPOSED_HEADERS)
        private List<String> exposedHeaders;

        @ParameterKey(Key.HTTP_CORS_MAX_AGE)
        private Integer maxAge;

        public List<String> getAllowOrigin() {
            return allowOrigin;
        }

        public void setAllowOrigin(List<String> allowOrigin) {
            this.allowOrigin = allowOrigin;
        }

        public List<String> getAllowHeaders() {
            return allowHeaders;
        }

        public void setAllowHeaders(List<String> allowHeaders) {
            this.allowHeaders = allowHeaders;
        }

        public List<String> getAllowMethods() {
            return allowMethods;
        }

        public void setAllowMethods(List<String> allowMethods) {
            this.allowMethods = allowMethods;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public Integer getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Integer maxAge) {
            this.maxAge = maxAge;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Email {
        @ParameterKey(Key.EMAIL_ENABLED)
        private Boolean enabled;

        @ParameterKey(Key.EMAIL_HOST)
        private String host;

        @ParameterKey(Key.EMAIL_PORT)
        private Integer port;

        @ParameterKey(Key.EMAIL_USERNAME)
        private String username;

        @ParameterKey(Key.EMAIL_PASSWORD)
        private String password;

        @ParameterKey(Key.EMAIL_PROTOCOL)
        private String protocol;

        @ParameterKey(Key.EMAIL_SUBJECT)
        private String subject;

        @ParameterKey(Key.EMAIL_FROM)
        private String from;

        private EmailProperties properties;

        public Email() {
            properties = new EmailProperties();
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public EmailProperties getProperties() {
            return properties;
        }

        public void setProperties(EmailProperties properties) {
            this.properties = properties;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public class EmailProperties {
            @ParameterKey(Key.EMAIL_PROPERTIES_AUTH_ENABLED)
            private Boolean auth;

            @ParameterKey(Key.EMAIL_PROPERTIES_STARTTLS_ENABLE)
            private Boolean startTlsEnable;

            @ParameterKey(Key.EMAIL_PROPERTIES_SSL_TRUST)
            private String sslTrust;

            public Boolean getAuth() {
                return auth;
            }

            public void setAuth(Boolean auth) {
                this.auth = auth;
            }

            public Boolean getStartTlsEnable() {
                return startTlsEnable;
            }

            public void setStartTlsEnable(Boolean startTlsEnable) {
                this.startTlsEnable = startTlsEnable;
            }

            public String getSslTrust() {
                return sslTrust;
            }

            public void setSslTrust(String sslTrust) {
                this.sslTrust = sslTrust;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class OpenAPIDocViewer {
        private OpenAPIDocType openAPIDocType;

        public OpenAPIDocViewer() { openAPIDocType = new OpenAPIDocType(); }

        public OpenAPIDocType getOpenAPIDocType() { return openAPIDocType; }

        public void setOpenAPIDocType(OpenAPIDocType openAPIDocType) {
            this.openAPIDocType = openAPIDocType;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAPIDocType {
        @ParameterKey(Key.OPEN_API_DOC_TYPE_SWAGGER_ENABLED)
        private Enabled swagger;

        @ParameterKey(Key.OPEN_API_DOC_TYPE_REDOC_ENABLED)
        private Enabled redoc;

        @ParameterKey(Key.OPEN_API_DOC_TYPE_DEFAULT)
        private String defaultType;

        public Enabled getSwagger() {
            return swagger;
        }

        public void setSwagger(Enabled swagger) {
            this.swagger = swagger;
        }

        public Enabled getRedoc() {
            return redoc;
        }

        public void setRedoc(Enabled redoc) {
            this.redoc = redoc;
        }

        public String getDefaultType() {
            return defaultType;
        }

        public void setDefaultType(String defaultType) {
            this.defaultType = defaultType;
        }
    }

}
