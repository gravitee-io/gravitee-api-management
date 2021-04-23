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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import java.util.List;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalSettingsEntity extends AbstractCommonSettingsEntity {

    private Analytics analytics;
    private Api api;
    private ApiQualityMetrics apiQualityMetrics;
    private ApiReview apiReview;
    private Application application;
    private PortalAuthentication authentication;
    private Company company;
    private PortalCors cors;
    private Documentation documentation;
    private OpenAPIDocViewer openAPIDocViewer;
    private Plan plan;
    private Portal portal;
    private PortalReCaptcha reCaptcha;
    private PortalScheduler scheduler;

    public PortalSettingsEntity() {
        super();
        analytics = new Analytics();
        api = new Api();
        apiQualityMetrics = new ApiQualityMetrics();
        apiReview = new ApiReview();
        application = new Application();
        authentication = new PortalAuthentication();
        company = new Company();
        cors = new PortalCors();
        documentation = new Documentation();
        openAPIDocViewer = new OpenAPIDocViewer();
        plan = new Plan();
        portal = new Portal();
        reCaptcha = new PortalReCaptcha();
        scheduler = new PortalScheduler();
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

    public PortalAuthentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(PortalAuthentication authentication) {
        this.authentication = authentication;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public PortalCors getCors() {
        return cors;
    }

    public void setCors(PortalCors cors) {
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

    public PortalReCaptcha getReCaptcha() {
        return reCaptcha;
    }

    public void setReCaptcha(PortalReCaptcha reCaptcha) {
        this.reCaptcha = reCaptcha;
    }

    public PortalScheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(PortalScheduler scheduler) {
        this.scheduler = scheduler;
    }

    // Classes
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PortalCors {

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
}
