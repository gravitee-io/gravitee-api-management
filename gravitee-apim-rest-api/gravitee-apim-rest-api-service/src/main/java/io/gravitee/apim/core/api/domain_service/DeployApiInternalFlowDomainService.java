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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiDefinitionCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService.ApiDeployment;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.definition.model.v4.flow.Flow;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
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
 * <h2>What a publisher sees of it</h2>
 *
 * The republished event <strong>is an entry of the API's deployment history</strong>: that history is a plain search
 * over the events of an API, with no filter on who wrote them, and a rollback accepts any publish event. So the
 * entry shows up under a deployment number of its own, the one after the deployment it patches — a gateway ignores an
 * event of the deployment number it already runs — and can be rolled back to, which restores that deployment with
 * these Guardians, the two being what it holds. Its label says so, since an unlabelled entry would read as a
 * deployment its publisher never made. The deployment that follows takes the number after it, and the out-of-sync
 * state, which compares the API with the payload of its latest event and not with a number, is left as it was.
 *
 * <h2>One call, because the order is not a caller's business</h2>
 *
 * The definition is written, then the event is handed over — and the event only if the write happened, so the
 * gateways never run an internal flow the store does not hold. The definition and the event are not in one
 * transaction, so one hole is left open on purpose: <strong>if the event write fails after the definition was
 * written</strong>, the API names a Guardian the gateways do not run. It then reads as out of sync, which is the true
 * signal, and the publisher can deploy it. Writing the old definition back would add a second point of failure at
 * the same place for no gain.
 *
 * <h2>What the gateways are given, and what the round trip costs</h2>
 *
 * The deployment is read as a typed {@link Api}, its internal flow is replaced, and it is handed back: so the
 * gateways are given the deployment <strong>re-expressed in the current model</strong>, not a copy of their payload
 * with one field edited. Two consequences, both accepted and both to be left alone:
 *
 * <ul>
 *   <li>Inside the <strong>definition</strong>, a field written by a rest-api of another version that the current
 *       model no longer declares is dropped, with no exception and no log. Pressing deploy drops it too — the
 *       definition {@code EventServiceImpl.buildApiEventPayload} ships is re-serialized from the current model as
 *       well — so failing here would make a Guardian change stricter than a deployment, which is not the trade a
 *       publisher is asking for.
 *   <li>Around the definition the two part ways, and this is the one that loses: pressing deploy copies the stored
 *       row column by column and swaps the definition alone, whereas here whatever the mapping between the
 *       repository model and the core model does not carry leaves the payload — {@code syncFrom}, {@code mode} and
 *       {@code integrationId}. <strong>No gateway reads any of the three.</strong>
 *       What they do read of a deployment is {@code definitionVersion}, {@code type} and {@code definition}, and
 *       then {@code lifecycleState} — a lost one disables the API on every gateway — and {@code environmentId} — a
 *       lost one makes them drop the event. All five survive, and a test pins each by name rather than leaving it
 *       to a reading of the mapper.
 * </ul>
 *
 * <p>The internal flow is set on a V4 HTTP definition and on nothing else: every other shape has none to hold and is
 * republished as it is. That also answers the API whose last deployment is still a V2 one — migrated to V4 and never
 * redeployed: the payload goes back unchanged, and the API already reads out of sync since V2 is not V4.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class DeployApiInternalFlowDomainService {

    private static final String NO_DEPLOYMENT_NUMBER = "0";

    private final ApiDefinitionCrudService apiDefinitionCrudService;
    private final EventCrudService eventCrudService;
    private final EventLatestCrudService eventLatestCrudService;
    private final EventLatestQueryService eventLatestQueryService;

    /**
     * Writes the definition of an API and hands its internal flow to the gateways, or does neither.
     *
     * <p>The internal flow sent is the one the definition being written carries, and none at all for a definition
     * that cannot hold one — a V2 API, a native one: the gateways then stop running the steps they were given.
     *
     * @param api the API carrying the definition to write and the update date to stamp, moved one millisecond past
     *     {@code expectedUpdatedAt} when it is not already. Nothing else of it is written, so a concurrent write of
     *     another field is neither rolled back nor overwritten.
     * @param expectedUpdatedAt the update date the API carried when that definition was computed from it.
     * @param label shown in the API's deployment history, where the republished event is an entry of its own: without
     *     it the entry reads as a deployment its publisher never made.
     * @return whether the definition was written. {@code false} means another writer got to the API first, or that it
     *     is gone; either way nothing was written and the gateways were sent nothing.
     * @throws TechnicalDomainException when the deployment the gateways run cannot be read. Nothing has been written
     *     at that point, so the API is left as it was rather than contradicting what the gateways run.
     */
    public boolean replaceInternalFlow(AuditInfo auditInfo, Api api, ZonedDateTime expectedUpdatedAt, String label) {
        ensureRunningDeploymentReadable(auditInfo.environmentId(), api.getId());
        var stamped = api.toBuilder().updatedAt(movedPast(api.getUpdatedAt(), expectedUpdatedAt)).build();
        if (!apiDefinitionCrudService.replaceDefinition(stamped, expectedUpdatedAt)) {
            return false;
        }
        republish(auditInfo, api.getId(), api.internalFlow(), label);
        return true;
    }

    /**
     * {@link #replaceInternalFlow} for a definition that cannot hold an internal flow: the gateways stop running the
     * steps they were given, and the API indexes no Guardian.
     */
    public boolean clearInternalFlow(AuditInfo auditInfo, Api api, ZonedDateTime expectedUpdatedAt, String label) {
        return replaceInternalFlow(auditInfo, api.withInternalFlow(null), expectedUpdatedAt, label);
    }

    /**
     * Fails as {@link #replaceInternalFlow} would, before it writes anything: a caller writing other parts of the API
     * first asks this before its own write, so an unreadable deployment leaves the whole API as it was. What is
     * republished is read again after the write.
     *
     * @throws TechnicalDomainException when the deployment the gateways run cannot be read.
     */
    public void ensureRunningDeploymentReadable(String environmentId, String apiId) {
        eventLatestQueryService.findRunningApiDeployment(environmentId, apiId);
    }

    /**
     * The date to stamp, strictly after the expected one once both are cut to the millisecond the stores keep: a date
     * the compare-and-swap left unchanged would let a writer that read the API before this write pass its own and
     * overwrite it.
     */
    private static ZonedDateTime movedPast(ZonedDateTime requested, ZonedDateTime expected) {
        var earliest = expected.truncatedTo(ChronoUnit.MILLIS).plus(1, ChronoUnit.MILLIS);
        return requested != null && !requested.isBefore(earliest) ? requested : earliest;
    }

    /**
     * Hands the deployment the gateways run, with its internal flow replaced, back to them: an event, and the latest
     * one. Nothing is sent for an API that was never deployed, or whose last event stopped or unpublished it — the
     * change reaches the gateways when the API is deployed or started.
     *
     * <p>The deployment is read after the definition write, so one the publisher made meanwhile is patched rather
     * than overwritten, and an API stopped meanwhile is left stopped. The window is narrowed to this read and the
     * event write, not closed — {@code events_latest} offers no conditional write to condition this one on.
     *
     * <p>The environment is the caller's, which is the one the deployment is read from and republished into: a
     * deployment of another environment is another tenant's.
     */
    private void republish(AuditInfo auditInfo, String apiId, Flow internalFlow, String label) {
        eventLatestQueryService
            .findRunningApiDeployment(auditInfo.environmentId(), apiId)
            .ifPresent(running -> {
                var event = eventCrudService.createApiEvent(
                    auditInfo.organizationId(),
                    auditInfo.environmentId(),
                    Set.of(auditInfo.environmentId()),
                    // Republished as it is: a started API is not restarted by a new internal flow, nor a deployed one started.
                    running.type(),
                    // The shape judged is the deployment's, never the written API's: a rollback to a V2 revision writes a
                    // V2 definition while the gateways still run a V4 payload, whose internal flow has to be emptied.
                    running.api().withInternalFlow(internalFlow),
                    propertiesOf(auditInfo, apiId, running, label)
                );
                eventLatestCrudService.createOrPatchLatestEvent(auditInfo.organizationId(), apiId, event);
            });
    }

    /**
     * The deployment number is the one after the deployment patched. A gateway reads that number as the revision of
     * the API it runs and replaces what it runs only with an event of another revision, however recent: an event
     * keeping the number would be ignored until the next deployment. It is the number a deployment of the API would
     * take, the one after its latest event, which the deployment patched is; and the event becomes that latest event,
     * so the deployment that follows takes the number after this one. The user is left out for a caller that is none,
     * a technical call, as the deployment of an API does.
     */
    private static Map<Event.EventProperties, String> propertiesOf(AuditInfo auditInfo, String apiId, ApiDeployment running, String label) {
        var properties = new EnumMap<Event.EventProperties, String>(Event.EventProperties.class);
        if (auditInfo.actor() != null && auditInfo.actor().userId() != null) {
            properties.put(Event.EventProperties.USER, auditInfo.actor().userId());
        }
        properties.put(Event.EventProperties.API_ID, apiId);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER, nextDeploymentNumber(running));
        properties.put(Event.EventProperties.DEPLOYMENT_LABEL, label);
        return properties;
    }

    /** A deployment carrying no number counts as number 0, as it does when the API is deployed. */
    private static String nextDeploymentNumber(ApiDeployment running) {
        return Long.toString(Long.parseLong(Objects.requireNonNullElse(running.deploymentNumber(), NO_DEPLOYMENT_NUMBER)) + 1);
    }
}
