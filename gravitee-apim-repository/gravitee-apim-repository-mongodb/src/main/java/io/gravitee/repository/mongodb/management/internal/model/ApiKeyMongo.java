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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mongo model for API Key
 *
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}keys")
public class ApiKeyMongo {

    /**
     * API Key's unique id
     */
    @Setter
    @Getter
    @Id
    @EqualsAndHashCode.Include
    private String id;

    /**
     * API Key
     */
    @Setter
    @Getter
    private String key;

    /**
     * The subscriptions for which the API Key is generated
     *
     * The collection should contain more than one element only if the subscriptions are made
     * for an application where the SHARED mode has been configured for API Key subscriptions
     *
     * @see io.gravitee.repository.management.model.ApiKeyMode
     */
    @Setter
    @Getter
    private Set<String> subscriptions = new HashSet<>();

    /**
     * API Key's environment id
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
     */
    @Setter
    @Getter
    private boolean federated;

    /**
     * Number of days before the expiration of this API Key when the last pre-expiration notification was sent
     */
    @Setter
    @Getter
    private Integer daysToExpirationOnLastNotification;

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
}
