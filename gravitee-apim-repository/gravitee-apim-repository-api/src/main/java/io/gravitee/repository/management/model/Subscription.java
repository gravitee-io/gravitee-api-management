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
package io.gravitee.repository.management.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Subscription implements Serializable {

    public enum AuditEvent implements Audit.ApiAuditEvent {
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_UPDATED,
        SUBSCRIPTION_DELETED,
        SUBSCRIPTION_CLOSED,
        SUBSCRIPTION_PAUSED,
        SUBSCRIPTION_RESUMED,
    }

    /**
     * Subscription ID.
     */
    private String id;

    /**
     * The subscribed {@link Api}.
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

    private Status status;

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

    /**
     * Subscription creation date
     */
    private Date createdAt;

    /**
     * Subscription last update date
     */
    private Date updatedAt;

    private Date closedAt;

    private Date pausedAt;

    private Integer generalConditionsContentRevision;

    private String generalConditionsContentPageId;

    private Boolean generalConditionsAccepted;

    /**
     * Number of days before the expiration of this subscription when the last pre-expiration notification was sent
     */
    private Integer daysToExpirationOnLastNotification;

    /**
     * The configuration on the subscription is used to maintain and store the configuration needed for a subscription.
     * For example, the way to configure a WebHook.
     */
    private String configuration;

    private Map<String, String> metadata;

    private String filter;

    public Subscription() {}

    public Subscription(Subscription cloned) {
        this.id = cloned.id;
        this.api = cloned.api;
        this.plan = cloned.plan;
        this.application = cloned.application;
        this.processedAt = cloned.processedAt;
        this.reason = cloned.reason;
        this.status = cloned.status;
        this.processedBy = cloned.processedBy;
        this.subscribedBy = cloned.subscribedBy;
        this.startingAt = cloned.startingAt;
        this.endingAt = cloned.endingAt;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.closedAt = cloned.closedAt;
        this.pausedAt = cloned.pausedAt;
        this.generalConditionsAccepted = cloned.generalConditionsAccepted;
        this.generalConditionsContentRevision = cloned.generalConditionsContentRevision;
        this.generalConditionsContentPageId = cloned.generalConditionsContentPageId;
        this.daysToExpirationOnLastNotification = cloned.daysToExpirationOnLastNotification;
        this.configuration = cloned.configuration;
        this.metadata = cloned.metadata;
        this.filter = cloned.filter;
    }

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }

    public String getSubscribedBy() {
        return subscribedBy;
    }

    public void setSubscribedBy(String subscribedBy) {
        this.subscribedBy = subscribedBy;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public Integer getGeneralConditionsContentRevision() {
        return generalConditionsContentRevision;
    }

    public void setGeneralConditionsContentRevision(Integer generalConditionsContentRevision) {
        this.generalConditionsContentRevision = generalConditionsContentRevision;
    }

    public String getGeneralConditionsContentPageId() {
        return generalConditionsContentPageId;
    }

    public void setGeneralConditionsContentPageId(String generalConditionsContentPageId) {
        this.generalConditionsContentPageId = generalConditionsContentPageId;
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

        Subscription that = (Subscription) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    // TODO: Validate if the type on the subscription is needed somewhere (gateway for the sync process ?)
    public enum Type {
        REQUEST,

        PUSH,
    }

    public enum Status {
        /**
         * Waiting for validation
         */
        PENDING,

        /**
         * Subscription has been rejected
         */
        REJECTED,

        /**
         * Subscription has been accepted
         */
        ACCEPTED,

        /**
         * Subscription has been closed
         */
        CLOSED,

        /**
         * Subscription has been paused
         */
        PAUSED,
    }
}
