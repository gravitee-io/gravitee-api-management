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
package io.gravitee.apim.infra.adapter;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.json.JsonSerializer;
import io.gravitee.apim.core.subscription.model.SubscriptionConfiguration;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.SubscriptionStatus;
import lombok.CustomLog;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    uses = { JsonDeserializer.class, JsonDeserializer.class },
    injectionStrategy = InjectionStrategy.FIELD,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL
)
@CustomLog
public abstract class SubscriptionAdapter {

    protected JsonDeserializer jsonDeserializer;

    protected JsonSerializer jsonSerializer;

    @Autowired
    public SubscriptionAdapter setJsonDeserializer(JsonDeserializer jsonDeserializer) {
        this.jsonDeserializer = jsonDeserializer;
        return this;
    }

    @Autowired
    public SubscriptionAdapter setJsonSerializer(JsonSerializer jsonSerializer) {
        this.jsonSerializer = jsonSerializer;
        return this;
    }

    @Mapping(source = "api", target = "apiId")
    @Mapping(source = "plan", target = "planId")
    @Mapping(source = "application", target = "applicationId")
    @Mapping(source = "request", target = "requestMessage")
    @Mapping(source = "reason", target = "reasonMessage")
    @Mapping(target = "configuration", expression = "java(deserializeConfiguration(subscription.getConfiguration()))")
    @Mapping(target = "referenceType", source = "referenceType", qualifiedByName = "mapReferenceTypeFromRepository")
    public abstract SubscriptionEntity toEntity(Subscription subscription);

    @Mapping(source = "apiId", target = "api")
    @Mapping(source = "planId", target = "plan")
    @Mapping(source = "applicationId", target = "application")
    @Mapping(source = "requestMessage", target = "request")
    @Mapping(source = "reasonMessage", target = "reason")
    @Mapping(target = "configuration", expression = "java(serializeConfiguration(subscription.getConfiguration()))")
    @Mapping(target = "referenceType", source = "referenceType", qualifiedByName = "mapReferenceTypeToRepository")
    public abstract Subscription fromEntity(SubscriptionEntity subscription);

    @Mapping(source = "apiId", target = "api")
    @Mapping(source = "referenceType", target = "referenceType", qualifiedByName = "coreReferenceTypeToRest")
    @Mapping(source = "planId", target = "plan")
    @Mapping(source = "applicationId", target = "application")
    @Mapping(source = "requestMessage", target = "request")
    @Mapping(source = "reasonMessage", target = "reason")
    @Mapping(target = "security", ignore = true)
    @Mapping(target = "keys", ignore = true)
    public abstract io.gravitee.rest.api.model.SubscriptionEntity map(SubscriptionEntity subscription);

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "referenceType", source = "referenceType", qualifiedByName = "restReferenceTypeToCore")
    @Mapping(source = "plan", target = "planId")
    @Mapping(source = "application", target = "applicationId")
    @Mapping(source = "request", target = "requestMessage")
    @Mapping(source = "reason", target = "reasonMessage")
    @Mapping(target = "generalConditionsContentRevision", ignore = true)
    @Mapping(target = "generalConditionsContentPageId", ignore = true)
    @Mapping(target = "generalConditionsAccepted", ignore = true)
    public abstract SubscriptionEntity toCore(io.gravitee.rest.api.model.SubscriptionEntity subscription);

    public SubscriptionEntity.Status toCore(SubscriptionStatus status) {
        return status == SubscriptionStatus.RESUMED ? SubscriptionEntity.Status.ACCEPTED : SubscriptionEntity.Status.valueOf(status.name());
    }

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startingAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "subscribedBy", ignore = true)
    @Mapping(target = "requestMessage", ignore = true)
    @Mapping(target = "reasonMessage", ignore = true)
    @Mapping(target = "processedBy", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "pausedAt", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "generalConditionsContentRevision", ignore = true)
    @Mapping(target = "generalConditionsContentPageId", ignore = true)
    @Mapping(target = "generalConditionsAccepted", ignore = true)
    @Mapping(target = "failureCause", ignore = true)
    @Mapping(target = "environmentId", ignore = true)
    @Mapping(target = "daysToExpirationOnLastNotification", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "consumerStatus", ignore = true)
    @Mapping(target = "consumerPausedAt", ignore = true)
    @Mapping(target = "configuration", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "clientCertificate", ignore = true)
    @Mapping(target = "origin", constant = "KUBERNETES")
    public abstract SubscriptionEntity fromSpec(SubscriptionCRDSpec spec);

    @Mapping(target = "request", ignore = true)
    @Mapping(target = "apiKeyMode", ignore = true)
    @Mapping(source = "applicationId", target = "application")
    @Mapping(source = "planId", target = "plan")
    @Mapping(target = "generalConditionsContentRevision", ignore = true)
    public abstract io.gravitee.rest.api.model.NewSubscriptionEntity fromCoreForCreate(SubscriptionEntity entity);

    public abstract io.gravitee.rest.api.model.UpdateSubscriptionEntity fromCoreForUpdate(SubscriptionEntity entity);

    @Named("deserializeConfiguration")
    public SubscriptionConfiguration deserializeConfiguration(String configuration) {
        if (configuration == null) {
            return null;
        }

        try {
            return jsonDeserializer.deserialize(configuration, JacksonSubscriptionConfiguration.class);
        } catch (JsonProcessingException e) {
            log.error("Unexpected error while deserializing Subscription configuration", e);
            return null;
        }
    }

    @Named("mapReferenceTypeFromRepository")
    protected SubscriptionReferenceType mapReferenceTypeFromRepository(
        io.gravitee.repository.management.model.SubscriptionReferenceType repoType
    ) {
        if (repoType == null) {
            return null;
        }
        return SubscriptionReferenceType.valueOf(repoType.name());
    }

    @Named("mapReferenceTypeToRepository")
    protected io.gravitee.repository.management.model.SubscriptionReferenceType mapReferenceTypeToRepository(
        SubscriptionReferenceType coreType
    ) {
        if (coreType == null) {
            return null;
        }
        return io.gravitee.repository.management.model.SubscriptionReferenceType.valueOf(coreType.name());
    }

    @Named("coreReferenceTypeToRest")
    protected String referenceTypeToRestString(SubscriptionReferenceType referenceType) {
        return referenceType == null ? null : referenceType.name();
    }

    @Named("restReferenceTypeToCore")
    protected SubscriptionReferenceType restReferenceTypeToCore(String referenceType) {
        if (referenceType == null) {
            return SubscriptionReferenceType.API;
        }
        return SubscriptionReferenceType.valueOf(referenceType);
    }

    @Named("serializeConfiguration")
    public String serializeConfiguration(SubscriptionConfiguration configuration) {
        if (configuration == null) {
            return null;
        }

        try {
            return jsonSerializer.serialize(new JacksonSubscriptionConfiguration(configuration));
        } catch (JsonProcessingException e) {
            log.error("Unexpected error while serializing Subscription configuration", e);
            return null;
        }
    }

    /**
     * This class is used to deserialize the SubscriptionConfiguration.
     * <p>
     *     It's a "hack" to support the deserialization of the `entrypointConfiguration` of SubscriptionConfiguration.
     *     This attribute is a String in the POJO but in the database it's a JSON string which is "deserialize" as
     *     JsonNode.
     * </p>
     */
    public static class JacksonSubscriptionConfiguration extends SubscriptionConfiguration {

        // required for Jackson deserialization
        public JacksonSubscriptionConfiguration() {}

        public JacksonSubscriptionConfiguration(SubscriptionConfiguration configuration) {
            super(configuration.getEntrypointId(), configuration.getChannel(), configuration.getEntrypointConfiguration());
        }

        @JsonRawValue
        public String getEntrypointConfiguration() {
            return super.getEntrypointConfiguration();
        }

        @JsonSetter
        public void setEntrypointConfiguration(final JsonNode configuration) {
            if (configuration != null && !configuration.isNull()) {
                this.setEntrypointConfiguration(configuration.toString());
            }
        }
    }
}
