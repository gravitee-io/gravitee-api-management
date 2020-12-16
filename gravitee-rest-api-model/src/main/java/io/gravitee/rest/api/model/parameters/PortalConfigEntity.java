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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.annotations.ParameterKey;

import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalConfigEntity extends AbstractCommonConfigEntity{
    private Analytics analytics;
    private Api api;
    private ApiQualityMetrics apiQualityMetrics;
    private ApiReview apiReview;
    private Application application;
    private Authentication authentication;
    private Company company;
    private Cors cors;
    private Documentation documentation;
    private OpenAPIDocViewer openAPIDocViewer;
    private Plan plan;
    private Portal portal;
    private ReCaptcha reCaptcha;
    private Scheduler scheduler;


    public PortalConfigEntity() {
        super();
        analytics = new Analytics();
        api = new Api();
        apiQualityMetrics = new ApiQualityMetrics();
        apiReview = new ApiReview();
        application = new Application();
        authentication = new Authentication();
        company = new Company();
        cors = new Cors();
        documentation = new Documentation();
        openAPIDocViewer = new OpenAPIDocViewer();
        plan = new Plan();
        portal = new Portal();
        reCaptcha = new ReCaptcha();
        scheduler = new Scheduler();
    }

     // Getters & Setters
     public Analytics getAnalytics() {
         return analytics;
     }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
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

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public Documentation getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Documentation documentation) {
        this.documentation = documentation;
    }

    public OpenAPIDocViewer getOpenAPIDocViewer() {
        return openAPIDocViewer;
    }

    public void setOpenAPIDocViewer(OpenAPIDocViewer openAPIDocViewer) {
        this.openAPIDocViewer = openAPIDocViewer;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public Portal getPortal() {
        return portal;
    }

    public void setPortal(Portal portal) {
        this.portal = portal;
    }

    public ReCaptcha getReCaptcha() {
        return reCaptcha;
    }

    public void setReCaptcha(ReCaptcha reCaptcha) {
        this.reCaptcha = reCaptcha;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    // Classes
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Analytics {
        @ParameterKey(Key.ANALYTICS_CLIENT_TIMEOUT)
        private Long clientTimeout;

        public Long getClientTimeout() {
            return clientTimeout;
        }
        public void setClientTimeout(Long clientTimeout) {
            this.clientTimeout = clientTimeout;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Api {
        @ParameterKey(Key.API_LABELS_DICTIONARY)
        private List<String> labelsDictionary;

        public List<String> getLabelsDictionary() {
            return labelsDictionary;
        }

        public void setLabelsDictionary(List<String> labelsDictionary) {
            this.labelsDictionary = labelsDictionary;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiQualityMetrics {

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiReview {
        @ParameterKey(Key.API_REVIEW_ENABLED)
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Application {

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

        public static class ApplicationTypes {
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

        public static class ClientRegistration {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Authentication extends CommonAuthentication {
        @ParameterKey(Key.PORTAL_AUTHENTICATION_FORCELOGIN_ENABLED)
        private Enabled forceLogin;
        @ParameterKey(Key.PORTAL_AUTHENTICATION_LOCALLOGIN_ENABLED)
        private Enabled localLogin;

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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cors {
        @ParameterKey(Key.PORTAL_HTTP_CORS_ALLOW_ORIGIN)
        private List<String> allowOrigin;

        @ParameterKey(Key.PORTAL_HTTP_CORS_ALLOW_HEADERS)
        private List<String> allowHeaders;

        @ParameterKey(Key.PORTAL_HTTP_CORS_ALLOW_METHODS)
        private List<String> allowMethods;

        @ParameterKey(Key.PORTAL_HTTP_CORS_EXPOSED_HEADERS)
        private List<String> exposedHeaders;

        @ParameterKey(Key.PORTAL_HTTP_CORS_MAX_AGE)
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
    public static class Company {

        @ParameterKey(Key.COMPANY_NAME)
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }


    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Documentation {

        @ParameterKey(Key.DOCUMENTATION_URL)
        String url;
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenAPIDocViewer {
        private OpenAPIDocType openAPIDocType;

        public OpenAPIDocViewer() { openAPIDocType = new OpenAPIDocType(); }

        public OpenAPIDocType getOpenAPIDocType() { return openAPIDocType; }

        public void setOpenAPIDocType(OpenAPIDocType openAPIDocType) {
            this.openAPIDocType = openAPIDocType;
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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Plan {
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
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Portal {
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

        // Classes
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

        public static class PortalRating {
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

            public static class RatingComment {
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

        public static class PortalUploadMedia {
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

        public static class PortalUserCreation {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReCaptcha {
        @ParameterKey(Key.PORTAL_RECAPTCHA_ENABLED)
        private Boolean enabled;
        @ParameterKey(Key.PORTAL_RECAPTCHA_SITE_KEY)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Scheduler {
        @JsonProperty("tasks")
        @ParameterKey(Key.PORTAL_SCHEDULER_TASKS)
        private Integer tasksInSeconds;

        @JsonProperty("notifications")
        @ParameterKey(Key.PORTAL_SCHEDULER_NOTIFICATIONS)
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
}
