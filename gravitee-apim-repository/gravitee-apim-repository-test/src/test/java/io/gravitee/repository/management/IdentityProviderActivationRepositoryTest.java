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
package io.gravitee.repository.management;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.IdentityProviderActivation;
import io.gravitee.repository.management.model.IdentityProviderActivationReferenceType;
import io.gravitee.repository.utils.DateUtils;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@TestMethodOrder(MethodName.class)
public class IdentityProviderActivationRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/identityprovideractivation-tests/";
    }

    @Test
    public void shouldCreate() throws Exception {
        final IdentityProviderActivation identityProviderActivation = new IdentityProviderActivation();
        identityProviderActivation.setIdentityProviderId("new-idp-act");
        identityProviderActivation.setReferenceId("DEFAULT");
        identityProviderActivation.setReferenceType(IdentityProviderActivationReferenceType.ENVIRONMENT);
        identityProviderActivation.setCreatedAt(new Date(1000000000000L));

        int nbIdentityProviderActivationsBeforeCreation = identityProviderActivationRepository.findAll().size();
        identityProviderActivationRepository.create(identityProviderActivation);
        int nbIdentityProviderActivationsAfterCreation = identityProviderActivationRepository.findAll().size();

        Assertions.assertEquals(nbIdentityProviderActivationsBeforeCreation + 1, nbIdentityProviderActivationsAfterCreation);

        Set<IdentityProviderActivation> identityProviderActivations = identityProviderActivationRepository.findAllByIdentityProviderId(
            "new-idp-act"
        );
        Assertions.assertTrue(
            identityProviderActivations != null && !identityProviderActivations.isEmpty(),
            "Identity provider activation saved not found"
        );

        final IdentityProviderActivation identityProviderActivationSaved = identityProviderActivations.iterator().next();
        Assertions.assertEquals(
            identityProviderActivationSaved.getIdentityProviderId(),
            identityProviderActivation.getIdentityProviderId(),
            "Invalid saved identity provider id."
        );
        Assertions.assertEquals(
            identityProviderActivationSaved.getReferenceId(),
            identityProviderActivation.getReferenceId(),
            "Invalid saved reference id."
        );
        Assertions.assertEquals(
            identityProviderActivationSaved.getReferenceType(),
            identityProviderActivation.getReferenceType(),
            "Invalid saved reference type."
        );
        Assertions.assertTrue(
            DateUtils.compareDate(new Date(1000000000000L), identityProviderActivation.getCreatedAt()),
            "Invalid saved created date."
        );
    }

    @Test
    public void shouldDeleteById() throws Exception {
        int nbIdentityProviderActivationsBeforeDeletion = identityProviderActivationRepository.findAll().size();
        identityProviderActivationRepository.delete("google_DEV", "DEV", IdentityProviderActivationReferenceType.ENVIRONMENT);
        int nbIdentityProviderActivationsAfterDeletion = identityProviderActivationRepository.findAll().size();

        Assertions.assertEquals(nbIdentityProviderActivationsBeforeDeletion - 1, nbIdentityProviderActivationsAfterDeletion);
    }

    @Test
    public void shouldDeleteByIdentityProviderId() throws Exception {
        int nbIdentityProviderActivationsBeforeDeletion = identityProviderActivationRepository.findAll().size();
        identityProviderActivationRepository.deleteByIdentityProviderId("oidc");
        int nbIdentityProviderActivationsAfterDeletion = identityProviderActivationRepository.findAll().size();

        Assertions.assertEquals(nbIdentityProviderActivationsBeforeDeletion - 2, nbIdentityProviderActivationsAfterDeletion);
    }

    @Test
    public void shouldDeleteByReferenceIdAndReferenceType() throws Exception {
        int nbIdentityProviderActivationsBeforeDeletion = identityProviderActivationRepository.findAll().size();
        identityProviderActivationRepository.deleteByReferenceIdAndReferenceType(
            "DEFAULT",
            IdentityProviderActivationReferenceType.ENVIRONMENT
        );
        int nbIdentityProviderActivationsAfterDeletion = identityProviderActivationRepository.findAll().size();

        Assertions.assertEquals(nbIdentityProviderActivationsBeforeDeletion - 3, nbIdentityProviderActivationsAfterDeletion);
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<IdentityProviderActivation> identityProviderActivations = identityProviderActivationRepository.findAll();

        assertNotNull(identityProviderActivations);
        assertEquals(5, identityProviderActivations.size());
    }

    @Test
    public void shouldFindAllByIdentityProviderId() throws Exception {
        final Set<IdentityProviderActivation> identityProviderActivations =
            identityProviderActivationRepository.findAllByIdentityProviderId("oidc");

        assertNotNull(identityProviderActivations);
        assertEquals(2, identityProviderActivations.size());
    }

    @Test
    public void shouldFindAllByReferenceIdAndReferenceType() throws Exception {
        final Set<IdentityProviderActivation> identityProviderActivations =
            identityProviderActivationRepository.findAllByReferenceIdAndReferenceType(
                "DEFAULT",
                IdentityProviderActivationReferenceType.ORGANIZATION
            );
        assertNotNull(identityProviderActivations);
        assertEquals(1, identityProviderActivations.size());
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<IdentityProviderActivation> optIdentityProviderActivation = identityProviderActivationRepository.findById(
            "github",
            "DEFAULT",
            IdentityProviderActivationReferenceType.ENVIRONMENT
        );

        assertTrue(optIdentityProviderActivation.isPresent());
        IdentityProviderActivation identityProviderActivation = optIdentityProviderActivation.get();
        Assertions.assertEquals("github", identityProviderActivation.getIdentityProviderId(), "Invalid identity provider id.");
        Assertions.assertEquals("DEFAULT", identityProviderActivation.getReferenceId(), "Invalid reference id.");
        Assertions.assertEquals(
            IdentityProviderActivationReferenceType.ENVIRONMENT,
            identityProviderActivation.getReferenceType(),
            "Invalid reference type."
        );
        Assertions.assertTrue(
            DateUtils.compareDate(new Date(1000000000000L), identityProviderActivation.getCreatedAt()),
            "Invalid created date."
        );
    }
}
