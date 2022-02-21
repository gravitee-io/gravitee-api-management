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

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mongo model for Api Key
 *
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "keys")
public class ApiKeyMongo {

    /**
     * Api Key's unique id
     */
    @Id
    private String id;

    /**
     * Api Key
     */
    private String key;

    /**
     * The subscriptions for which the Api Key is generated
     *
     * The collection should contain more than one element only if the subscriptions are made
     * for an application where the SHARED mode has been configured for API Key subscriptions
     *
     * @see io.gravitee.repository.management.model.ApiKeyMode
     */
    private Set<String> subscriptions = new HashSet<>();

    /**
     * The subscription for which the Api Key is generated
     *
     */
    private String subscription;

    /**
     * The subscribed plan
     *
     */
    private String plan;

    /**
     * The api on which this api key is used
     *
     */
    private String api;

    /**
     * The application used to make the subscription
     */
    private String application;

    /**
     * Expiration date (end date) of the Api Key
     */
    private Date expireAt;

    /**
     * API key creation date
     */
    private Date createdAt;

    /**
     * API key updated date
     */
    private Date updatedAt;

    /**
     * Flag to indicate if the Api Key is revoked ?
     */
    private boolean revoked;

    /**
     * Flag to indicate if the Api Key is paused ?
     */
    private boolean paused;

    /**
     * If the key is revoked, the revocation date
     */
    private Date revokedAt;

    /**
     * Number of days before the expiration of this api key when the last pre-expiration notification was sent
     */
    private Integer daysToExpirationOnLastNotification;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Set<String> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Set<String> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Date getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Date expireAt) {
        this.expireAt = expireAt;
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

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Date getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Date revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public Integer getDaysToExpirationOnLastNotification() {
        return daysToExpirationOnLastNotification;
    }

    public void setDaysToExpirationOnLastNotification(Integer daysToExpirationOnLastNotification) {
        this.daysToExpirationOnLastNotification = daysToExpirationOnLastNotification;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @deprecated
     * Starting from 3.17 this field is kept for backward compatibility only and subscriptions should be used instead
     */
    @Deprecated
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
        ApiKeyMongo key = (ApiKeyMongo) o;
        return Objects.equals(this.id, key.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiKey{");
        sb.append("name='").append(key).append('\'');
        sb.append(", expiration=").append(expireAt).append('\'');
        sb.append(", revoked=").append(revoked).append('\'');
        sb.append(", revokedAt=").append(revokedAt);
        sb.append('}');
        return sb.toString();
    }
}
