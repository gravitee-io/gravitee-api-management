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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class SubscriptionEntity {

    private String id;

    private String api;

    private String environmentId;

    private String plan;

    private String application;

    private SubscriptionStatus status;

    @JsonProperty("consumerStatus")
    private SubscriptionConsumerStatus consumerStatus;

    @JsonProperty("processed_at")
    private Date processedAt;

    @JsonProperty("processed_by")
    private String processedBy;

    @JsonProperty("subscribed_by")
    private String subscribedBy;

    private String request;

    private String reason;

    @JsonProperty("starting_at")
    private Date startingAt;

    @JsonProperty("ending_at")
    private Date endingAt;

    /**
     * Subscription creation date
     */
    @JsonProperty("created_at")
    private Date createdAt;

    /**
     * Subscription last update date
     */
    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("closed_at")
    private Date closedAt;

    @JsonProperty("paused_at")
    private Date pausedAt;

    @JsonProperty("consumerPausedAt")
    private Date consumerPausedAt;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_certificate")
    private String clientCertificate;

    private List<String> keys;

    private String security;

    /**
     * Number of days before the pollInterval of this subscription when the last pre-pollInterval notification was sent
     */
    private Integer daysToExpirationOnLastNotification;

    private SubscriptionConfigurationEntity configuration;

    private Map<String, String> metadata;

    private String failureCause;

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

    public String getEnvironmentId() {
        return environmentId;
    }

    public SubscriptionEntity setEnvironmentId(final String environmentId) {
        this.environmentId = environmentId;
        return this;
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

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public SubscriptionConsumerStatus getConsumerStatus() {
        return consumerStatus;
    }

    public void setConsumerStatus(SubscriptionConsumerStatus consumerStatus) {
        this.consumerStatus = consumerStatus;
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

    public String getClientCertificate() {
        return clientCertificate;
    }

    public void setClientCertificate(String clientCertificate) {
        this.clientCertificate = clientCertificate;
    }

    public Date getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(Date pausedAt) {
        this.pausedAt = pausedAt;
    }

    public Date getConsumerPausedAt() {
        return consumerPausedAt;
    }

    public void setConsumerPausedAt(Date consumerPausedAt) {
        this.consumerPausedAt = consumerPausedAt;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    public Integer getDaysToExpirationOnLastNotification() {
        return daysToExpirationOnLastNotification;
    }

    public void setDaysToExpirationOnLastNotification(Integer daysToExpirationOnLastNotification) {
        this.daysToExpirationOnLastNotification = daysToExpirationOnLastNotification;
    }

    public SubscriptionConfigurationEntity getConfiguration() {
        return configuration;
    }

    public void setConfiguration(SubscriptionConfigurationEntity configuration) {
        this.configuration = configuration;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void setFailureCause(String failureCause) {
        this.failureCause = failureCause;
    }

    public String getFailureCause() {
        return failureCause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubscriptionEntity that = (SubscriptionEntity) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
