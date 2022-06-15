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
package io.gravitee.repository.mock.management;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.model.IdentityProviderActivation;
import io.gravitee.repository.management.model.IdentityProviderActivationReferenceType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProviderActivationRepositoryMock extends AbstractRepositoryMock<IdentityProviderActivationRepository> {

    public IdentityProviderActivationRepositoryMock() {
        super(IdentityProviderActivationRepository.class);
    }

    @Override
    protected void prepare(IdentityProviderActivationRepository identityProviderActivationRepository) throws Exception {
        final IdentityProviderActivation githubDefaultEnv = createMock(
            "github",
            "DEFAULT",
            IdentityProviderActivationReferenceType.ENVIRONMENT,
            1000000000000L
        );
        final IdentityProviderActivation googleDefaultEnv = createMock(
            "google",
            "DEFAULT",
            IdentityProviderActivationReferenceType.ENVIRONMENT,
            1100000000000L
        );
        final IdentityProviderActivation oidcDefaultEnv = createMock(
            "oidc",
            "DEFAULT",
            IdentityProviderActivationReferenceType.ENVIRONMENT,
            1200000000000L
        );
        final IdentityProviderActivation oidcDefaultOrg = createMock(
            "oidc",
            "DEFAULT",
            IdentityProviderActivationReferenceType.ORGANIZATION,
            1300000000000L
        );
        final IdentityProviderActivation googleDEVDevEnv = createMock(
            "google_DEV",
            "DEV",
            IdentityProviderActivationReferenceType.ENVIRONMENT,
            1400000000000L
        );

        final IdentityProviderActivation newIdpActDefaultEnv = createMock(
            "new-idp-act",
            "DEFAULT",
            IdentityProviderActivationReferenceType.ENVIRONMENT,
            1000000000000L
        );

        final Set<IdentityProviderActivation> identityProviderActivations = newSet(
            githubDefaultEnv,
            googleDefaultEnv,
            oidcDefaultEnv,
            oidcDefaultOrg,
            googleDEVDevEnv
        );
        final Set<IdentityProviderActivation> identityProviderActivationsAfterDeleteById = newSet(
            githubDefaultEnv,
            googleDefaultEnv,
            oidcDefaultEnv,
            oidcDefaultOrg
        );
        final Set<IdentityProviderActivation> identityProviderActivationsAfterDeleteByIdp = newSet(
            githubDefaultEnv,
            googleDefaultEnv,
            googleDEVDevEnv
        );
        final Set<IdentityProviderActivation> identityProviderActivationsAfterDeleteByRef = newSet(oidcDefaultOrg, googleDEVDevEnv);
        final Set<IdentityProviderActivation> identityProviderActivationsAfterAdd = newSet(
            githubDefaultEnv,
            googleDefaultEnv,
            oidcDefaultEnv,
            oidcDefaultOrg,
            googleDEVDevEnv,
            newIdpActDefaultEnv
        );

        when(identityProviderActivationRepository.findAll())
            .thenReturn(
                identityProviderActivations,
                identityProviderActivationsAfterAdd, //shouldCreate
                identityProviderActivations,
                identityProviderActivationsAfterDeleteById, //shouldDeleteById
                identityProviderActivations,
                identityProviderActivationsAfterDeleteByIdp, //shouldDeleteByIdentityProviderId
                identityProviderActivations,
                identityProviderActivationsAfterDeleteByRef, //shouldDeleteByReferenceIdAndReferenceType
                identityProviderActivations //shouldFindAll
            );

        when(identityProviderActivationRepository.create(any(IdentityProviderActivation.class))).thenReturn(newIdpActDefaultEnv);
        when(identityProviderActivationRepository.findAllByIdentityProviderId("new-idp-act")).thenReturn(newSet(newIdpActDefaultEnv));
        when(identityProviderActivationRepository.findAllByIdentityProviderId("github")).thenReturn(newSet(githubDefaultEnv));
        when(identityProviderActivationRepository.findAllByIdentityProviderId("oidc")).thenReturn(newSet(oidcDefaultEnv, oidcDefaultOrg));
        when(
            identityProviderActivationRepository.findAllByReferenceIdAndReferenceType(
                "DEFAULT",
                IdentityProviderActivationReferenceType.ORGANIZATION
            )
        )
            .thenReturn(newSet(oidcDefaultOrg));
        when(identityProviderActivationRepository.findById("github", "DEFAULT", IdentityProviderActivationReferenceType.ENVIRONMENT))
            .thenReturn(Optional.of(githubDefaultEnv));
    }

    private IdentityProviderActivation createMock(
        String idpId,
        String refId,
        IdentityProviderActivationReferenceType refType,
        long createdAtTimestamp
    ) {
        final IdentityProviderActivation ipa = new IdentityProviderActivation();
        ipa.setIdentityProviderId(idpId);
        ipa.setReferenceId(refId);
        ipa.setReferenceType(refType);
        ipa.setCreatedAt(new Date(createdAtTimestamp));
        return ipa;
    }
}
