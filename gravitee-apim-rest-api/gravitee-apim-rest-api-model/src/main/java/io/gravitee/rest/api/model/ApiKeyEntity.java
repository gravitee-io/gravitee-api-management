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

import static java.util.stream.Collectors.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
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
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class ApiKeyEntity {

    @ToString.Include
    private String id;

    private String key;

    @Builder.Default
    private Set<SubscriptionEntity> subscriptions = new HashSet<>();

    @ToString.Include
    private String environmentId;

    private ApplicationEntity application;

    @ToString.Include
    @JsonProperty("expire_at")
    private Date expireAt;

    @ToString.Include
    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    private boolean revoked;

    @JsonProperty("revoked_at")
    private Date revokedAt;

    private boolean paused;

    private boolean expired;

    /**
     * Indicates the API Key is coming from external provider.
     * <p>
     *     It should not be synchronized on the Gateway.
     * </p>
     * */
    private boolean federated;

    /**
     * Number of days before the expiration of this API Key when the last pre-expiration notification was sent
     */
    private Integer daysToExpirationOnLastNotification;

    @JsonIgnore
    public List<String> getSubscriptionIds() {
        return subscriptions.stream().map(SubscriptionEntity::getId).collect(toList());
    }

    public boolean hasSubscription(String subscriptionId) {
        return getSubscriptionIds().stream().anyMatch(subscriptionId::equals);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKeyEntity that = (ApiKeyEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
