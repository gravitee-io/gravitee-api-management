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
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

    /** Absent and empty mean the same: the UI saves empty lists, a revision published earlier may have none. */
    private static final List<String> LISTS = List.of("policies", "resources");

    /** Shown in the deployment history, where the restored revision would otherwise read as a deployment nobody made. */
    private static final String RESTORED_LABEL = "Restored: an API runs it";

    private final GuardianRepository guardianRepository;
    private final GuardianPublicationDomainService publications;
    private final JsonSerializer jsonSerializer;
    private final JsonDeserializer jsonDeserializer;

    public GuardianDeployment current(Guardian guardian) {
        return current(guardian.getEnvironmentId(), Set.of(guardian.getId())).get(guardian.getId());
    }

    /** The latest publication or withdrawal of each Guardian, in one read, missing from the map when it has none. */
    public Map<String, GuardianDeployment> current(String environmentId, Set<String> guardianIds) {
        return publications.findAll(environmentId, guardianIds);
    }

    /**
     * The Guardian as its latest publication shows it. The lifecycle state, the deployment date and revision come from the
     * publication alone, never from the stored Guardian, which no deployment writes; the update date is advanced to
     * the publication, which is also what the store does once the deployment completes.
     */
    public Guardian state(Guardian guardian, GuardianDeployment publication) {
        return switch (publication) {
            case null -> guardian
                .toBuilder()
                .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
                .deployedAt(null)
                .deployedRevisionId(null)
                .build();
            case GuardianDeployment.Published published -> guardian
                .toBuilder()
                .updatedAt(advancedTo(guardian, published))
                .lifecycleState(matches(guardian, published) ? GuardianLifecycleState.DEPLOYED : GuardianLifecycleState.PENDING)
                .deployedAt(published.at())
                .deployedRevisionId(published.revisionId())
                .build();
            case GuardianDeployment.Withdrawn withdrawn -> guardian
                .toBuilder()
                .updatedAt(advancedTo(guardian, withdrawn))
                .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
                .deployedAt(withdrawn.deployedAt())
                .deployedRevisionId(null)
                .build();
        };
    }

    private static Instant advancedTo(Guardian guardian, GuardianDeployment deployment) {
        if (guardian.getUpdatedAt() == null || guardian.getUpdatedAt().isBefore(deployment.at())) {
            return deployment.at();
        }
        return guardian.getUpdatedAt();
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

    /**
     * Publishes the definition as it stands, without judging it.
     *
     * <p>Whether the Guardian is worth publishing is the caller's to decide, and one rule has to be
     * decided before calling: a revision without an enabled guardrail policy enforces nothing, and it would
     * replace the one the gateways run. {@code DeployGuardianUseCase} refuses that case; any future
     * publishing path owes the same check.
     */
    public GuardianDeployment.Published publish(Guardian guardian, String label, AuditInfo auditInfo) {
        return publications.publish(guardian.getDefinition(), label, auditInfo);
    }

    /** Publishes again the revision a change replaced, which the change found an API running once written. */
    public GuardianDeployment.Published restore(GuardianDeployment.Published replaced, AuditInfo auditInfo) {
        return publications.publish(replaced.definition(), RESTORED_LABEL, auditInfo);
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
        return orEmpty(draft.getApiTypes()).equals(orEmpty(published.getApiTypes())) && content(draft).equals(content(published));
    }

    private static Set<ApiType> orEmpty(Set<ApiType> apiTypes) {
        return Objects.requireNonNullElse(apiTypes, Set.of());
    }

    private JsonNode content(Object definition) {
        try {
            var tree = jsonDeserializer.deserialize(jsonSerializer.serialize(definition), ObjectNode.class);
            tree.remove("apiTypes");
            LISTS.stream()
                .filter(list -> !tree.hasNonNull(list))
                .forEach(tree::putArray);
            return tree;
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Failed to compare Guardian definitions", e);
        }
    }
}
