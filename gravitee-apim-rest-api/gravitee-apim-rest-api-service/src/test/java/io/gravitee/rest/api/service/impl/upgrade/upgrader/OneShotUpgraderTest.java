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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.InstallationEntity;
import io.gravitee.rest.api.service.InstallationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class OneShotUpgraderTest {

    private static final String STATUS_KEY = "DRY_RUN_TEST";

    @Mock
    private InstallationService installationService;

    @Spy
    @InjectMocks
    private DryRunUpgrader upgrader = new DryRunUpgrader();

    @Test
    public void shouldNotProcessUpgradeOnDryRunSuccess() throws Exception {
        InstallationEntity installationEntity = new InstallationEntity();
        installationEntity.getAdditionalInformation().put(STATUS_KEY, UpgradeStatus.DRY_SUCCESS.name());
        when(installationService.getOrInitialize()).thenReturn(installationEntity);
        upgrader.upgrade();
        verify(upgrader, never()).processOneShotUpgrade();
    }

    private static class DryRunUpgrader extends OneShotUpgrader {

        public DryRunUpgrader() {
            super(STATUS_KEY);
        }

        @Override
        protected void processOneShotUpgrade() throws Exception {}

        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public boolean isDryRun() {
            return true;
        }
    }
}
