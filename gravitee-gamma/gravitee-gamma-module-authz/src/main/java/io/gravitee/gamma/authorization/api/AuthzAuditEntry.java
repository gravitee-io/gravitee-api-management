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
package io.gravitee.gamma.authorization.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthzAuditEntry(
    @NotNull AuthzCallerContext caller,
    @NotNull AuthzAuditEvent event,
    @NotNull AuthzAuditReferenceKind referenceKind,
    @NotBlank String referenceId,
    AuthzAuditSnapshot oldSnapshot,
    AuthzAuditSnapshot newSnapshot
) {
    public AuthzAuditEntry {
        AuthzValidators.validateCtor(AuthzAuditEntry.class, caller, event, referenceKind, referenceId, oldSnapshot, newSnapshot);
        if (oldSnapshot == null && newSnapshot == null) {
            throw new IllegalArgumentException("at least one of oldSnapshot / newSnapshot must be non-null");
        }
        switch (referenceKind) {
            case POLICY -> {
                if (!(event instanceof AuthzPolicyAuditEvent)) {
                    throw new IllegalArgumentException(
                        "referenceKind=POLICY requires AuthzPolicyAuditEvent, got " + event.getClass().getSimpleName()
                    );
                }
                if (oldSnapshot != null && !(oldSnapshot instanceof AuthzPolicyAuditSnapshot)) {
                    throw new IllegalArgumentException("referenceKind=POLICY requires PolicyAuditSnapshot for oldSnapshot");
                }
                if (newSnapshot != null && !(newSnapshot instanceof AuthzPolicyAuditSnapshot)) {
                    throw new IllegalArgumentException("referenceKind=POLICY requires PolicyAuditSnapshot for newSnapshot");
                }
            }
            case ENTITY -> {
                if (!(event instanceof AuthzEntityAuditEvent)) {
                    throw new IllegalArgumentException(
                        "referenceKind=ENTITY requires AuthzEntityAuditEvent, got " + event.getClass().getSimpleName()
                    );
                }
                if (oldSnapshot != null && !(oldSnapshot instanceof AuthzEntityAuditSnapshot)) {
                    throw new IllegalArgumentException("referenceKind=ENTITY requires EntityAuditSnapshot for oldSnapshot");
                }
                if (newSnapshot != null && !(newSnapshot instanceof AuthzEntityAuditSnapshot)) {
                    throw new IllegalArgumentException("referenceKind=ENTITY requires EntityAuditSnapshot for newSnapshot");
                }
            }
        }
    }

    public static AuthzAuditEntry policy(
        AuthzCallerContext caller,
        AuthzPolicyAuditEvent event,
        String policyId,
        AuthzPolicyAuditSnapshot oldSnapshot,
        AuthzPolicyAuditSnapshot newSnapshot
    ) {
        return new AuthzAuditEntry(caller, event, AuthzAuditReferenceKind.POLICY, policyId, oldSnapshot, newSnapshot);
    }

    public static AuthzAuditEntry entity(
        AuthzCallerContext caller,
        AuthzEntityAuditEvent event,
        String entityId,
        AuthzEntityAuditSnapshot oldSnapshot,
        AuthzEntityAuditSnapshot newSnapshot
    ) {
        return new AuthzAuditEntry(caller, event, AuthzAuditReferenceKind.ENTITY, entityId, oldSnapshot, newSnapshot);
    }
}
