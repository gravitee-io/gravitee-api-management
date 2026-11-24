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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.secret.domain_service.SecretReferencePermissionDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException.RefusedChange;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailOverwriteOvertakenException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailVersionConflictException;
import io.gravitee.gamma.rest.core.guardrail.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.UpdateGuardrailCommand;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the draft of a Guardrail, whether the console sends it or a deployment it rolls back to: both go through the
 * same checks, since an old revision may carry a name another Guardrail took since, or a guardrail policy or a resource type no
 * longer installed.
 *
 * <p>One thing separates them, {@link DraftOrigin}: a submitted draft is stored as its plugins accept it, where a
 * restored revision is stored as it was published.
 */
@DomainService
@RequiredArgsConstructor
public class GuardrailDraftDomainService {

    private final GuardrailRepository guardrailRepository;
    private final ValidateGuardrailDomainService validateGuardrail;
    private final AuditDomainService auditDomainService;
    private final GuardrailDeploymentDomainService deployments;
    private final GuardrailUsageDomainService usage;
    private final SecretReferencePermissionDomainService secretReferencePermissionDomainService;

    /** How many times an overwrite counts again from a draft another writer just stored, before it is refused. */
    private static final int OVERWRITE_ATTEMPTS = 3;

    /** Where a draft being stored comes from, which decides what is done with a configuration its plugin would correct. */
    public enum DraftOrigin {
        /** Written by the caller: what is stored is the configuration each plugin accepts, corrections included. */
        SUBMITTED,
        /**
         * Restored from a revision the gateways ran: it is stored as it was published, so a configuration a
         * plugin would correct is refused rather than rewritten. Storing the corrected revision instead would
         * leave a draft that no longer matches the publication, and the Guardrail {@code PENDING} straight
         * after the rollback.
         */
        RESTORED,
    }

    /**
     * Stores the draft the command describes in place of the existing one, and audits it.
     *
     * @param precondition what the stored draft has to be for the write to go through
     * @param auditProperties what the audit records beside the Guardrail
     * @param origin where the draft comes from, see {@link DraftOrigin}
     * @throws GuardrailVersionConflictException when the stored draft does not satisfy the precondition
     * @throws GuardrailOverwriteOvertakenException when a write that states no version lost every attempt
     * @throws ValidationDomainException when the definition is invalid or names a policy or a resource type that is not installed
     * @throws GuardrailInUseException when the draft moves to another phase or to other API types while an API runs it
     */
    public Guardrail replace(
        Guardrail existingGuardrail,
        UpdateGuardrailCommand command,
        DraftPrecondition precondition,
        AuditInfo auditInfo,
        GuardrailAuditEvent event,
        Map<AuditProperties, String> auditProperties,
        DraftOrigin origin
    ) {
        // before anything is judged: a payload built on a draft that moved on is refused for that, not for
        // what validating it against the newer draft would happen to find
        if (precondition instanceof DraftPrecondition.OneOf required && !required.isSatisfiedBy(existingGuardrail.getVersion())) {
            throw new GuardrailVersionConflictException(deployments.state(existingGuardrail), required);
        }

        var guardrailToUpdate = existingGuardrail.update(command);
        // after the merge, not before: the identity it needs comes from the path
        validateGuardrail.validateDefinition(guardrailToUpdate.getDefinition());
        requireRightsForAddedSecretReferences(auditInfo, existingGuardrail.getDefinition(), guardrailToUpdate.getDefinition());

        // Only a rename is checked. An edit that leaves the name alone cannot newly break the rule, and
        // checking it anyway would make a Guardrail unsaveable whenever the store already holds a case
        // variant of its name — "Foo" beside "foo", from data that predates the rule or from two creates
        // that raced past it. Nothing else about that Guardrail could then be edited.
        //
        // After validateDefinition, which refuses a missing name: the command declares the definition
        // @NotNull but not @Valid, so the name may still be null here.
        if (!guardrailToUpdate.getName().equalsIgnoreCase(existingGuardrail.getName())) {
            validateGuardrail.validateNameIsAvailable(auditInfo.environmentId(), guardrailToUpdate.getName(), existingGuardrail.getId());
        }

        switch (origin) {
            case SUBMITTED -> validateGuardrail.validateAgainstInstalledPlugins(guardrailToUpdate);
            case RESTORED -> validateGuardrail.validateStoredAgainstInstalledPlugins(guardrailToUpdate);
        }

        if (!guardrailToUpdate.runsWhere(existingGuardrail)) {
            usage.ensureUnused(existingGuardrail, RefusedChange.PHASE_OR_API_TYPES_CHANGE);
        }

        var publication = deployments.current(existingGuardrail);
        var stored = store(guardrailToUpdate, existingGuardrail, precondition, origin);
        var updatedGuardrail = deployments.state(stored.written(), publication);
        var properties = new EnumMap<AuditProperties, String>(AuditProperties.class);
        properties.putAll(auditProperties);
        properties.put(AuditProperties.GUARDRAIL, updatedGuardrail.getId());
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(event)
                .actor(auditInfo.actor())
                .oldValue(deployments.state(stored.replaced(), publication))
                .newValue(updatedGuardrail)
                .createdAt(updatedGuardrail.getUpdatedAt().atZone(ZoneOffset.UTC))
                .properties(properties)
                .build()
        );
        return updatedGuardrail;
    }

    /** A draft as it was written, with the one it took the place of: what the audit records as old and new. */
    private record Stored(Guardrail written, Guardrail replaced) {}

    /**
     * Writes the draft the way its precondition asks. A stated version is compared by the store in the
     * same step as the write: the check {@link #replace} opens with only spares a doomed write its
     * validation, and another writer may still get in after it.
     */
    private Stored store(Guardrail guardrailToUpdate, Guardrail existingGuardrail, DraftPrecondition precondition, DraftOrigin origin) {
        return switch (precondition) {
            case DraftPrecondition.AnyVersion ignored -> overwrite(guardrailToUpdate, existingGuardrail, origin);
            case DraftPrecondition.OneOf required -> guardrailRepository
                // matched against a stated version when replace() opened, and none of those is null
                .updateIfVersionMatches(guardrailToUpdate, Objects.requireNonNull(existingGuardrail.getVersion()))
                .map(written -> new Stored(written, existingGuardrail))
                .orElseThrow(() -> new GuardrailVersionConflictException(deployments.state(current(existingGuardrail)), required));
        };
    }

    /**
     * Replaces whatever draft is stored, and still gives it a version no other draft carried.
     *
     * <p>An overwrite counts from the version it read, like any write. Stored blindly, one that crossed
     * another writer would reuse the number that writer just took: two different drafts under one version,
     * and a third writer holding that number would pass a precondition against a draft it never read. So
     * the store compares here too, and an overwrite that lost the race counts again from what it finds —
     * it does not give up at once, since any version is what it asked for. The draft it finally replaces
     * is the one the audit has to show, not the one read before the race.
     *
     * <p>Losing every attempt is answered as a plain conflict rather than settled by a blind write. Not as
     * a failed precondition: this path states none — a rollback sends no version at all.
     *
     * <p>What is written is the draft that was validated, on every attempt: a {@code PUT} replaces the whole
     * definition, so nothing of it comes from the draft found. The one thing a restored revision does not
     * carry is the description, which is not part of a definition and is kept as it is stored — so a
     * restoration that lost the race keeps the description it finds, not the one it read before.
     *
     * <p>A Guardrail that carries no version — written by a node older than the counter — has none to
     * compare, and is stored as it is.
     */
    private Stored overwrite(Guardrail guardrailToUpdate, Guardrail existingGuardrail, DraftOrigin origin) {
        var read = existingGuardrail;
        for (int attempt = 0; attempt < OVERWRITE_ATTEMPTS && read.getVersion() != null; attempt++) {
            var draft = draftOver(read, guardrailToUpdate, origin).toBuilder().version(read.getVersion() + 1).build();
            var written = guardrailRepository.updateIfVersionMatches(draft, read.getVersion());
            if (written.isPresent()) {
                return new Stored(written.get(), read);
            }
            read = current(existingGuardrail);
        }
        if (read.getVersion() != null) {
            throw new GuardrailOverwriteOvertakenException(existingGuardrail.getId());
        }
        return new Stored(guardrailRepository.update(draftOver(read, guardrailToUpdate, origin)), read);
    }

    /**
     * A restored revision keeps the description of the draft it replaces: a description is not part of a
     * definition, so no revision carries one to restore.
     */
    private static Guardrail draftOver(Guardrail read, Guardrail guardrailToUpdate, DraftOrigin origin) {
        return origin == DraftOrigin.RESTORED
            ? guardrailToUpdate.toBuilder().description(read.getDescription()).build()
            : guardrailToUpdate;
    }

    /** The Guardrail as another writer left it, or a 404 when that writer deleted it. */
    private Guardrail current(Guardrail guardrail) {
        return guardrailRepository
            .findByEnvironmentId(guardrail.getEnvironmentId(), guardrail.getId())
            .orElseThrow(() -> new GuardrailNotFoundException(guardrail.getId()));
    }

    private void requireRightsForAddedSecretReferences(AuditInfo auditInfo, Object before, Object after) {
        secretReferencePermissionDomainService.validate(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            auditInfo.actor().userId(),
            before,
            after
        );
    }
}
