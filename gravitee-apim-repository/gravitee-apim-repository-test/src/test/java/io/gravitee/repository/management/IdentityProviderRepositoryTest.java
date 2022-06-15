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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProviderRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/identityprovider-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<IdentityProvider> identityProviders = identityProviderRepository.findAll();

        assertNotNull(identityProviders);
        assertEquals(3, identityProviders.size());
        for (IdentityProvider idp : identityProviders) {
            final Map<String, String[]> groupMappings = idp.getGroupMappings();
            assertNotNull(groupMappings);
            for (Map.Entry<String, String[]> gm : groupMappings.entrySet()) {
                assertNotNull(gm.getValue());
            }
        }
    }

    @Test
    public void shouldFindAllByOrganizationId() throws Exception {
        final Set<IdentityProvider> identityProviders = identityProviderRepository.findAllByOrganizationId("DEFAULT");

        assertNotNull(identityProviders);
        assertEquals(3, identityProviders.size());
    }

    @Test
    public void shouldFindById() throws Exception {
        final Optional<IdentityProvider> identityProviderOpt = identityProviderRepository.findById("idp-3");
        assertNotNull(identityProviderOpt);
        assertTrue(identityProviderOpt.isPresent());

        final IdentityProvider identityProvider = identityProviderOpt.get();
        assertNotNull(identityProvider);
        assertEquals("Gravitee.io AM", identityProvider.getName());
        assertEquals("Gravitee.io AM Identity Provider", identityProvider.getDescription());
        assertEquals("DEFAULT", identityProvider.getOrganizationId());
        assertFalse(identityProvider.isEnabled());
        assertEquals(IdentityProviderType.GRAVITEEIO_AM, identityProvider.getType());
        assertNull(identityProvider.getEmailRequired());
        assertNull(identityProvider.getSyncMappings());

        String condition = "{#jsonPath('$.email_verified')}";

        // check group mappings
        assertNotNull(identityProvider.getGroupMappings());
        assertEquals(1, identityProvider.getGroupMappings().size());
        assertNotNull(identityProvider.getGroupMappings().get(condition));
        assertEquals(2, identityProvider.getGroupMappings().get(condition).length);
        assertEquals("group1", identityProvider.getGroupMappings().get(condition)[0]);
        assertEquals("group2", identityProvider.getGroupMappings().get(condition)[1]);

        // check role mappings
        assertNotNull(identityProvider.getRoleMappings());
        assertEquals(1, identityProvider.getRoleMappings().size());
        assertNotNull(identityProvider.getRoleMappings().get(condition));
        assertEquals(2, identityProvider.getRoleMappings().get(condition).length);
        assertEquals("role1", identityProvider.getRoleMappings().get(condition)[0]);
        assertEquals("role2", identityProvider.getRoleMappings().get(condition)[1]);

        // check user profile mappings
        assertNotNull(identityProvider.getUserProfileMapping());
        assertEquals("id", identityProvider.getUserProfileMapping().get("sub"));
        assertEquals("firstname", identityProvider.getUserProfileMapping().get("firstname"));
        assertEquals("mail", identityProvider.getUserProfileMapping().get("email"));
    }

    @Test
    public void shouldCreate() throws Exception {
        final IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setId("new-idp");
        identityProvider.setOrganizationId("DEFAULT");
        identityProvider.setName("My idp 1");
        identityProvider.setDescription("Description for my idp 1");
        identityProvider.setCreatedAt(new Date(1000000000000L));
        identityProvider.setUpdatedAt(new Date(1439032010883L));
        identityProvider.setType(IdentityProviderType.GITHUB);
        identityProvider.setEnabled(true);
        identityProvider.setEmailRequired(true);
        identityProvider.setSyncMappings(true);

        int nbIdentityProvidersBeforeCreation = identityProviderRepository.findAll().size();
        identityProviderRepository.create(identityProvider);
        int nbIdentityProvidersAfterCreation = identityProviderRepository.findAll().size();

        Assert.assertEquals(nbIdentityProvidersBeforeCreation + 1, nbIdentityProvidersAfterCreation);

        Optional<IdentityProvider> optional = identityProviderRepository.findById("new-idp");
        Assert.assertTrue("Identity provider saved not found", optional.isPresent());

        final IdentityProvider identityProviderSaved = optional.get();
        Assert.assertEquals("Invalid saved identity provider name.", identityProvider.getName(), identityProviderSaved.getName());
        Assert.assertEquals(
            "Invalid saved identity provider organization id.",
            identityProvider.getOrganizationId(),
            identityProviderSaved.getOrganizationId()
        );
        Assert.assertEquals(
            "Invalid identity provider description.",
            identityProvider.getDescription(),
            identityProviderSaved.getDescription()
        );
        Assert.assertTrue(
            "Invalid identity provider createdAt.",
            compareDate(identityProvider.getCreatedAt(), identityProviderSaved.getCreatedAt())
        );
        Assert.assertTrue(
            "Invalid identity provider updatedAt.",
            compareDate(identityProvider.getUpdatedAt(), identityProviderSaved.getUpdatedAt())
        );
        Assert.assertEquals("Invalid identity provider type.", identityProvider.getType(), identityProviderSaved.getType());
        Assert.assertEquals("Invalid identity provider enabled.", identityProvider.isEnabled(), identityProviderSaved.isEnabled());
        Assert.assertEquals(
            "Invalid identity provider emailRequired.",
            identityProvider.getEmailRequired(),
            identityProviderSaved.getEmailRequired()
        );
        Assert.assertEquals(
            "Invalid identity provider syncMappings.",
            identityProvider.getSyncMappings(),
            identityProviderSaved.getSyncMappings()
        );
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<IdentityProvider> optional = identityProviderRepository.findById("idp-1");
        Assert.assertTrue("Identity provider to update not found", optional.isPresent());
        Assert.assertEquals("Invalid saved identity provider name.", "Google", optional.get().getName());

        final IdentityProvider identityProvider = optional.get();
        identityProvider.setName("Google");
        identityProvider.setOrganizationId("DEFAULT");
        identityProvider.setDescription("Google Identity Provider");
        identityProvider.setCreatedAt(new Date(1000000000000L));
        identityProvider.setUpdatedAt(new Date(1486771200000L));
        identityProvider.setType(IdentityProviderType.GOOGLE);
        identityProvider.setEnabled(true);
        identityProvider.setEmailRequired(true);
        identityProvider.setSyncMappings(true);

        int nbIdentityProvidersBeforeUpdate = identityProviderRepository.findAll().size();
        identityProviderRepository.update(identityProvider);
        int nbIdentityProvidersAfterUpdate = identityProviderRepository.findAll().size();

        Assert.assertEquals(nbIdentityProvidersBeforeUpdate, nbIdentityProvidersAfterUpdate);

        Optional<IdentityProvider> optionalUpdated = identityProviderRepository.findById("idp-1");
        Assert.assertTrue("Identity provider to update not found", optionalUpdated.isPresent());

        final IdentityProvider identityProviderUpdated = optionalUpdated.get();
        Assert.assertEquals("Invalid saved identity provider name.", identityProvider.getName(), identityProviderUpdated.getName());
        Assert.assertEquals(
            "Invalid saved identity provider organization id.",
            identityProvider.getOrganizationId(),
            identityProviderUpdated.getOrganizationId()
        );
        Assert.assertEquals(
            "Invalid identity provider description.",
            identityProvider.getDescription(),
            identityProviderUpdated.getDescription()
        );
        Assert.assertTrue(
            "Invalid identity provider createdAt.",
            compareDate(identityProvider.getCreatedAt(), identityProviderUpdated.getCreatedAt())
        );
        Assert.assertTrue(
            "Invalid identity provider updatedAt.",
            compareDate(identityProvider.getUpdatedAt(), identityProviderUpdated.getUpdatedAt())
        );
        Assert.assertEquals("Invalid identity provider type.", identityProvider.getType(), identityProviderUpdated.getType());
        Assert.assertEquals("Invalid identity provider enabled.", identityProvider.isEnabled(), identityProviderUpdated.isEnabled());
        Assert.assertEquals(
            "Invalid identity provider emailRequired.",
            identityProvider.getEmailRequired(),
            identityProviderUpdated.getEmailRequired()
        );
        Assert.assertEquals(
            "Invalid identity provider syncMappings.",
            identityProvider.getSyncMappings(),
            identityProviderUpdated.getSyncMappings()
        );
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbIdentityProvidersBeforeDeletion = identityProviderRepository.findAll().size();
        identityProviderRepository.delete("idp-3");
        int nbIdentityProvidersAfterDeletion = identityProviderRepository.findAll().size();

        Assert.assertEquals(nbIdentityProvidersBeforeDeletion - 1, nbIdentityProvidersAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownIdentityProvider() throws Exception {
        IdentityProvider unknownIdentityProvider = new IdentityProvider();
        unknownIdentityProvider.setId("unknown");
        unknownIdentityProvider.setOrganizationId("unknown");
        identityProviderRepository.update(unknownIdentityProvider);
        fail("An unknown identity provider should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        identityProviderRepository.update(null);
        fail("A null identity provider should not be updated");
    }
}
