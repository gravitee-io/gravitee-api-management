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

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewSubscriptionEntity {

    private String application;
    private String plan;
    private String request;
    private PageEntity.PageRevisionId generalConditionsContentRevision;
    private Boolean generalConditionsAccepted;

    private String filter;

    private String configuration;

    public NewSubscriptionEntity() {}

    public NewSubscriptionEntity(String plan, String application) {
        this.application = application;
        this.plan = plan;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public Boolean getGeneralConditionsAccepted() {
        return generalConditionsAccepted;
    }

    public void setGeneralConditionsAccepted(Boolean generalConditionsAccepted) {
        this.generalConditionsAccepted = generalConditionsAccepted;
    }

    public PageEntity.PageRevisionId getGeneralConditionsContentRevision() {
        return generalConditionsContentRevision;
    }

    public void setGeneralConditionsContentRevision(PageEntity.PageRevisionId generalConditionsContentRevision) {
        this.generalConditionsContentRevision = generalConditionsContentRevision;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
