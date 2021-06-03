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
package io.gravitee.rest.api.service.promotion;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.cockpit.command.bridge.operation.BridgeOperation;
import io.gravitee.rest.api.service.cockpit.services.CockpitReply;
import io.gravitee.rest.api.service.cockpit.services.CockpitReplyStatus;
import io.gravitee.rest.api.service.cockpit.services.CockpitService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.BridgeOperationException;
import io.gravitee.rest.api.service.impl.AccessControlServiceImpl;
import io.gravitee.rest.api.service.impl.promotion.PromotionServiceImpl;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PromotionServiceTest {

    public static final String INSTALLATION_ID = "my-installation-id";
    public static final String ORGANIZATION_ID = "my-organization-id";
    public static final String ENVIRONMENT_ID = "my-environment-id";

    @InjectMocks
    private PromotionServiceImpl promotionService;

    @Mock
    private CockpitService cockpitService;

    @Test(expected = BridgeOperationException.class)
    public void shouldFailToListPromotionTargets() {
        // Given
        when(cockpitService.listPromotionTargets(ORGANIZATION_ID))
            .thenReturn(new CockpitReply<>(Collections.emptyList(), CockpitReplyStatus.ERROR));

        // When
        promotionService.listPromotionTargets(ORGANIZATION_ID, ENVIRONMENT_ID);

        assertTrue(false);
    }

    @Test
    public void shouldListPromotionTargets() {
        // Given
        PromotionTargetEntity envA = new PromotionTargetEntity();
        envA.setId("my-env-A");
        envA.setOrganizationId(ORGANIZATION_ID);
        envA.setName("ENV A");
        envA.setInstallationId(INSTALLATION_ID);

        PromotionTargetEntity envB = new PromotionTargetEntity();
        envB.setId("my-env-B");
        envB.setOrganizationId(ORGANIZATION_ID);
        envB.setName("ENV B");
        envB.setInstallationId(INSTALLATION_ID);

        PromotionTargetEntity currentEnv = new PromotionTargetEntity();
        currentEnv.setId(ENVIRONMENT_ID);
        currentEnv.setOrganizationId(ORGANIZATION_ID);
        currentEnv.setName("My Environment");
        currentEnv.setInstallationId(INSTALLATION_ID);

        when(cockpitService.listPromotionTargets(ORGANIZATION_ID))
            .thenReturn(new CockpitReply<>(Arrays.asList(envA, envB, currentEnv), CockpitReplyStatus.SUCCEEDED));

        final List<PromotionTargetEntity> promotionTargetEntities = promotionService.listPromotionTargets(ORGANIZATION_ID, ENVIRONMENT_ID);

        assertNotNull(promotionTargetEntities);
        assertEquals(2, promotionTargetEntities.size());
    }
}
