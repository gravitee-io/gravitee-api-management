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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.repository.management.model.Promotion;
import io.gravitee.repository.management.model.PromotionAuthor;
import io.gravitee.repository.management.model.PromotionStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityAuthor;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.services.CockpitReply;
import io.gravitee.rest.api.service.cockpit.services.CockpitReplyStatus;
import io.gravitee.rest.api.service.cockpit.services.CockpitService;
import io.gravitee.rest.api.service.exceptions.BridgeOperationException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.promotion.PromotionServiceImpl;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PromotionServiceTest {

    public static final String INSTALLATION_ID = "my-installation-id";
    public static final String ORGANIZATION_ID = "my-organization-id";
    public static final String ENVIRONMENT_ID = "my-environment-id";

    private PromotionService promotionService;

    @Mock
    private CockpitService cockpitService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiService apiService;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private UserService userService;

    @Before
    public void setUp() {
        promotionService = new PromotionServiceImpl(apiService, cockpitService, promotionRepository, environmentService, userService);
    }

    @Test(expected = BridgeOperationException.class)
    public void shouldFailToListPromotionTargets() {
        // Given
        when(cockpitService.listPromotionTargets(ORGANIZATION_ID))
            .thenReturn(new CockpitReply<>(Collections.emptyList(), CockpitReplyStatus.ERROR));

        // When
        promotionService.listPromotionTargets(ORGANIZATION_ID, ENVIRONMENT_ID);
    }

    @Test
    public void shouldListPromotionTargets() {
        // Given
        PromotionTargetEntity envA = new PromotionTargetEntity();
        envA.setId("my-env-A");
        envA.setHrids(Collections.singletonList("my-env-A"));
        envA.setOrganizationId(ORGANIZATION_ID);
        envA.setName("ENV A");
        envA.setInstallationId(INSTALLATION_ID);

        PromotionTargetEntity envB = new PromotionTargetEntity();
        envB.setId("my-env-B");
        envB.setHrids(Collections.singletonList("my-env-B"));
        envB.setOrganizationId(ORGANIZATION_ID);
        envB.setName("ENV B");
        envB.setInstallationId(INSTALLATION_ID);

        PromotionTargetEntity currentEnv = new PromotionTargetEntity();
        currentEnv.setId(ENVIRONMENT_ID);
        currentEnv.setHrids(Collections.singletonList(ENVIRONMENT_ID));
        currentEnv.setOrganizationId(ORGANIZATION_ID);
        currentEnv.setName("My Environment");
        currentEnv.setInstallationId(INSTALLATION_ID);

        when(cockpitService.listPromotionTargets(ORGANIZATION_ID))
            .thenReturn(new CockpitReply<>(Arrays.asList(envA, envB, currentEnv), CockpitReplyStatus.SUCCEEDED));

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENVIRONMENT_ID);
        environmentEntity.setCockpitId(ENVIRONMENT_ID);
        environmentEntity.setHrids(Collections.singletonList(ENVIRONMENT_ID));
        when(environmentService.findById(ENVIRONMENT_ID)).thenReturn(environmentEntity);

        final List<PromotionTargetEntity> promotionTargetEntities = promotionService.listPromotionTargets(ORGANIZATION_ID, ENVIRONMENT_ID);

        assertThat(promotionTargetEntities).isNotNull();
        assertThat(promotionTargetEntities).hasSize(2);
    }

    @Test
    public void shouldCreateIfNoExistingPromotion() throws TechnicalException {
        when(promotionRepository.findById(any())).thenReturn(Optional.empty());
        when(promotionRepository.create(any())).thenReturn(getAPromotion());

        promotionService.createOrUpdate(getAPromotionEntity());

        verify(promotionRepository, times(1)).create(any());
    }

    @Test
    public void shouldUpdateIfExistingPromotion() throws TechnicalException {
        when(promotionRepository.findById(any())).thenReturn(Optional.of(new Promotion()));
        when(promotionRepository.update(any())).thenReturn(getAPromotion());

        promotionService.createOrUpdate(getAPromotionEntity());

        verify(promotionRepository, times(1)).update(any());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateOrUpdateException() throws TechnicalException {
        when(promotionRepository.findById(any())).thenThrow(new TechnicalException());

        promotionService.createOrUpdate(getAPromotionEntity());
    }

    @Test
    public void shouldSearch() throws TechnicalException {
        Page<Promotion> promotionPage = new Page<>(singletonList(getAPromotion()), 0, 1, 1);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        final Page<PromotionEntity> result = promotionService.search(any(), any(), any());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotSearchException() throws TechnicalException {
        when(promotionRepository.search(any(), any(), any())).thenThrow(new TechnicalException());

        promotionService.search(any(), any(), any());
    }

    private PromotionEntity getAPromotionEntity() {
        PromotionEntityAuthor promotionEntityAuthor = new PromotionEntityAuthor();
        promotionEntityAuthor.setUserId("user#1");
        promotionEntityAuthor.setDisplayName("User 1");
        promotionEntityAuthor.setEmail("user1@gv.io");
        promotionEntityAuthor.setSource("cockpit");
        promotionEntityAuthor.setSourceId("user1");

        final PromotionEntity promotionEntity = new PromotionEntity();
        promotionEntity.setSourceEnvCockpitId("sourceEnvId");
        promotionEntity.setSourceEnvName("sourceEnv Name");
        promotionEntity.setTargetEnvCockpitId("targetEnvId");
        promotionEntity.setTargetEnvName("targetEnv Name");
        promotionEntity.setApiDefinition("definition");
        promotionEntity.setStatus(PromotionEntityStatus.TO_BE_VALIDATED);
        promotionEntity.setAuthor(promotionEntityAuthor);

        return promotionEntity;
    }

    private Promotion getAPromotion() {
        PromotionAuthor promotionAuthor = new PromotionAuthor();
        promotionAuthor.setUserId("user#1");
        promotionAuthor.setDisplayName("User 1");
        promotionAuthor.setEmail("user1@gv.io");
        promotionAuthor.setSource("cockpit");
        promotionAuthor.setSourceId("user1");

        Promotion promotion = new Promotion();
        promotion.setCreatedAt(new Date());
        promotion.setStatus(PromotionStatus.TO_BE_VALIDATED);
        promotion.setApiDefinition("apiDefinition");
        promotion.setSourceEnvCockpitId("sourceEnvId");
        promotion.setSourceEnvName("sourceEnv Name");
        promotion.setTargetEnvCockpitId("targetEnvironmentId");
        promotion.setTargetEnvName("targetEnv Name");
        promotion.setAuthor(promotionAuthor);

        return promotion;
    }
}
