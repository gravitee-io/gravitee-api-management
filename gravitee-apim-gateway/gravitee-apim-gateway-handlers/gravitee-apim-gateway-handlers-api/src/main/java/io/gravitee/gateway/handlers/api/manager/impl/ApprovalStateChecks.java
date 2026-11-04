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
package io.gravitee.gateway.handlers.api.manager.impl;

import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;

/** The checks every approval state store applies to a state before returning it, whichever way it holds the states. */
final class ApprovalStateChecks {

    private static final String TIMED = "TIMED";

    private ApprovalStateChecks() {}

    /**
     * AIM raises the version on every change, so when both copies carry one the higher wins, whichever arrives last.
     * Otherwise, or for the same version, the copies are ordered by {@code updatedAt}: AIM keeps one latest event per
     * request and overwrites it, so a copy read later is never an older write than one read before, even when both carry
     * the same millisecond. Such a copy that differs is therefore the later write and replaces the one held; an
     * identical copy, read again, changes nothing and wakes nobody.
     */
    static boolean supersedes(DeployedApprovalState candidate, DeployedApprovalState current) {
        if (candidate.version() > 0 && current.version() > 0 && candidate.version() != current.version()) {
            return candidate.version() > current.version();
        }
        return (
            candidate.updatedAt() > current.updatedAt() || (candidate.updatedAt() == current.updatedAt() && !candidate.equals(current))
        );
    }

    static boolean isFoundByOpenKey(DeployedApprovalState state) {
        return state.openKey() != null && state.approvalStatus().isFoundByOpenKey();
    }

    static boolean isKept(DeployedApprovalState state, long now) {
        return state.keepUntil() > now;
    }

    static boolean grantCovers(DeployedApprovalState state, long now) {
        DeployedApprovalState.Grant grant = state.grant();
        return (
            grant != null &&
            TIMED.equals(grant.scope()) &&
            grant.coversUntil() != null &&
            grant.coversUntil() > now &&
            state.approvalStatus().isGranting()
        );
    }
}
