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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.node.api.upgrader.UpgraderRepository;
import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class InstallationKeyStatusUpgraderTest {

    @InjectMocks
    private InstallationKeyStatusUpgrader cut;

    @Mock
    private InstallationService installationService;

    @Mock
    private UpgraderRepository upgraderRepository;

    @Test
    public void should_not_upgrade_when_there_is_exception() {
        assertThrows(UpgraderException.class, () -> {
            when(installationService.get()).thenReturn(new InstallationEntity());
            when(installationService.get().getAdditionalInformation()).thenThrow(new RuntimeException());

            cut.upgrade();

            verify(installationService, times(1)).get();
            verify(upgraderRepository, times(0)).create(any());
        });
    }

    @Test
    public void should_upgrade() throws UpgraderException {
        Map<String, String> additionalInformation = new HashMap<>();
        additionalInformation.put(InstallationKeyStatusUpgrader.ORPHAN_CATEGORY_UPGRADER_STATUS, "SUCCESS");
        additionalInformation.put(InstallationKeyStatusUpgrader.APPLICATION_API_KEY_MODE_UPGRADER_STATUS, "SUCCESS");
        additionalInformation.put(InstallationKeyStatusUpgrader.API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS, "SUCCESS");
        additionalInformation.put(InstallationKeyStatusUpgrader.PLANS_DATA_UPGRADER_STATUS, "FAILED");

        InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setAdditionalInformation(additionalInformation);
        when(installationService.get()).thenReturn(installationEntity);
        when(upgraderRepository.findById(anyString())).thenReturn(Maybe.empty());
        when(upgraderRepository.create(any(UpgradeRecord.class))).thenReturn(Single.just(new UpgradeRecord()));

        boolean upgraded = cut.upgrade();
        assertTrue(upgraded);

        verify(installationService, times(1)).get();
        verify(upgraderRepository, times(3)).create(any(UpgradeRecord.class));
    }

    @Test
    public void should_not_create_record_when_already_exist() throws UpgraderException {
        Map<String, String> additionalInformation = new HashMap<>();
        additionalInformation.put(InstallationKeyStatusUpgrader.ORPHAN_CATEGORY_UPGRADER_STATUS, "SUCCESS");
        additionalInformation.put(InstallationKeyStatusUpgrader.APPLICATION_API_KEY_MODE_UPGRADER_STATUS, "SUCCESS");
        additionalInformation.put(InstallationKeyStatusUpgrader.API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS, "SUCCESS");
        additionalInformation.put(InstallationKeyStatusUpgrader.PLANS_DATA_UPGRADER_STATUS, "FAILED");

        InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.setAdditionalInformation(additionalInformation);
        when(installationService.get()).thenReturn(installationEntity);
        when(
            upgraderRepository.findById(
                InstallationKeyStatusUpgrader.INSTALLATION_KEY_STATUS.get(InstallationKeyStatusUpgrader.ORPHAN_CATEGORY_UPGRADER_STATUS)
            )
        ).thenReturn(Maybe.just(new UpgradeRecord()));
        when(
            upgraderRepository.findById(
                InstallationKeyStatusUpgrader.INSTALLATION_KEY_STATUS.get(
                    InstallationKeyStatusUpgrader.APPLICATION_API_KEY_MODE_UPGRADER_STATUS
                )
            )
        ).thenReturn(Maybe.empty());
        when(
            upgraderRepository.findById(
                InstallationKeyStatusUpgrader.INSTALLATION_KEY_STATUS.get(
                    InstallationKeyStatusUpgrader.API_KEY_SUBSCRIPTIONS_UPGRADER_STATUS
                )
            )
        ).thenReturn(Maybe.empty());
        when(upgraderRepository.create(any(UpgradeRecord.class))).thenReturn(Single.just(new UpgradeRecord()));

        boolean upgraded = cut.upgrade();
        assertTrue(upgraded);

        verify(installationService, times(1)).get();
        verify(upgraderRepository, times(2)).create(any(UpgradeRecord.class));
    }

    @Test
    public void test_order() {
        Assertions.assertEquals(UpgraderOrder.INSTALLATION_KEY_STATUS_UPGRADER, cut.getOrder());
    }
}
