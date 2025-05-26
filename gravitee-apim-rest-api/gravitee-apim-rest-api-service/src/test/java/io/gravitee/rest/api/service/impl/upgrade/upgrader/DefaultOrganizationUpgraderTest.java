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

import static org.mockito.Mockito.*;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.rest.api.service.OrganizationService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultOrganizationUpgraderTest {

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private final DefaultOrganizationUpgrader upgrader = new DefaultOrganizationUpgrader();

    @Test
    public void shouldInitializeOrganisation() throws UpgraderException {
        when(organizationService.count()).thenReturn(0L);
        upgrader.upgrade();
        verify(organizationService, times(1)).initialize();
    }

    @Test
    public void shouldNotInitializeOrganisation() throws UpgraderException {
        when(organizationService.count()).thenReturn(1L);
        upgrader.upgrade();
        verify(organizationService, never()).initialize();
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.DEFAULT_ORGANIZATION_UPGRADER, upgrader.getOrder());
    }
}
