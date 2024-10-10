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

import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.Subscription;
import java.util.Date;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}subscriptions")
@Getter
@Setter
@EqualsAndHashCode(of = { "id" }, callSuper = false)
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
     * The environment related to this subscription
     */
    private String environmentId;

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
     * The clientCertificate linked to the subscription
     */
    private String clientCertificate;

    /**
     * PENDING, ACCEPTED, REJECTED
     */
    private String status;

    /**
     * STOPPED, STARTED
     */
    private String consumerStatus = Subscription.ConsumerStatus.STARTED.name();

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

    private Date consumerPausedAt;

    private PageRevisionPkMongo generalConditionsContentRevision;

    private Boolean generalConditionsAccepted;

    /**
     * Number of days before the pollInterval of this subscription when the last pre-pollInterval notification was sent
     */
    private Integer daysToExpirationOnLastNotification;

    private String filter;

    private String configuration;

    private Map<String, String> metadata;

    private String failureCause;

    /**
     * STANDARD, SUBSCRIPTION
     */
    private String type;
}
