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

import io.gravitee.apim.core.DomainService;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.exception.ApiGuardianSequenceRefusedException;
import io.gravitee.gamma.rest.core.guardian.exception.ApiGuardianSequenceRefusedException.Refusal;
import io.gravitee.gamma.rest.core.guardian.exception.DuplicateApiGuardianException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianIncompatibleApiTypeException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotDeployedException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianPhaseMismatchException;
import io.gravitee.gamma.rest.core.guardian.exception.TooManyApiGuardiansException;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardians.GuardianRef;
import io.gravitee.gamma.rest.core.guardian.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
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
 * The Guardians an API is allowed to run, phase by phase: the management-side half of the fail-closed check the
 * gateway runs on the Guardian it loads.
 *
 * <p>Everything is read from the revision the gateways run, not from the draft: a draft a publisher has not deployed
 * yet must decide neither where a step goes nor whether the API may run it. A Guardian is therefore refused unless
 * its deployed revision supports the API's type and runs in the phase it is listed on.
 *
 * <p>The whole sequence is judged before anything is refused, so a publisher who got three Guardians wrong learns
 * all three from one answer. How that answer is shaped, and why:
 *
 * <ul>
 *   <li>every fault is "the environment holds no such Guardian" — {@link GuardianNotFoundException}, a
 *       {@code NotFoundDomainException}, naming all of them;</li>
 *   <li>exactly one fault — that fault's own exception, unchanged, because a list of one is worse prose than the
 *       sentence itself;</li>
 *   <li>anything else — {@link ApiGuardianSequenceRefusedException}, a {@code ValidationDomainException} whose
 *       parameters keep the natures apart.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateApiGuardiansDomainService {

    /**
     * How many Guardians an API may run on one phase. The length of the list comes from a client payload, and both
     * the validation and the store read it whole, so something has to bound it. A sequence this long is already past
     * what a request's latency budget can carry.
     */
    private static final int MAX_GUARDIANS_PER_PHASE = 20;

    private final GuardianRepository guardianRepository;
    private final GuardianDeploymentDomainService deployments;

    /**
     * The Guardians of every phase of one request, judged together so that one answer carries every fault.
     *
     * @param guardianIdsByPhase the sequence of each phase, in execution order. A phase the map leaves out is not
     *     judged and comes back absent; a phase mapped to an empty list runs no Guardian.
     * @throws TooManyApiGuardiansException when a phase lists more Guardians than an API may run, which is checked
     *     before anything is read.
     * @throws GuardianNotFoundException when every fault of the request is a Guardian the environment does not hold,
     *     naming all of them.
     * @throws DuplicateApiGuardianException when that is the request's only fault; likewise
     *     {@link GuardianNotDeployedException}, {@link GuardianIncompatibleApiTypeException} and
     *     {@link GuardianPhaseMismatchException}.
     * @throws ApiGuardianSequenceRefusedException when the request has several faults and they are not all "not
     *     found": its parameters say which ids are refused for which reason.
     */
    public SequencedMap<Phase, List<GuardianRef>> validate(GuardedApi api, Map<Phase, List<String>> guardianIdsByPhase) {
        guardianIdsByPhase.forEach((phase, guardianIds) -> {
            if (guardianIds.size() > MAX_GUARDIANS_PER_PHASE) {
                throw new TooManyApiGuardiansException(phase, guardianIds.size(), MAX_GUARDIANS_PER_PHASE);
            }
        });
        var lookup = lookUp(api.environmentId(), guardianIdsByPhase);

        var refusals = new ArrayList<Refused>();
        var refs = new LinkedHashMap<Phase, List<GuardianRef>>();
        for (var phase : Phase.values()) {
            var guardianIds = guardianIdsByPhase.get(phase);
            if (guardianIds == null) {
                continue;
            }
            var phaseRefs = new ArrayList<GuardianRef>();
            var listed = new HashSet<String>();
            for (var guardianId : guardianIds) {
                var refused = listed.add(guardianId)
                    ? refusalOf(api, phase, guardianId, lookup)
                    : Optional.of(new Refused(Refusal.LISTED_TWICE, guardianId, phase, null));
                refused.ifPresentOrElse(refusals::add, () -> phaseRefs.add(lookup.refOf(guardianId)));
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
    private Lookup lookUp(String environmentId, Map<Phase, List<String>> guardianIdsByPhase) {
        var requested = guardianIdsByPhase.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        var known = guardianRepository
            .findByEnvironmentIdAndIds(environmentId, requested)
            .stream()
            .map(Guardian::getId)
            .collect(Collectors.toSet());
        return new Lookup(known, deployments.current(environmentId, requested));
    }

    private static Optional<Refused> refusalOf(GuardedApi api, Phase phase, String guardianId, Lookup lookup) {
        if (!lookup.known().contains(guardianId)) {
            return Optional.of(new Refused(Refusal.NOT_FOUND, guardianId, phase, null));
        }
        if (!(lookup.current().get(guardianId) instanceof GuardianDeployment.Published(var deployed, _, _))) {
            return Optional.of(new Refused(Refusal.NOT_DEPLOYED, guardianId, phase, null));
        }
        if (!deployed.isCompatibleWith(api.type())) {
            return Optional.of(new Refused(Refusal.INCOMPATIBLE_API_TYPE, guardianId, phase, null));
        }
        if (deployed.getPhase() != phase) {
            return Optional.of(new Refused(Refusal.PHASE_MISMATCH, guardianId, phase, deployed.getPhase()));
        }
        return Optional.empty();
    }

    private static RuntimeException refusalOf(GuardedApi api, List<Refused> refusals) {
        if (refusals.stream().allMatch(refused -> refused.kind() == Refusal.NOT_FOUND)) {
            return GuardianNotFoundException.of(
                refusals.stream().map(Refused::guardianId).collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
        if (refusals.size() == 1) {
            // A request with a single fault answers the exception that fault is on its own.
            var refused = refusals.getFirst();
            return switch (refused.kind()) {
                case LISTED_TWICE -> new DuplicateApiGuardianException(refused.guardianId());
                case NOT_FOUND -> new GuardianNotFoundException(refused.guardianId());
                case NOT_DEPLOYED -> new GuardianNotDeployedException(refused.guardianId());
                case INCOMPATIBLE_API_TYPE -> new GuardianIncompatibleApiTypeException(refused.guardianId(), api.type());
                case PHASE_MISMATCH -> new GuardianPhaseMismatchException(refused.guardianId(), refused.phase(), refused.deployedPhase());
            };
        }
        // Once per id and per refusal: the same id refused for the same reason on both phases is one thing to fix.
        var idsByRefusal = new LinkedHashMap<Refusal, SequencedSet<String>>();
        for (var refused : refusals) {
            idsByRefusal.computeIfAbsent(refused.kind(), _ -> new LinkedHashSet<>()).add(refused.guardianId());
        }
        return new ApiGuardianSequenceRefusedException(idsByRefusal);
    }

    /** What the store and the deployment history answer for the Guardians of one request. */
    private record Lookup(Set<String> known, Map<String, GuardianDeployment> current) {
        GuardianRef refOf(String guardianId) {
            return new GuardianRef(guardianId, ((GuardianDeployment.Published) current.get(guardianId)).definition().getName());
        }
    }

    /**
     * One fault of the request.
     *
     * @param deployedPhase the phase the deployed revision runs in, for a phase mismatch only.
     */
    private record Refused(Refusal kind, String guardianId, Phase phase, Phase deployedPhase) {}
}
