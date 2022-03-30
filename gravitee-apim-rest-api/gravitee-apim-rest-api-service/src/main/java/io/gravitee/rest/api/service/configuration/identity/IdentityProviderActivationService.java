/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.configuration.identity;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface IdentityProviderActivationService {
    Set<IdentityProviderActivationEntity> activateIdpOnTargets(
        ExecutionContext executionContext,
        String identityProviderId,
        ActivationTarget... targetsToAdd
    );

    Set<IdentityProviderActivationEntity> addIdpsOnTarget(
        ExecutionContext executionContext,
        ActivationTarget target,
        String... identityProviderIdsToAdd
    );

    Set<IdentityProviderActivationEntity> findAllByIdentityProviderId(String identityProviderId);

    Set<IdentityProviderActivationEntity> findAllByTarget(ActivationTarget target);

    void deactivateIdpOnTargets(ExecutionContext executionContext, String identityProviderId, ActivationTarget... targetsToRemove);

    void removeIdpsFromTarget(ExecutionContext executionContext, ActivationTarget target, String... identityProviderIdsToRemove);

    void deactivateIdpOnAllTargets(ExecutionContext executionContext, String identityProviderId);

    void removeAllIdpsFromTarget(ExecutionContext executionContext, ActivationTarget target);

    void updateTargetIdp(ExecutionContext executionContext, ActivationTarget target, List<String> identityProviderIds);

    class ActivationTarget {

        private String referenceId;
        private IdentityProviderActivationReferenceType referenceType;

        public ActivationTarget(String referenceId, IdentityProviderActivationReferenceType referenceType) {
            this.referenceId = referenceId;
            this.referenceType = referenceType;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public IdentityProviderActivationReferenceType getReferenceType() {
            return referenceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ActivationTarget that = (ActivationTarget) o;
            return Objects.equals(referenceId, that.referenceId) && referenceType == that.referenceType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(referenceId, referenceType);
        }
    }
}
