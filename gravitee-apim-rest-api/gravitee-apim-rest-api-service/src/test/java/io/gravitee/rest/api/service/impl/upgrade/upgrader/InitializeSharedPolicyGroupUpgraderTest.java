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

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.shared_policy_group.use_case.InitializeSharedPolicyGroupUseCase;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.OrganizationService;
import java.util.List;
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
public class InitializeSharedPolicyGroupUpgraderTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private InitializeSharedPolicyGroupUseCase initializeSharedPolicyGroupUseCase;

    @InjectMocks
    private final InitializeSharedPolicyGroupUpgrader upgrader = new InitializeSharedPolicyGroupUpgrader();

    @Test
    public void upgrade_should_failed_because_of_exception() throws TechnicalException {
        when(organizationService.findAll()).thenThrow(new RuntimeException());

        assertFalse(upgrader.upgrade());

        verify(organizationService, times(1)).findAll();
        verifyNoMoreInteractions(environmentService);
        verifyNoMoreInteractions(initializeSharedPolicyGroupUseCase);
    }

    @Test
    public void should_initialize_shared_policy_group() throws TechnicalException {
        when(organizationService.findAll()).thenReturn(List.of(new OrganizationEntity()));
        when(environmentService.findByOrganization(any())).thenReturn(List.of(new EnvironmentEntity()));

        Assert.assertTrue(upgrader.upgrade());

        verify(organizationService, times(1)).findAll();
        verify(environmentService, times(1)).findByOrganization(any());
        verify(initializeSharedPolicyGroupUseCase, times(1)).execute(any());
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.INITIALIZE_SHARED_POLICY_GROUP_UPGRADER, upgrader.getOrder());
    }
}
