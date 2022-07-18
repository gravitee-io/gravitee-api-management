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
package io.gravitee.repository.mongodb.management.internal.model;

import io.gravitee.repository.management.model.Plan;
import java.util.Date;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "subscriptions")
public class SubscriptionMongo extends Auditable {

    /**
     * Subscription ID.
     */
    @Id
    private String id;

    /**
     * The subscribed {@link io.gravitee.repository.management.model.Api}.
     */
    private String api;

    /**
     * The subscribed {@link Plan}.
     */
    private String plan;

    /**
     * The application linked to the subscription
     */
    private String application;

    /**
     * The clientId linked to the subscription
     */
    private String clientId;

    /**
     * PENDING, ACCEPTED, REJECTED
     */
    private String status;

    /**
     * Vhen the subscription have been processed.
     */
    private Date processedAt;

    /**
     * Give a request message to the api owner why a user want to subscribe
     */
    private String request;

    /**
     * Give a reason to the developer if the subscription is accepted or not.
     */
    private String reason;

    /**
     * The username of the user who has processed the subscription
     * <code>null</code> if the subscription is relative to an automatic plan.
     */
    private String processedBy;

    /**
     * The username of the user who has subscribed to the plan.
     */
    private String subscribedBy;

    private Date startingAt;

    private Date endingAt;

    private Date closedAt;

    private Date pausedAt;

    private PageRevisionPkMongo generalConditionsContentRevision;

    private Boolean generalConditionsAccepted;

    /**
     * Number of days before the expiration of this subscription when the last pre-expiration notification was sent
     */
    private Integer daysToExpirationOnLastNotification;

    private String configuration;

    private Map<String, String> metadata;

    private String filter;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public Date getStartingAt() {
        return startingAt;
    }

    public void setStartingAt(Date startingAt) {
        this.startingAt = startingAt;
    }

    public Date getEndingAt() {
        return endingAt;
    }

    public void setEndingAt(Date endingAt) {
        this.endingAt = endingAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubscribedBy() {
        return subscribedBy;
    }

    public void setSubscribedBy(String subscribedBy) {
        this.subscribedBy = subscribedBy;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Date getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(Date pausedAt) {
        this.pausedAt = pausedAt;
    }

    public PageRevisionPkMongo getGeneralConditionsContentRevision() {
        return generalConditionsContentRevision;
    }

    public void setGeneralConditionsContentRevision(PageRevisionPkMongo generalConditionsContentRevision) {
        this.generalConditionsContentRevision = generalConditionsContentRevision;
    }

    public Boolean getGeneralConditionsAccepted() {
        return generalConditionsAccepted;
    }

    public void setGeneralConditionsAccepted(Boolean generalConditionsAccepted) {
        this.generalConditionsAccepted = generalConditionsAccepted;
    }

    public Integer getDaysToExpirationOnLastNotification() {
        return daysToExpirationOnLastNotification;
    }

    public void setDaysToExpirationOnLastNotification(Integer daysToExpirationOnLastNotification) {
        this.daysToExpirationOnLastNotification = daysToExpirationOnLastNotification;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionMongo that = (SubscriptionMongo) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
