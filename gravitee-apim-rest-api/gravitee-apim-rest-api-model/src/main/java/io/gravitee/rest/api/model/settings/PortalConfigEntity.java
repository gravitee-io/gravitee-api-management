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
package io.gravitee.rest.api.model.settings;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
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
    private PortalNext portalNext;
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
        portalNext = new PortalNext();
        reCaptcha = new PortalReCaptcha();
        scheduler = new PortalScheduler();
        dashboards = new Dashboards();
    }
}
