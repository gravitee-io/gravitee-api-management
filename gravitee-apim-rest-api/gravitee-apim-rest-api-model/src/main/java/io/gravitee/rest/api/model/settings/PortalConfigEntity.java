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

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalConfigEntity {

    private Analytics analytics;
    private Api api;
    private ApiQualityMetrics apiQualityMetrics;
    private ApiReview apiReview;
    private PortalApplicationSettings application;
    private PortalAuthentication authentication;
    private Company company;
    private Documentation documentation;
    private OpenAPIDocViewer openAPIDocViewer;
    private PlanSettings plan;
    private Portal portal;
    private PortalReCaptcha reCaptcha;
    private PortalScheduler scheduler;
    private Dashboards dashboards;

    public PortalConfigEntity() {
        super();
        analytics = new Analytics();
        api = new Api();
        apiQualityMetrics = new ApiQualityMetrics();
        apiReview = new ApiReview();
        application = new PortalApplicationSettings();
        authentication = new PortalAuthentication();
        company = new Company();
        documentation = new Documentation();
        openAPIDocViewer = new OpenAPIDocViewer();
        plan = new PlanSettings();
        portal = new Portal();
        reCaptcha = new PortalReCaptcha();
        scheduler = new PortalScheduler();
        dashboards = new Dashboards();
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

    public PortalApplicationSettings getApplication() {
        return application;
    }

    public void setApplication(PortalApplicationSettings application) {
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

    public PlanSettings getPlan() {
        return plan;
    }

    public void setPlan(PlanSettings plan) {
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

    public Dashboards getDashboards() {
        return dashboards;
    }

    public void setDashboards(Dashboards dashboards) {
        this.dashboards = dashboards;
    }
}
