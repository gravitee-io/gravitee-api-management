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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.core.json.JsonSerializer;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Reconciles Guardian drafts with their latest publication: deploying, withdrawing, and the lifecycle state a draft
 * shows against what the gateways run.
 */
@DomainService
@RequiredArgsConstructor
public class GuardianDeploymentDomainService {

    private final GuardianRepository guardianRepository;
    private final GuardianPublicationDomainService publications;
    private final JsonSerializer jsonSerializer;
    private final JsonDeserializer jsonDeserializer;

    public GuardianDeployment current(Guardian guardian) {
        return publications.findAll(guardian.getEnvironmentId(), Set.of(guardian.getId())).get(guardian.getId());
    }

    /**
     * The Guardian as its latest publication shows it. The lifecycle state and the deployment date come from the
     * publication alone, never from the stored Guardian, which no deployment writes; the update date is advanced to
     * the publication, which is also what the store does once the deployment completes.
     */
    public Guardian state(Guardian guardian, GuardianDeployment publication) {
        return switch (publication) {
            case null -> guardian.toBuilder().lifecycleState(GuardianLifecycleState.UNDEPLOYED).deployedAt(null).build();
            case GuardianDeployment.Published published -> advancedTo(guardian, published)
                .lifecycleState(matches(guardian, published) ? GuardianLifecycleState.DEPLOYED : GuardianLifecycleState.PENDING)
                .deployedAt(published.at())
                .build();
            case GuardianDeployment.Withdrawn withdrawn -> advancedTo(guardian, withdrawn)
                .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
                .deployedAt(withdrawn.deployedAt())
                .build();
        };
    }

    private static Guardian.GuardianBuilder advancedTo(Guardian guardian, GuardianDeployment deployment) {
        var builder = guardian.toBuilder();
        if (guardian.getUpdatedAt() == null || guardian.getUpdatedAt().isBefore(deployment.at())) {
            builder.updatedAt(deployment.at());
        }
        return builder;
    }

    public Guardian state(Guardian guardian) {
        return state(guardian, current(guardian));
    }

    public List<Guardian> states(String environmentId, List<Guardian> guardians) {
        var latest = publications.findAll(environmentId, guardians.stream().map(Guardian::getId).collect(Collectors.toSet()));
        return guardians
            .stream()
            .map(g -> state(g, latest.get(g.getId())))
            .toList();
    }

    public GuardianDeployment.Published publish(Guardian guardian, String label, AuditInfo auditInfo) {
        return publications.publish(guardian.getDefinition(), label, auditInfo);
    }

    public GuardianDeployment.Withdrawn withdraw(Guardian guardian, GuardianDeployment.Published ended, AuditInfo auditInfo) {
        return publications.withdraw(guardian.getId(), ended, auditInfo);
    }

    /**
     * Advances the draft's sorting timestamp to the deployment. Only the timestamp: writing the draft read before
     * deploying would replace one saved meanwhile.
     */
    public void complete(Guardian guardian, GuardianDeployment deployment) {
        guardianRepository.updateUpdatedAt(guardian.getEnvironmentId(), guardian.getId(), deployment.at());
    }

    private boolean matches(Guardian guardian, GuardianDeployment.Published publication) {
        var draft = guardian.getDefinition();
        var published = publication.definition();
        return Objects.equals(draft.getApiTypes(), published.getApiTypes()) && content(draft).equals(content(published));
    }

    private JsonNode content(Object definition) {
        try {
            var tree = jsonDeserializer.deserialize(jsonSerializer.serialize(definition), ObjectNode.class);
            tree.remove("apiTypes");
            return tree;
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Failed to compare Guardian definitions", e);
        }
    }
}
