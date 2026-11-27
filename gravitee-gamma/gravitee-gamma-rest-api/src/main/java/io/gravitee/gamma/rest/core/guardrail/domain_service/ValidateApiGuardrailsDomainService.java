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
package io.gravitee.gamma.rest.core.guardrail.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiGuardrailSequenceRefusedException;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiGuardrailSequenceRefusedException.Refusal;
import io.gravitee.gamma.rest.core.guardrail.exception.DuplicateApiGuardrailException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailIncompatibleApiTypeException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotDeployedException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailPhaseMismatchException;
import io.gravitee.gamma.rest.core.guardrail.exception.InvalidApiGuardrailConditionException;
import io.gravitee.gamma.rest.core.guardrail.exception.TooManyApiGuardrailsException;
import io.gravitee.gamma.rest.core.guardrail.model.ApiGuardrailAttachment;
import io.gravitee.gamma.rest.core.guardrail.model.ApiGuardrails.GuardrailRef;
import io.gravitee.gamma.rest.core.guardrail.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.ConditionSyntaxPort;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * The Guardrails an API is allowed to run, phase by phase: the management-side half of the fail-closed check the
 * gateway runs on the Guardrail it loads.
 *
 * <p>Everything is read from the revision the gateways run, not from the draft: a draft a publisher has not deployed
 * yet must decide neither where a step goes nor whether the API may run it. A Guardrail is therefore refused unless
 * its deployed revision supports the API's type and runs in the phase it is listed on — and, when it is listed under a
 * condition, unless the gateway can evaluate that condition.
 *
 * <p>The whole sequence is judged before anything is refused, so a publisher who got three Guardrails wrong learns
 * all three from one answer. How that answer is shaped, and why:
 *
 * <ul>
 *   <li>every fault is "the environment holds no such Guardrail" — {@link GuardrailNotFoundException}, a
 *       {@code NotFoundDomainException}, naming all of them;</li>
 *   <li>exactly one fault — that fault's own exception, unchanged, because a list of one is worse prose than the
 *       sentence itself;</li>
 *   <li>anything else — {@link ApiGuardrailSequenceRefusedException}, a {@code ValidationDomainException} whose
 *       parameters keep the natures apart.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateApiGuardrailsDomainService {

    /**
     * How many Guardrails an API may run on one phase. The length of the list comes from a client payload, and both
     * the validation and the store read it whole, so something has to bound it. A sequence this long is already past
     * what a request's latency budget can carry.
     */
    private static final int MAX_GUARDRAILS_PER_PHASE = 20;

    private final GuardrailRepository guardrailRepository;
    private final GuardrailDeploymentDomainService deployments;
    private final ConditionSyntaxPort conditionSyntax;

    /**
     * The Guardrails of every phase of one request and the conditions they run under, judged together so that one
     * answer carries every fault.
     *
     * @param attachmentsByPhase the sequence of each phase, in execution order. A phase the map leaves out is not
     *     judged and comes back absent; a phase mapped to an empty list runs no Guardrail.
     * @return the Guardrails to write, each with its condition.
     * @throws TooManyApiGuardrailsException when a phase lists more Guardrails than an API may run, which is checked
     *     before anything is read.
     * @throws GuardrailNotFoundException when every fault of the request is a Guardrail the environment does not hold,
     *     naming all of them.
     * @throws DuplicateApiGuardrailException when that is the request's only fault; likewise
     *     {@link GuardrailNotDeployedException}, {@link GuardrailIncompatibleApiTypeException},
     *     {@link GuardrailPhaseMismatchException} and {@link InvalidApiGuardrailConditionException}.
     * @throws ApiGuardrailSequenceRefusedException when the request has several faults and they are not all "not
     *     found": its parameters say which ids are refused for which reason.
     */
    public SequencedMap<Phase, List<GuardrailRef>> validateAttachments(
        GuardedApi api,
        Map<Phase, List<ApiGuardrailAttachment>> attachmentsByPhase
    ) {
        attachmentsByPhase.forEach((phase, attachments) -> {
            if (attachments.size() > MAX_GUARDRAILS_PER_PHASE) {
                throw new TooManyApiGuardrailsException(phase, attachments.size(), MAX_GUARDRAILS_PER_PHASE);
            }
        });
        var lookup = lookUp(api.environmentId(), attachmentsByPhase);

        var refusals = new ArrayList<Refused>();
        var refs = new LinkedHashMap<Phase, List<GuardrailRef>>();
        for (var phase : Phase.values()) {
            var attachments = attachmentsByPhase.get(phase);
            if (attachments == null) {
                continue;
            }
            var phaseRefs = new ArrayList<GuardrailRef>();
            var listed = new HashSet<String>();
            for (var attachment : attachments) {
                var refused = listed.add(attachment.guardrailId())
                    ? refusalOf(api, phase, attachment, lookup)
                    : Optional.of(new Refused(Refusal.LISTED_TWICE, attachment.guardrailId(), phase, null, null));
                refused.ifPresentOrElse(refusals::add, () -> phaseRefs.add(lookup.refOf(attachment)));
            }
            refs.put(phase, phaseRefs);
        }
        if (!refusals.isEmpty()) {
            throw refusalOf(api, refusals);
        }
        return refs;
    }

    /**
     * One read of the store and one of the deployment history for the whole request, whatever the number of phases:
     * this runs on every deployment and every start of the API.
     */
    private Lookup lookUp(String environmentId, Map<Phase, List<ApiGuardrailAttachment>> attachmentsByPhase) {
        var requested = attachmentsByPhase
            .values()
            .stream()
            .flatMap(List::stream)
            .map(ApiGuardrailAttachment::guardrailId)
            .collect(Collectors.toSet());
        var known = guardrailRepository
            .findByEnvironmentIdAndIds(environmentId, requested)
            .stream()
            .map(Guardrail::getId)
            .collect(Collectors.toSet());
        return new Lookup(known, deployments.current(environmentId, requested));
    }

    private Optional<Refused> refusalOf(GuardedApi api, Phase phase, ApiGuardrailAttachment attachment, Lookup lookup) {
        var guardrailId = attachment.guardrailId();
        if (!lookup.known().contains(guardrailId)) {
            return Optional.of(new Refused(Refusal.NOT_FOUND, guardrailId, phase, null, null));
        }
        if (!(lookup.current().get(guardrailId) instanceof GuardrailDeployment.Published(var deployed, _, _))) {
            return Optional.of(new Refused(Refusal.NOT_DEPLOYED, guardrailId, phase, null, null));
        }
        if (!deployed.isCompatibleWith(api.type())) {
            return Optional.of(new Refused(Refusal.INCOMPATIBLE_API_TYPE, guardrailId, phase, null, null));
        }
        if (deployed.getPhase() != phase) {
            return Optional.of(new Refused(Refusal.PHASE_MISMATCH, guardrailId, phase, deployed.getPhase(), null));
        }
        return Optional.ofNullable(attachment.condition())
            .flatMap(conditionSyntax::errorIn)
            .map(error -> new Refused(Refusal.INVALID_CONDITION, guardrailId, phase, null, error));
    }

    private static RuntimeException refusalOf(GuardedApi api, List<Refused> refusals) {
        if (refusals.stream().allMatch(refused -> refused.kind() == Refusal.NOT_FOUND)) {
            return GuardrailNotFoundException.of(
                refusals.stream().map(Refused::guardrailId).collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
        if (refusals.size() == 1) {
            // A request with a single fault answers the exception that fault is on its own.
            var refused = refusals.getFirst();
            return switch (refused.kind()) {
                case LISTED_TWICE -> new DuplicateApiGuardrailException(refused.guardrailId());
                case NOT_FOUND -> new GuardrailNotFoundException(refused.guardrailId());
                case NOT_DEPLOYED -> new GuardrailNotDeployedException(refused.guardrailId());
                case INCOMPATIBLE_API_TYPE -> new GuardrailIncompatibleApiTypeException(refused.guardrailId(), api.type());
                case PHASE_MISMATCH -> new GuardrailPhaseMismatchException(refused.guardrailId(), refused.phase(), refused.deployedPhase());
                case INVALID_CONDITION -> new InvalidApiGuardrailConditionException(refused.guardrailId(), refused.conditionError());
            };
        }
        // Once per id and per refusal: the same id refused for the same reason on both phases is one thing to fix.
        var idsByRefusal = new LinkedHashMap<Refusal, SequencedSet<String>>();
        for (var refused : refusals) {
            idsByRefusal.computeIfAbsent(refused.kind(), _ -> new LinkedHashSet<>()).add(refused.guardrailId());
        }
        return new ApiGuardrailSequenceRefusedException(idsByRefusal);
    }

    /** What the store and the deployment history answer for the Guardrails of one request. */
    private record Lookup(Set<String> known, Map<String, GuardrailDeployment> current) {
        GuardrailRef refOf(ApiGuardrailAttachment attachment) {
            var deployed = ((GuardrailDeployment.Published) current.get(attachment.guardrailId())).definition();
            return new GuardrailRef(attachment.guardrailId(), deployed.getName(), attachment.condition());
        }
    }

    /**
     * One fault of the request.
     *
     * @param deployedPhase the phase the deployed revision runs in, for a phase mismatch only.
     * @param conditionError why the gateway cannot evaluate the condition, for an invalid condition only.
     */
    private record Refused(Refusal kind, String guardrailId, Phase phase, Phase deployedPhase, String conditionError) {}
}
