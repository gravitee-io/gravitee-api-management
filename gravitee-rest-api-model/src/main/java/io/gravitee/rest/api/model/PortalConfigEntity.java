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
package io.gravitee.rest.api.model;

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
@JsonIgnoreProperties(value = {"baseURL", "orgBaseURL", "envBaseURL"})
public class PortalConfigEntity {

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

    public PortalConfigEntity() {
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

    public static class Alert {
        @ParameterKey(Key.ALERT_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

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

    public static class Newsletter {
        @ParameterKey(Key.NEWSLETTER_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ReCaptcha {
        @ParameterKey(Key.RECAPTCHA_ENABLED)
        private Boolean enabled;
        @ParameterKey(Key.RECAPTCHA_SITE_KEY)
        private String siteKey;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getSiteKey() {
            return siteKey;
        }

        public void setSiteKey(String siteKey) {
            this.siteKey = siteKey;
        }
    }

    public class Api {
        @ParameterKey(Key.API_LABELS_DICTIONARY)
        private List<String> labelsDictionary;

        public List<String> getLabelsDictionary() {
            return labelsDictionary;
        }

        public void setLabelsDictionary(List<String> labelsDictionary) {
            this.labelsDictionary = labelsDictionary;
        }
    }

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
        @ParameterKey(Key.PORTAL_ENTRYPOINT)
        private String entrypoint;
        @ParameterKey(Key.PORTAL_APIKEY_HEADER)
        private String apikeyHeader;
        @ParameterKey(Key.PORTAL_SUPPORT_ENABLED)
        private Enabled support;
        @ParameterKey(Key.PORTAL_URL)
        private String url;

        private PortalApis apis;
        private PortalAnalytics analytics;
        private PortalRating rating;

        private PortalUploadMedia media;

        private PortalUserCreation userCreation;

        public Portal() {
            apis = new PortalApis();
            analytics = new PortalAnalytics();
            rating = new PortalRating();
            media = new PortalUploadMedia();
            userCreation = new PortalUserCreation();
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

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public PortalApis getApis() {
            return apis;
        }

        public void setApis(PortalApis apis) {
            this.apis = apis;
        }

        public Enabled getSupport() {
            return support;
        }

        public void setSupport(Enabled support) {
            this.support = support;
        }

        public PortalUserCreation getUserCreation() {
            return userCreation;
        }

        public void setUserCreation(PortalUserCreation userCreation) {
            this.userCreation = userCreation;
        }

        public PortalAnalytics getAnalytics() {
            return analytics;
        }

        public void setAnalytics(PortalAnalytics analytics) {
            this.analytics = analytics;
        }

        public PortalRating getRating() {
            return rating;
        }

        public void setRating(PortalRating rating) {
            this.rating = rating;
        }

        public PortalUploadMedia getUploadMedia() {
            return media;
        }

        public void setUploadMedia(PortalUploadMedia media) {
            this.media = media;
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

        public class PortalUploadMedia {
            @ParameterKey(Key.PORTAL_UPLOAD_MEDIA_ENABLED)
            private Boolean enabled;
            @ParameterKey(Key.PORTAL_UPLOAD_MEDIA_MAXSIZE)
            private Integer maxSizeInOctet;

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public Integer getMaxSizeInOctet() {
                return maxSizeInOctet;
            }

            public void setMaxSizeInOctet(Integer maxSizeInOctet) {
                this.maxSizeInOctet = maxSizeInOctet;
            }
        }

        public class PortalUserCreation {
            @ParameterKey(Key.PORTAL_USERCREATION_ENABLED)
            private Boolean enabled;
            @ParameterKey(Key.PORTAL_USERCREATION_AUTOMATICVALIDATION_ENABLED)
            private Enabled automaticValidation;

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public Enabled getAutomaticValidation() {
                return automaticValidation;
            }

            public void setAutomaticValidation(Enabled automaticValidation) {
                this.automaticValidation = automaticValidation;
            }
        }
    }

    public class Management {
        @ParameterKey(Key.MANAGEMENT_TITLE)
        private String title;
        @ParameterKey(Key.MANAGEMENT_URL)
        private String url;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
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

    public class OpenAPIDocViewer {
        private OpenAPIDocType openAPIDocType;

        public OpenAPIDocViewer() { openAPIDocType = new OpenAPIDocType(); }

        public OpenAPIDocType getOpenAPIDocType() { return openAPIDocType; }

        public void setOpenAPIDocType(OpenAPIDocType openAPIDocType) {
            this.openAPIDocType = openAPIDocType;
        }
    }

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

    public class ApiQualityMetrics {

        @ParameterKey(Key.API_QUALITY_METRICS_ENABLED)
        private Boolean enabled;

        @ParameterKey(Key.API_QUALITY_METRICS_FUNCTIONAL_DOCUMENTATION_WEIGHT)
        private Integer functionalDocumentationWeight;

        @ParameterKey(Key.API_QUALITY_METRICS_TECHNICAL_DOCUMENTATION_WEIGHT)
        private Integer technicalDocumentationWeight;

        @ParameterKey(Key.API_QUALITY_METRICS_HEALTHCHECK_WEIGHT)
        private Integer HealthcheckWeight;

        @ParameterKey(Key.API_QUALITY_METRICS_DESCRIPTION_WEIGHT)
        private Integer descriptionWeight;

        @ParameterKey(Key.API_QUALITY_METRICS_DESCRIPTION_MIN_LENGTH)
        private Integer descriptionMinLength;

        @ParameterKey(Key.API_QUALITY_METRICS_LOGO_WEIGHT)
        private Integer logoWeight;

        @ParameterKey(Key.API_QUALITY_METRICS_CATEGORIES_WEIGHT)
        private Integer categoriesWeight;

        @ParameterKey(Key.API_QUALITY_METRICS_LABELS_WEIGHT)
        private Integer labelsWeight;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getFunctionalDocumentationWeight() {
            return functionalDocumentationWeight;
        }

        public void setFunctionalDocumentationWeight(Integer functionalDocumentationWeight) {
            this.functionalDocumentationWeight = functionalDocumentationWeight;
        }

        public Integer getTechnicalDocumentationWeight() {
            return technicalDocumentationWeight;
        }

        public void setTechnicalDocumentationWeight(Integer technicalDocumentationWeight) {
            this.technicalDocumentationWeight = technicalDocumentationWeight;
        }

        public Integer getHealthcheckWeight() {
            return HealthcheckWeight;
        }

        public void setHealthcheckWeight(Integer healthcheckWeight) {
            HealthcheckWeight = healthcheckWeight;
        }

        public Integer getDescriptionWeight() {
            return descriptionWeight;
        }

        public void setDescriptionWeight(Integer descriptionWeight) {
            this.descriptionWeight = descriptionWeight;
        }

        public Integer getDescriptionMinLength() {
            return descriptionMinLength;
        }

        public void setDescriptionMinLength(Integer descriptionMinLength) {
            this.descriptionMinLength = descriptionMinLength;
        }

        public Integer getLogoWeight() {
            return logoWeight;
        }

        public void setLogoWeight(Integer logoWeight) {
            this.logoWeight = logoWeight;
        }

        public Integer getCategoriesWeight() {
            return categoriesWeight;
        }

        public void setCategoriesWeight(Integer categoriesWeight) {
            this.categoriesWeight = categoriesWeight;
        }

        public Integer getLabelsWeight() {
            return labelsWeight;
        }

        public void setLabelsWeight(Integer labelsWeight) {
            this.labelsWeight = labelsWeight;
        }
    }

    public class ApiReview {
        @ParameterKey(Key.API_REVIEW_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    public class Logging {
        @ParameterKey(Key.LOGGING_DEFAULT_MAX_DURATION)
        private Long maxDurationMillis;
        private Audit audit = new Audit();
        private User user = new User();

        public Long getMaxDurationMillis() {
            return maxDurationMillis;
        }

        public void setMaxDurationMillis(Long maxDurationMillis) {
            this.maxDurationMillis = maxDurationMillis;
        }

        public Audit getAudit() {
            return audit;
        }

        public void setAudit(Audit audit) {
            this.audit = audit;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public class Audit {
            @ParameterKey(Key.LOGGING_AUDIT_ENABLED)
            private Boolean enabled;
            private AuditTrail trail = new AuditTrail();

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public AuditTrail getTrail() {
                return trail;
            }

            public void setTrail(AuditTrail trail) {
                this.trail = trail;
            }

            public class AuditTrail {
                @ParameterKey(Key.LOGGING_AUDIT_TRAIL_ENABLED)
                private Boolean enabled;

                public Boolean getEnabled() {
                    return enabled;
                }

                public void setEnabled(Boolean enabled) {
                    this.enabled = enabled;
                }
            }
        }

        public class User {
            @ParameterKey(Key.LOGGING_USER_DISPLAYED)
            private Boolean displayed;

            public Boolean getDisplayed() {
                return displayed;
            }

            public void setDisplayed(Boolean displayed) {
                this.displayed = displayed;
            }
        }
    }

    public class Analytics {
        @ParameterKey(Key.ANALYTICS_CLIENT_TIMEOUT)
        private Long clientTimeout;

        public Long getClientTimeout() {
            return clientTimeout;
        }

        public void setClientTimeout(Long clientTimeout) {
            this.clientTimeout = clientTimeout;
        }
    }

    public class Application {

        private ClientRegistration registration = new ClientRegistration();
        private ApplicationTypes types = new ApplicationTypes();

        public ClientRegistration getRegistration() {
            return registration;
        }

        public void setRegistration(ClientRegistration registration) {
            this.registration = registration;
        }

        public ApplicationTypes getTypes() {
            return types;
        }

        public class ApplicationTypes {
            @JsonProperty("simple")
            @ParameterKey(Key.APPLICATION_TYPE_SIMPLE_ENABLED)
            private Enabled simpleType;

            @JsonProperty("browser")
            @ParameterKey(Key.APPLICATION_TYPE_BROWSER_ENABLED)
            private Enabled browserType;

            @JsonProperty("web")
            @ParameterKey(Key.APPLICATION_TYPE_WEB_ENABLED)
            private Enabled webType;

            @JsonProperty("native")
            @ParameterKey(Key.APPLICATION_TYPE_NATIVE_ENABLED)
            private Enabled nativeType;

            @JsonProperty("backend_to_backend")
            @ParameterKey(Key.APPLICATION_TYPE_BACKEND_TO_BACKEND_ENABLED)
            private Enabled backendToBackendType;

            public Enabled getSimpleType() {
                return simpleType;
            }

            public void setSimpleType(Enabled simpleType) {
                this.simpleType = simpleType;
            }

            public Enabled getBrowserType() {
                return browserType;
            }

            public void setBrowserType(Enabled browserType) {
                this.browserType = browserType;
            }

            public Enabled getWebType() {
                return webType;
            }

            public void setWebType(Enabled webType) {
                this.webType = webType;
            }

            public Enabled getNativeType() {
                return nativeType;
            }

            public void setNativeType(Enabled nativeType) {
                this.nativeType = nativeType;
            }

            public Enabled getBackendToBackendType() {
                return backendToBackendType;
            }

            public void setBackendToBackendType(Enabled backendToBackendType) {
                this.backendToBackendType = backendToBackendType;
            }
        }

        public class ClientRegistration {
            @ParameterKey(Key.APPLICATION_REGISTRATION_ENABLED)
            private Boolean enabled;

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    public class Maintenance {
        @ParameterKey(Key.MAINTENANCE_MODE_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
