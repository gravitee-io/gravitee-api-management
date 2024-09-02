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
package io.gravitee.apim.core.subscription.model;

import io.gravitee.common.utils.TimeProvider;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SubscriptionEntity {

    /** Subscription ID. */
    private String id;
    /** The subscribed API. */
    private String apiId;
    /** The environmentId related to this subscription. */
    private String environmentId;
    /** The subscribed Plan. */
    private String planId;
    /** The application linked to the subscription */
    private String applicationId;
    /** The clientId linked to the subscription */
    private String clientId;
    /** The clientCertificate linked to the subscription */
    private String clientCertificate;
    /** When the subscription has been processed. */
    private ZonedDateTime processedAt;
    /** Give a request message to the api owner why a user wants to subscribe */
    private String requestMessage;
    /** Give a reason to the developer if the subscription is accepted or not. */
    private String reasonMessage;
    /** The status of the current subscription */
    private Status status;

    /** The status chosen by the consumer */
    @Builder.Default
    private ConsumerStatus consumerStatus = ConsumerStatus.STARTED;

    /**
     * The username of the user who has processed the subscription
     * <code>null</code> if the subscription is relative to an automatic plan.
     */
    private String processedBy;
    /** The username of the user who has subscribed to the plan. */
    private String subscribedBy;
    private ZonedDateTime startingAt;
    private ZonedDateTime endingAt;
    /** Subscription creation date */
    private ZonedDateTime createdAt;
    /** Subscription last update date */
    private ZonedDateTime updatedAt;
    private ZonedDateTime closedAt;
    private ZonedDateTime pausedAt;
    private ZonedDateTime consumerPausedAt;

    private Integer generalConditionsContentRevision;
    private String generalConditionsContentPageId;
    private Boolean generalConditionsAccepted;
    /** Number of days before the expiration of this subscription when the last pre-expiration notification was sent */
    private Integer daysToExpirationOnLastNotification;
    private SubscriptionConfiguration configuration;
    private Map<String, String> metadata;

    @Builder.Default
    private Type type = Type.STANDARD;

    /** The failure cause when the PUSH subscription has failed. */
    private String failureCause;

    /**
     * Consumer can start subscription only if its consumer status is {@link this#consumerStatus} is {@link ConsumerStatus#STOPPED} or {@link ConsumerStatus#STARTED}
     * @return true when consumer can start the subscription
     */
    public boolean canBeStartedByConsumer() {
        return ConsumerStatus.STOPPED.equals(consumerStatus) || ConsumerStatus.FAILURE.equals(consumerStatus);
    }

    /**
     * Consumer can stop subscription only if its consumer status is {@link this#consumerStatus} is {@link ConsumerStatus#STARTED} or {@link ConsumerStatus#STARTED}
     * @return true when consumer can stop the subscription
     */
    public boolean canBeStoppedByConsumer() {
        return ConsumerStatus.STARTED.equals(consumerStatus) || ConsumerStatus.FAILURE.equals(consumerStatus);
    }

    public SubscriptionEntity close() {
        return switch (this.status) {
            case ACCEPTED, PAUSED -> {
                final ZonedDateTime now = ZonedDateTime.now();
                yield this.toBuilder().updatedAt(now).closedAt(now).status(Status.CLOSED).build();
            }
            case REJECTED, CLOSED -> this;
            case PENDING -> throw new IllegalStateException("Pending subscription is not closable");
        };
    }

    public SubscriptionEntity rejectBy(String userId, String reason) {
        if (Status.PENDING.equals(this.status)) {
            final ZonedDateTime now = TimeProvider.now();
            return this.toBuilder()
                .processedBy(userId)
                .processedAt(now)
                .updatedAt(now)
                .closedAt(now)
                .status(Status.REJECTED)
                .reasonMessage(reason)
                .build();
        }
        throw new IllegalStateException("Cannot reject subscription");
    }

    public SubscriptionEntity acceptBy(String userId, ZonedDateTime startingAt, ZonedDateTime endingAt, String reasonMessage) {
        return acceptBy(userId, startingAt, endingAt, reasonMessage, null);
    }

    public SubscriptionEntity acceptBy(
        String userId,
        ZonedDateTime startingAt,
        ZonedDateTime endingAt,
        String reasonMessage,
        Map<String, String> metadata
    ) {
        return switch (this.status) {
            case PENDING -> {
                final ZonedDateTime now = TimeProvider.now();
                yield this.toBuilder()
                    .processedBy(userId)
                    .updatedAt(now)
                    .startingAt(startingAt != null ? startingAt : now)
                    .endingAt(endingAt)
                    .processedAt(now)
                    .status(Status.ACCEPTED)
                    .reasonMessage(reasonMessage)
                    .metadata(metadata)
                    .build();
            }
            case ACCEPTED -> this;
            default -> throw new IllegalStateException("Cannot accept subscription");
        };
    }

    public boolean isAccepted() {
        return this.status == Status.ACCEPTED;
    }

    public boolean isPaused() {
        return this.status == Status.PAUSED;
    }

    public boolean isPending() {
        return this.status == Status.PENDING;
    }

    public boolean isRejected() {
        return this.status == Status.REJECTED;
    }

    public boolean isClosed() {
        return this.status == Status.CLOSED;
    }

    public enum Status {
        /** Waiting for validation */
        PENDING,

        /** Subscription has been rejected */
        REJECTED,

        /** Subscription has been accepted */
        ACCEPTED,

        /** Subscription has been closed */
        CLOSED,

        /** Subscription has been paused */
        PAUSED,
    }

    public enum ConsumerStatus {
        /** Subscription is started */
        STARTED,
        /** Subscription has been paused */
        STOPPED,
        /** Subscription has encountered a failure */
        FAILURE,
    }

    public enum Type {
        /** <code>STANDARD</code> subscriptions are subscription used to manage calls in proxy mode (request/response) */
        STANDARD,

        /**
         * <code>PUSH</code> type of subscription are subscription used to manage push calls
         * (the call from the gateway is initiated by the incoming subscription, a typical use-case is webhook).
         */
        PUSH,
    }
}
