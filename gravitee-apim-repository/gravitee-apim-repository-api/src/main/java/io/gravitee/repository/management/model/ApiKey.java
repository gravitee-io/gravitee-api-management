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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
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
    @Setter
    @Getter
    private String id;

    /**
     * API Key
     */
    @Setter
    @Getter
    private String key;

    /**
     * The subscriptions for which the API Key is generated
     */
    @Setter
    @Getter
    private List<String> subscriptions = new ArrayList<>();

    /**
     * The ID of the environment the apiKey is attached to
     */
    @Setter
    @Getter
    private String environmentId;

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
    @Setter
    @Getter
    private String application;

    /**
     * Expiration date (end date) of the API Key
     */
    @Setter
    @Getter
    private Date expireAt;

    /**
     * API Key creation date
     */
    @Setter
    @Getter
    private Date createdAt;

    /**
     * API Key updated date
     */
    @Setter
    @Getter
    private Date updatedAt;

    /**
     * Flag to indicate if the API Key is revoked ?
     */
    @Setter
    @Getter
    private boolean revoked;

    /**
     * Flag to indicate if the API Key is paused ?
     */
    @Setter
    @Getter
    private boolean paused;

    /**
     * If the key is revoked, the revocation date
     */
    @Setter
    @Getter
    private Date revokedAt;

    /**
     * Indicates the API Key is coming from external provider.
     * <p>
     *     It should not be synchronized on the Gateway.
     * </p>
     * */
    @Getter
    @Setter
    private boolean federated;

    /**
     * Number of days before the pollInterval of this API Key when the last pre-pollInterval notification was sent
     */
    @Setter
    @Getter
    private Integer daysToExpirationOnLastNotification;

    public ApiKey(ApiKey cloned) {
        this.id = cloned.id;
        this.key = cloned.key;
        this.subscriptions = cloned.subscriptions;
        this.subscription = cloned.subscription;
        this.application = cloned.application;
        this.plan = cloned.plan;
        this.expireAt = cloned.expireAt;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
        this.revoked = cloned.revoked;
        this.revokedAt = cloned.revokedAt;
        this.paused = cloned.paused;
        this.federated = cloned.federated;
        this.daysToExpirationOnLastNotification = cloned.daysToExpirationOnLastNotification;
        this.api = cloned.api;
        this.environmentId = cloned.environmentId;
    }

    public ApiKey revoke() {
        var revokedApiKey = new ApiKey(this);
        var now = new Date();
        revokedApiKey.setRevoked(true);
        revokedApiKey.setUpdatedAt(now);
        revokedApiKey.setRevokedAt(now);
        return revokedApiKey;
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
}
