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
package io.gravitee.repository.management.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ApiKey implements Serializable {

    public enum AuditEvent implements Audit.ApiAuditEvent {
        APIKEY_CREATED,
        APIKEY_RENEWED,
        APIKEY_REVOKED,
        APIKEY_EXPIRED,
        APIKEY_REACTIVATED,
    }

    /**
     * API Key's unique id
     */
    @Getter
    private String id;

    /**
     * API Key
     */
    @Getter
    private String key;

    /**
     * The subscriptions for which the API Key is generated
     */
    @Getter
    private List<String> subscriptions = new ArrayList<>();

    /**
     * The subscription for which the API Key is generated
     *
     * @deprecated
     * Starting from 3.17 this field is kept for backward compatibility only and subscriptions should be used instead
     */
    @Deprecated(since = "3.17.0", forRemoval = true)
    private String subscription;

    /**
     * The subscribed plan
     *
     * @deprecated
     * Starting from 3.17 this field is kept for backward compatibility and plans should be accessed through subscriptions instead
     */
    @Deprecated(since = "3.17.0", forRemoval = true)
    private String plan;

    /**
     * The api on which this API Key is used
     *
     * @deprecated
     * Starting from 3.17 this field is kept for backward compatibility and apis should be accessed through subscriptions instead
     */
    @Deprecated(since = "3.17.0", forRemoval = true)
    private String api;

    /**
     * The application used to make the subscription
     */
    @Getter
    private String application;

    /**
     * Expiration date (end date) of the API Key
     */
    @Getter
    private Date expireAt;

    /**
     * API Key creation date
     */
    @Getter
    private Date createdAt;

    /**
     * API Key updated date
     */
    @Getter
    private Date updatedAt;

    /**
     * Flag to indicate if the API Key is revoked ?
     */
    @Getter
    private boolean revoked;

    /**
     * Flag to indicate if the API Key is paused ?
     */
    @Getter
    private boolean paused;

    /**
     * If the key is revoked, the revocation date
     */
    @Getter
    private Date revokedAt;

    /**
     * Number of days before the expiration of this API Key when the last pre-expiration notification was sent
     */
    @Getter
    private Integer daysToExpirationOnLastNotification;

    public ApiKey(ApiKey cloned) {
        this.id = cloned.id;
        this.key = cloned.key;
        this.subscriptions = cloned.subscriptions;
        this.application = cloned.application;
        this.plan = cloned.plan;
        this.expireAt = cloned.expireAt;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.revoked = cloned.revoked;
        this.revokedAt = cloned.revokedAt;
        this.paused = cloned.paused;
        this.daysToExpirationOnLastNotification = cloned.daysToExpirationOnLastNotification;
        this.api = cloned.api;
    }

    public ApiKey revoke() {
        var revokedApiKey = new ApiKey(this);
        var now = new Date();
        revokedApiKey.setRevoked(true);
        revokedApiKey.setUpdatedAt(now);
        revokedApiKey.setRevokedAt(now);
        return revokedApiKey;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setExpireAt(Date expireAt) {
        this.expireAt = expireAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setRevokedAt(Date revokedAt) {
        this.revokedAt = revokedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setSubscriptions(List<String> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setDaysToExpirationOnLastNotification(Integer daysToExpirationOnLastNotification) {
        this.daysToExpirationOnLastNotification = daysToExpirationOnLastNotification;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @deprecated
     * Starting from 3.17 this field is kept for backward compatibility only and subscriptions should be used instead
     */
    @Deprecated(since = "3.17.0", forRemoval = true)
    public String getSubscription() {
        return subscription;
    }

    @Deprecated(since = "3.17.0", forRemoval = true)
    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    /**
     * @deprecated
     * Starting from 3.17 this field is kept for backward compatibility and plans should be accessed through subscriptions instead
     */
    @Deprecated(since = "3.17.0", forRemoval = true)
    public String getPlan() {
        return plan;
    }

    @Deprecated(since = "3.17.0", forRemoval = true)
    public void setPlan(String plan) {
        this.plan = plan;
    }

    /**
     * @deprecated
     * Starting from 3.17 this field is kept for backward compatibility and apis should be accessed through subscriptions instead
     */
    @Deprecated(since = "3.17.0", forRemoval = true)
    public String getApi() {
        return api;
    }

    @Deprecated(since = "3.17.0", forRemoval = true)
    public void setApi(String api) {
        this.api = api;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKey apiKey = (ApiKey) o;
        return Objects.equals(id, apiKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isExpired() {
        return this.expireAt != null && new Date().after(this.getExpireAt());
    }

    public boolean canBeRevoked() {
        return !this.isRevoked() && !this.isExpired();
    }
}
