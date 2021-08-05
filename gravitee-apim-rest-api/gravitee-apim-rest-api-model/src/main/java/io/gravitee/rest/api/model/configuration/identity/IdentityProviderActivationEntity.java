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
package io.gravitee.rest.api.model.configuration.identity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.Objects;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProviderActivationEntity {

    private String identityProvider;

    private String referenceId;

    private IdentityProviderActivationReferenceType referenceType;

    @JsonProperty("created_at")
    private Date createdAt;

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
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
        IdentityProviderActivationEntity that = (IdentityProviderActivationEntity) o;
        return (
            Objects.equals(identityProvider, that.identityProvider) &&
            Objects.equals(referenceId, that.referenceId) &&
            Objects.equals(referenceType, that.referenceType)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityProvider, referenceId, referenceType);
    }
}
