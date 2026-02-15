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
package io.gravitee.rest.api.management.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionConsumerStatus;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Setter
@Getter
public class Subscription {

    private String id;

    private Api api;

    private ApiProduct apiProduct;

    private String referenceId;

    private String referenceType;

    private Plan plan;

    private Application application;

    private SubscriptionStatus status;

    @JsonProperty("consumerStatus")
    private SubscriptionConsumerStatus consumerStatus;

    @JsonProperty("processed_at")
    private Date processedAt;

    @JsonProperty("processed_by")
    private String processedBy;

    @JsonProperty("subscribed_by")
    private User subscribedBy;

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

    private Map<String, String> metadata;

    private SubscriptionConfigurationEntity configuration;

    @JsonProperty("failureCause")
    private String failureCause;

    private OriginContext.Origin origin;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subscription that = (Subscription) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class Plan {

        private final String id;
        private final String name;
        private String security;

        public Plan(final String id, final String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSecurity() {
            return security;
        }

        public void setSecurity(String security) {
            this.security = security;
        }
    }

    public static class Application {

        private final String id;
        private final String name;
        private final String type;
        private final String description;
        private final String domain;
        private final User owner;
        private final ApiKeyMode apiKeyMode;

        public Application(
            final String id,
            final String name,
            final String type,
            final String description,
            final String domain,
            final User owner,
            final ApiKeyMode apiKeyMode
        ) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.description = description;
            this.domain = domain;
            this.owner = owner;
            this.apiKeyMode = apiKeyMode;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getDomain() {
            return domain;
        }

        public User getOwner() {
            return owner;
        }

        public ApiKeyMode getApiKeyMode() {
            return apiKeyMode;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Api {

        private final String id;
        private final String name;
        private final String version;
        private final DefinitionVersion definitionVersion;
        private final User owner;
    }

    @AllArgsConstructor
    @Getter
    public static class ApiProduct {

        private final String id;
        private final String name;
        private final String version;
        private final User owner;
    }

    public static class User {

        private final String id;
        private final String displayName;

        public User(final String id, final String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
