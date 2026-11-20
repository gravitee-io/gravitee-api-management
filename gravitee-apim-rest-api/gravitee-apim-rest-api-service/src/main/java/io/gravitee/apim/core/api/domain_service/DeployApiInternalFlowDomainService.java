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
package io.gravitee.apim.core.api.domain_service;

import static java.util.Map.entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.model.EventType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Sends a new internal flow of an API to the gateways, and nothing else of it.
 *
 * <p>The internal flow is where the Guardians an API runs live, and a Guardian takes effect when it is turned on
 * rather than at the next deployment of the API. So this republishes what the gateways were last given with the one
 * field replaced: every plan, flow and property the publisher saved without deploying stays out, exactly as a
 * publisher who did not press deploy expects.
 *
 * <p>Hence the two writes done here instead of through {@code EventService.createApiEvent}: that one rebuilds the
 * definition it ships by reading the plans and the flows of the API from the store, which would deploy whatever is
 * saved there. The API products take the same route for the same reason
 * ({@code DeployApiProductDomainService.deployApiList}).
 *
 * <p>No audit entry and no notification of its own: the caller audits the change it made — one change, one entry —
 * and a deployment notification would announce a deployment to the subscribers of an API whose publisher did not ask
 * for one. The API products do neither either.
 *
 * <h2>The order a caller must keep</h2>
 *
 * {@link #prepare} first, then the write of the definition, then {@link #deploy}: preparing is what fails when the
 * payload cannot be read, and it fails before anything has been written. The definition and the event are not in one
 * transaction, so one hole is left open on purpose: <strong>if the event write fails after the definition was
 * written</strong>, the API names a Guardian the gateways do not run. It then reads as out of sync, which is the true
 * signal, and the publisher can deploy it. Writing the old definition back would add a second point of failure at
 * the same place for no gain.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class DeployApiInternalFlowDomainService {

    private static final String DEFINITION = "definition";
    private static final String INTERNAL_FLOW = "internalFlow";
    private static final String NO_DEPLOYMENT_NUMBER = "0";

    /**
     * The event types that mean "the gateways run this API". {@code events_latest} holds exactly one row per API, so
     * the API is deployed if and only if that row is one of these: a stopped or unpublished API has a row of another
     * type and answers none of the queries below.
     */
    private static final Set<EventType> DEPLOYED = Set.of(EventType.PUBLISH_API, EventType.START_API);

    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final EventLatestQueryService eventLatestQueryService;
    private final ObjectMapper objectMapper;

    /**
     * Patches the internal flow into the deployment the gateways are running, without writing anything.
     *
     * @return empty when nothing is to be sent: an API that was never deployed, or one whose last event stopped or
     *     unpublished it. Both leave the API out of sync, which is the honest answer — the change reaches the
     *     gateways when its publisher deploys the API, and until then nothing runs it.
     * @throws TechnicalDomainException when the deployment cannot be read. Nothing has been written at this point, so
     *     the caller fails without leaving a definition the gateways contradict.
     */
    public Optional<PatchedDeployment> prepare(String apiId, Flow internalFlow) {
        return lastDeployment(apiId).map(deployed ->
            new PatchedDeployment(
                apiId,
                deployed.getType(),
                patch(apiId, deployed.getPayload(), internalFlow),
                deployed.getProperties().getOrDefault(Event.EventProperties.DEPLOYMENT_NUMBER, NO_DEPLOYMENT_NUMBER)
            )
        );
    }

    /** Hands the patched deployment to the gateways, as the deployment of an API does: an event, and the latest one. */
    public void deploy(AuditInfo auditInfo, PatchedDeployment deployment) {
        var event = eventCrudService.createEvent(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            Set.of(auditInfo.environmentId()),
            deployment.type(),
            deployment.payload(),
            Map.ofEntries(
                entry(Event.EventProperties.USER, auditInfo.actor().userId()),
                entry(Event.EventProperties.API_ID, deployment.apiId()),
                entry(Event.EventProperties.DEPLOYMENT_NUMBER, deployment.deploymentNumber())
            )
        );
        eventLatestCrudService.createOrPatchLatestEvent(auditInfo.organizationId(), deployment.apiId(), event);
    }

    private Optional<Event> lastDeployment(String apiId) {
        return DEPLOYED.stream()
            .map(type -> eventLatestQueryService.findLatestByEntityIds(Set.of(apiId), type, Event.EventProperties.API_ID))
            .flatMap(List::stream)
            .filter(event -> event.getPayload() != null && !event.getPayload().isBlank())
            .findFirst();
    }

    /**
     * The payload is a repository API whose {@code definition} is a JSON string, so the patch is nested: the string
     * is read, its internal flow replaced, and the string written back. Everything else of the payload is copied as
     * it is — that is the point of patching rather than rebuilding.
     */
    private ObjectNode patch(String apiId, String payload, Flow internalFlow) {
        try {
            var deployed = asObject(objectMapper.readTree(payload), apiId);
            var definitionField = deployed.get(DEFINITION);
            if (definitionField == null || !definitionField.isTextual()) {
                throw new TechnicalDomainException("The deployment of api " + apiId + " carries no definition to patch");
            }
            var definition = asObject(objectMapper.readTree(definitionField.asText()), apiId);
            if (internalFlow == null) {
                definition.remove(INTERNAL_FLOW);
            } else {
                definition.set(INTERNAL_FLOW, objectMapper.valueToTree(internalFlow));
            }
            return deployed.set(DEFINITION, TextNode.valueOf(objectMapper.writeValueAsString(definition)));
        } catch (JsonProcessingException e) {
            throw new TechnicalDomainException("Unable to read what the gateways run for api " + apiId, e);
        }
    }

    private static ObjectNode asObject(JsonNode node, String apiId) {
        if (node instanceof ObjectNode object) {
            return object;
        }
        throw new TechnicalDomainException("Unable to read what the gateways run for api " + apiId);
    }

    /**
     * A deployment of an API, ready to be handed to the gateways.
     *
     * @param type the type of the event that deployed the API, republished as it is: a started API is not restarted
     *     by a deployment of its new internal flow, and a deployed one is not started by it.
     * @param deploymentNumber the number the deployment being patched carried, kept so the republished event belongs
     *     to the same deployment rather than opening a new one.
     */
    public record PatchedDeployment(String apiId, EventType type, ObjectNode payload, String deploymentNumber) {}
}
