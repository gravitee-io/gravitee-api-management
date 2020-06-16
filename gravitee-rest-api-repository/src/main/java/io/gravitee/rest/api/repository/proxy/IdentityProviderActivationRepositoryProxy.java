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
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.model.IdentityProviderActivation;
import io.gravitee.repository.management.model.IdentityProviderActivationReferenceType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class IdentityProviderActivationRepositoryProxy extends AbstractProxy<IdentityProviderActivationRepository> implements IdentityProviderActivationRepository {


    @Override
    public Optional<IdentityProviderActivation> findById(String identityProviderId, String referenceId, IdentityProviderActivationReferenceType referenceType) throws TechnicalException {
        return target.findById(identityProviderId, referenceId, referenceType);
    }

    @Override
    public Set<IdentityProviderActivation> findAll() throws TechnicalException {
        return target.findAll();
    }

    @Override
    public Set<IdentityProviderActivation> findAllByIdentityProviderId(String identityProviderId) throws TechnicalException {
        return target.findAllByIdentityProviderId(identityProviderId);
    }

    @Override
    public Set<IdentityProviderActivation> findAllByReferenceIdAndReferenceType(String referenceId, IdentityProviderActivationReferenceType referenceType) throws TechnicalException {
        return target.findAllByReferenceIdAndReferenceType(referenceId, referenceType);
    }

    @Override
    public IdentityProviderActivation create(IdentityProviderActivation identityProviderActivation) throws TechnicalException {
        return target.create(identityProviderActivation);
    }

    @Override
    public void delete(String identityProviderId, String referenceId, IdentityProviderActivationReferenceType referenceType) throws TechnicalException {
        target.delete(identityProviderId, referenceId, referenceType);
    }

    @Override
    public void deleteByIdentityProviderId(String identityProviderId) throws TechnicalException {
        target.deleteByIdentityProviderId(identityProviderId);
    }

    @Override
    public void deleteByReferenceIdAndReferenceType(String referenceId, IdentityProviderActivationReferenceType referenceType) throws TechnicalException {
        target.deleteByReferenceIdAndReferenceType(referenceId, referenceType);
    }
}
