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
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class SubscriptionEntity {

    private String id;

    /**
     * @deprecated since 4.11.0. Use {@link #referenceId} and {@link #referenceType} instead.
     */
    @Deprecated(since = "4.11.0", forRemoval = true)
    private String api;

    private String environmentId;

    private String plan;

    private String application;

    private String applicationName;

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

    private String origin = OriginContext.Origin.MANAGEMENT.name();

    /**
     * Number of days before the expiration of this subscription when the last pre-expiration notification was sent
     */
    private Integer daysToExpirationOnLastNotification;

    private SubscriptionConfigurationEntity configuration;

    private Map<String, String> metadata;

    private String failureCause;

    /**
     * Reference id (API id or API Product id). When set, takes precedence over {@link #api} for subscription context.
     */
    private String referenceId;

    /**
     * Reference type: "API" or "API_PRODUCT". Defaults to API when null.
     */
    private String referenceType;

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
