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
package io.gravitee.rest.api.service;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.model.Installation;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.exceptions.InstallationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.InstallationServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class InstallationServiceTest {

    private static final String INSTALLATION_ID = "id-installation";
    private static final String COCKPIT_INSTALLATION_ID = "id-cockpit-installation";
    private static final String COCKPIT_INSTALLATION_STATUS = "status-cockpit-installation";
    private static final Map<String, String> ADDITIONAL_INFORMATION = new HashMap<String, String>() {
        {
            put(InstallationService.COCKPIT_INSTALLATION_ID, COCKPIT_INSTALLATION_ID);
            put(InstallationService.COCKPIT_INSTALLATION_STATUS, COCKPIT_INSTALLATION_STATUS);
        }
    };

    @InjectMocks
    private InstallationServiceImpl installationService = new InstallationServiceImpl();

    @Mock
    private InstallationRepository installationRepository;

    private Installation installation;

    private final static Date NOW = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));;

    @Before
    public void init() throws TechnicalException {
        reset(installationRepository);

        installation = new Installation();
        installation.setId(INSTALLATION_ID);
        installation.setCreatedAt(NOW);
        installation.setUpdatedAt(NOW);
        installation.setAdditionalInformation(ADDITIONAL_INFORMATION);
        when(installationRepository.find()).thenReturn(Optional.of(installation));
    }

    @Test
    public void shouldGetInstallation() throws TechnicalException {
        final InstallationEntity installationEntity = installationService.get();

        verify(installationRepository).find();

        assertNotNull(installationEntity);
        assertEquals(INSTALLATION_ID, installationEntity.getId());
        assertEquals(NOW, installationEntity.getCreatedAt());
        assertEquals(NOW, installationEntity.getUpdatedAt());
        assertEquals(2, installationEntity.getAdditionalInformation().size());
        assertEquals(COCKPIT_INSTALLATION_ID, installationEntity.getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_ID));
        assertEquals(COCKPIT_INSTALLATION_STATUS, installationEntity.getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_STATUS));
    }

    @Test
    public void shouldGetInstallationIfExists() throws TechnicalException {
        final InstallationEntity installationEntity = installationService.getOrInitialize();

        verify(installationRepository).find();
        verify(installationRepository, times(0)).create(any());

        assertNotNull(installationEntity);
        assertEquals(INSTALLATION_ID, installationEntity.getId());
        assertEquals(NOW, installationEntity.getCreatedAt());
        assertEquals(NOW, installationEntity.getUpdatedAt());
        assertEquals(2, installationEntity.getAdditionalInformation().size());
        assertEquals(COCKPIT_INSTALLATION_ID, installationEntity.getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_ID));
        assertEquals(COCKPIT_INSTALLATION_STATUS, installationEntity.getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_STATUS));
    }

    @Test
    public void shouldCreateInstallationIfNotFound() throws TechnicalException {
        when(installationRepository.find()).thenReturn(Optional.empty());
        when(installationRepository.create(any())).thenReturn(installation);

        final InstallationEntity installationEntity = installationService.getOrInitialize();

        verify(installationRepository).find();
        verify(installationRepository).create(any());

        assertNotNull(installationEntity);
        assertEquals(INSTALLATION_ID, installationEntity.getId());
        assertEquals(NOW, installationEntity.getCreatedAt());
        assertEquals(NOW, installationEntity.getUpdatedAt());
        assertEquals(2, installationEntity.getAdditionalInformation().size());
        assertEquals(COCKPIT_INSTALLATION_ID, installationEntity.getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_ID));
        assertEquals(COCKPIT_INSTALLATION_STATUS, installationEntity.getAdditionalInformation().get(InstallationService.COCKPIT_INSTALLATION_STATUS));
    }

    @Test
    public void shouldUpdateAdditionalInformation() throws TechnicalException {
        Map<String, String> newAdditionalInformation = new HashMap<>();
        newAdditionalInformation.put("key1", "value1");

        Installation updatedInstallation = new Installation(installation);
        updatedInstallation.setAdditionalInformation(newAdditionalInformation);
        updatedInstallation.setUpdatedAt(new Date());
        when(installationRepository.update(any(Installation.class))).thenReturn(updatedInstallation);

        final InstallationEntity updatedInstallationEntity = installationService.setAdditionalInformation(newAdditionalInformation);

        verify(installationRepository).find();
        verify(installationRepository).update(ArgumentMatchers.argThat(argument -> argument != null
                && INSTALLATION_ID.equals(argument.getId())
                && NOW.equals(argument.getCreatedAt())
                && NOW.before(argument.getUpdatedAt())
                && newAdditionalInformation.equals(argument.getAdditionalInformation())));

        assertNotNull(updatedInstallationEntity);
        assertEquals(INSTALLATION_ID, updatedInstallationEntity.getId());
        assertEquals(NOW, updatedInstallationEntity.getCreatedAt());
        assertEquals(1, updatedInstallationEntity.getAdditionalInformation().size());
        assertEquals("value1", updatedInstallationEntity.getAdditionalInformation().get("key1"));
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(installationRepository.find()).thenThrow(TechnicalException.class);
        installationService.get();
    }

    @Test(expected = InstallationNotFoundException.class)
    public void shouldNotFindBecauseNotExists() throws TechnicalException {
        when(installationRepository.find()).thenReturn(Optional.empty());

        installationService.get();
    }

    @Test(expected = InstallationNotFoundException.class)
    public void shouldNotUpdateAddiontalInformationFindBecauseNotExists() throws TechnicalException {
        when(installationRepository.find()).thenReturn(Optional.empty());

        installationService.setAdditionalInformation(new HashMap<>());
    }
}