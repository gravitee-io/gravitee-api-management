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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProviderActivation {

    /**
     * Identity provider ID
     */
    private String identityProviderId;

    /**
     * Reference ID
     */
    private String referenceId;

    /**
     * Reference Type
     */
    private IdentityProviderActivationReferenceType referenceType;

    /**
     * Activation date
     */
    private Date createdAt;

    public String getIdentityProviderId() {
        return identityProviderId;
    }

    public void setIdentityProviderId(String identityProviderId) {
        this.identityProviderId = identityProviderId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public IdentityProviderActivationReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(IdentityProviderActivationReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityProviderActivation that = (IdentityProviderActivation) o;
        return Objects.equals(identityProviderId, that.identityProviderId) &&
                Objects.equals(referenceId, that.referenceId) &&
                Objects.equals(referenceType, that.referenceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityProviderId, referenceId, referenceType);
    }
}
