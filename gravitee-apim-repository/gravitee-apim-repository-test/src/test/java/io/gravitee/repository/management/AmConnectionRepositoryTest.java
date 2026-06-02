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

import io.gravitee.repository.management.model.AmConnection;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
public class AmConnectionRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/amconnection-tests/";
    }

    @Test
    public void shouldCreateAndFindByOrganizationId() throws Exception {
        final AmConnection amConnection = new AmConnection();
        amConnection.setOrganizationId("org-create");
        amConnection.setBaseUrl("https://am-create.example.com");
        amConnection.setServiceAccountAccessTokenEncrypted("cipher-create");
        amConnection.setDefaultDomainId("domain-create");
        amConnection.setDefaultDomainHrid("domain-hrid-create");
        amConnection.setGatewayUrl("https://gw-create.example.com");
        amConnection.setUpdatedAt(new Date(1439032010883L));

        amConnectionRepository.create(amConnection);

        final Optional<AmConnection> optional = amConnectionRepository.findByOrganizationId("org-create");
        assertTrue(optional.isPresent(), "Am connection saved not found");

        final AmConnection saved = optional.get();
        assertEquals("org-create", saved.getOrganizationId());
        assertEquals("https://am-create.example.com", saved.getBaseUrl());
        assertEquals("cipher-create", saved.getServiceAccountAccessTokenEncrypted());
        assertEquals("domain-create", saved.getDefaultDomainId());
        assertEquals("domain-hrid-create", saved.getDefaultDomainHrid());
        assertEquals("https://gw-create.example.com", saved.getGatewayUrl());
        assertTrue(compareDate(new Date(1439032010883L), saved.getUpdatedAt()), "Invalid updatedAt");
    }

    @Test
    public void shouldUpdate() throws Exception {
        final Optional<AmConnection> optional = amConnectionRepository.findByOrganizationId("org-update");
        assertTrue(optional.isPresent(), "Am connection to update not found");

        final AmConnection amConnection = optional.get();
        amConnection.setBaseUrl("https://am-updated.example.com");
        amConnection.setServiceAccountAccessTokenEncrypted("cipher-updated");
        amConnection.setUpdatedAt(new Date(1486771200000L));

        amConnectionRepository.update(amConnection);

        final Optional<AmConnection> optionalUpdated = amConnectionRepository.findByOrganizationId("org-update");
        assertTrue(optionalUpdated.isPresent(), "Updated am connection not found");

        final AmConnection updated = optionalUpdated.get();
        assertEquals("https://am-updated.example.com", updated.getBaseUrl());
        assertEquals("cipher-updated", updated.getServiceAccountAccessTokenEncrypted());
        assertTrue(compareDate(new Date(1486771200000L), updated.getUpdatedAt()), "Invalid updatedAt");
    }

    @Test
    public void shouldThrowWhenUpdatingUnknownOrganization() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            final AmConnection amConnection = new AmConnection();
            amConnection.setOrganizationId("unknown-org");
            amConnection.setBaseUrl("https://am-unknown.example.com");

            amConnectionRepository.update(amConnection);
        });
    }

    @Test
    public void shouldDelete() throws Exception {
        assertTrue(amConnectionRepository.findByOrganizationId("org-delete").isPresent());

        amConnectionRepository.delete("org-delete");

        assertFalse(amConnectionRepository.findByOrganizationId("org-delete").isPresent());
    }

    @Test
    public void shouldReturnEmptyForUnknownOrganization() throws Exception {
        assertTrue(amConnectionRepository.findByOrganizationId("unknown-org").isEmpty());
    }
}
