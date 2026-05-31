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

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.management.model.IdentityProvider;
import io.gravitee.repository.management.model.IdentityProviderType;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        assertEquals(5, identityProviders.size());
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

        Assertions.assertEquals(nbIdentityProvidersBeforeCreation + 1, nbIdentityProvidersAfterCreation);

        Optional<IdentityProvider> optional = identityProviderRepository.findById("new-idp");
        Assertions.assertTrue(optional.isPresent(), "Identity provider saved not found");

        final IdentityProvider identityProviderSaved = optional.get();
        Assertions.assertEquals(identityProvider.getName(), identityProviderSaved.getName(), "Invalid saved identity provider name.");
        Assertions.assertEquals(
            identityProvider.getOrganizationId(),
            identityProviderSaved.getOrganizationId(),
            "Invalid saved identity provider organization id."
        );
        Assertions.assertEquals(
            identityProvider.getDescription(),
            identityProviderSaved.getDescription(),
            "Invalid identity provider description."
        );
        Assertions.assertTrue(
            compareDate(identityProvider.getCreatedAt(), identityProviderSaved.getCreatedAt()),
            "Invalid identity provider createdAt."
        );
        Assertions.assertTrue(
            compareDate(identityProvider.getUpdatedAt(), identityProviderSaved.getUpdatedAt()),
            "Invalid identity provider updatedAt."
        );
        Assertions.assertEquals(identityProvider.getType(), identityProviderSaved.getType(), "Invalid identity provider type.");
        Assertions.assertEquals(identityProvider.isEnabled(), identityProviderSaved.isEnabled(), "Invalid identity provider enabled.");
        Assertions.assertEquals(
            identityProvider.getEmailRequired(),
            identityProviderSaved.getEmailRequired(),
            "Invalid identity provider emailRequired."
        );
        Assertions.assertEquals(
            identityProvider.getSyncMappings(),
            identityProviderSaved.getSyncMappings(),
            "Invalid identity provider syncMappings."
        );
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<IdentityProvider> optional = identityProviderRepository.findById("idp-1");
        Assertions.assertTrue(optional.isPresent(), "Identity provider to update not found");
        Assertions.assertEquals("Google", optional.get().getName(), "Invalid saved identity provider name.");

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

        Assertions.assertEquals(nbIdentityProvidersBeforeUpdate, nbIdentityProvidersAfterUpdate);

        Optional<IdentityProvider> optionalUpdated = identityProviderRepository.findById("idp-1");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Identity provider to update not found");

        final IdentityProvider identityProviderUpdated = optionalUpdated.get();
        Assertions.assertEquals(identityProvider.getName(), identityProviderUpdated.getName(), "Invalid saved identity provider name.");
        Assertions.assertEquals(
            identityProvider.getOrganizationId(),
            identityProviderUpdated.getOrganizationId(),
            "Invalid saved identity provider organization id."
        );
        Assertions.assertEquals(
            identityProvider.getDescription(),
            identityProviderUpdated.getDescription(),
            "Invalid identity provider description."
        );
        Assertions.assertTrue(
            compareDate(identityProvider.getCreatedAt(), identityProviderUpdated.getCreatedAt()),
            "Invalid identity provider createdAt."
        );
        Assertions.assertTrue(
            compareDate(identityProvider.getUpdatedAt(), identityProviderUpdated.getUpdatedAt()),
            "Invalid identity provider updatedAt."
        );
        Assertions.assertEquals(identityProvider.getType(), identityProviderUpdated.getType(), "Invalid identity provider type.");
        Assertions.assertEquals(identityProvider.isEnabled(), identityProviderUpdated.isEnabled(), "Invalid identity provider enabled.");
        Assertions.assertEquals(
            identityProvider.getEmailRequired(),
            identityProviderUpdated.getEmailRequired(),
            "Invalid identity provider emailRequired."
        );
        Assertions.assertEquals(
            identityProvider.getSyncMappings(),
            identityProviderUpdated.getSyncMappings(),
            "Invalid identity provider syncMappings."
        );
    }

    @Test
    public void shouldBeAbleToStoreBigGroupMappingAndRoleMapping() throws Exception {
        Optional<IdentityProvider> optional = identityProviderRepository.findById("idp-1");
        Assertions.assertTrue(optional.isPresent(), "Identity provider to update not found");
        Assertions.assertEquals("Google", optional.get().getName(), "Invalid saved identity provider name.");

        Map<String, String[]> veryBigGroupMappings = new HashMap<>();
        Map<String, String[]> veryBigRoleMappings = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            veryBigGroupMappings.put(
                "{#jsonPath(#profile, '$.groups').contains('long_text_to_increase_the_final_size_" + i + "')}",
                new String[] { "A_very_very_long_group_id_to_increase_the_final_size_" + i }
            );
            veryBigRoleMappings.put(
                "{#jsonPath(#profile, '$.groups').contains('long_text_to_increase_the_final_size_" + i + "')}",
                new String[] { "A_very_very_long_role_id_to_increase_the_final_size_" + i }
            );
        }

        final IdentityProvider identityProvider = optional.get();
        identityProvider.setGroupMappings(veryBigGroupMappings);
        identityProvider.setRoleMappings(veryBigRoleMappings);

        int nbIdentityProvidersBeforeUpdate = identityProviderRepository.findAll().size();
        identityProviderRepository.update(identityProvider);
        int nbIdentityProvidersAfterUpdate = identityProviderRepository.findAll().size();

        Assertions.assertEquals(nbIdentityProvidersBeforeUpdate, nbIdentityProvidersAfterUpdate);

        Optional<IdentityProvider> optionalUpdated = identityProviderRepository.findById("idp-1");
        Assertions.assertTrue(optionalUpdated.isPresent(), "Identity provider to update not found");

        final IdentityProvider identityProviderUpdated = optionalUpdated.get();
        Assertions.assertEquals(identityProvider.getName(), identityProviderUpdated.getName(), "Invalid saved identity provider name.");
        Assertions.assertTrue(
            areEqualWithArrayValue(identityProvider.getGroupMappings(), identityProviderUpdated.getGroupMappings()),
            "Invalid saved identity provider group mappings list."
        );
        Assertions.assertTrue(
            areEqualWithArrayValue(identityProvider.getRoleMappings(), identityProviderUpdated.getRoleMappings()),
            "Invalid saved identity provider role mappings list."
        );
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbIdentityProvidersBeforeDeletion = identityProviderRepository.findAll().size();
        identityProviderRepository.delete("idp-3");
        int nbIdentityProvidersAfterDeletion = identityProviderRepository.findAll().size();

        Assertions.assertEquals(nbIdentityProvidersBeforeDeletion - 1, nbIdentityProvidersAfterDeletion);
    }

    @Test
    public void shouldNotUpdateUnknownIdentityProvider() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            IdentityProvider unknownIdentityProvider = new IdentityProvider();
            unknownIdentityProvider.setId("unknown");
            unknownIdentityProvider.setOrganizationId("unknown");
            identityProviderRepository.update(unknownIdentityProvider);
            fail("An unknown identity provider should not be updated");
        });
    }

    @Test
    public void shouldNotUpdateNull() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            identityProviderRepository.update(null);
            fail("A null identity provider should not be updated");
        });
    }

    @Test
    public void should_delete_by_organization_id() throws Exception {
        final int nbBeforeDeletion = identityProviderRepository.findAllByOrganizationId("ToBeDeleted").size();
        List<String> deleted = identityProviderRepository.deleteByOrganizationId("ToBeDeleted");
        final int nbAfterDeletion = identityProviderRepository.findAllByOrganizationId("ToBeDeleted").size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted.size());
        assertEquals(0, nbAfterDeletion);
    }

    private boolean areEqualWithArrayValue(Map<String, String[]> first, Map<String, String[]> second) {
        if (first.size() != second.size()) {
            return false;
        }

        return first
            .entrySet()
            .stream()
            .allMatch(e -> Arrays.equals(e.getValue(), second.get(e.getKey())));
    }
}
