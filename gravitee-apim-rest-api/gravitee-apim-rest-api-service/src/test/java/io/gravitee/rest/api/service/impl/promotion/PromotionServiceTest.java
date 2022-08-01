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
package io.gravitee.rest.api.service.impl.promotion;

import static io.gravitee.repository.management.model.Promotion.AuditEvent.PROMOTION_CREATED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PromotionRepository;
import io.gravitee.repository.management.model.Promotion;
import io.gravitee.repository.management.model.PromotionAuthor;
import io.gravitee.repository.management.model.PromotionStatus;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityAuthor;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.model.promotion.PromotionRequestEntity;
import io.gravitee.rest.api.model.promotion.PromotionTargetEntity;
import io.gravitee.rest.api.model.v4.api.IndexableApi;
import io.gravitee.rest.api.service.ApiDuplicatorService;
import io.gravitee.rest.api.service.ApiExportService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.cockpit.services.CockpitPromotionService;
import io.gravitee.rest.api.service.cockpit.services.CockpitReply;
import io.gravitee.rest.api.service.cockpit.services.CockpitReplyStatus;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.BridgeOperationException;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.PromotionAlreadyInProgressException;
import io.gravitee.rest.api.service.exceptions.PromotionNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
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
    public static final String PROMOTION_ID = "my-promotion-id";
    public static final String USER_ID = "my-user-id";

    @InjectMocks
    private PromotionServiceImpl promotionService;

    @Mock
    private CockpitPromotionService cockpitPromotionService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiService apiService;

    @Mock
    private ApiDuplicatorService apiDuplicatorService;

    @Mock
    private ApiExportService apiExportService;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private UserService userService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectMapper objectMapper;

    @Test(expected = BridgeOperationException.class)
    public void shouldFailToListPromotionTargets() {
        // Given
        when(cockpitPromotionService.listPromotionTargets(ORGANIZATION_ID, ENVIRONMENT_ID))
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

        when(cockpitPromotionService.listPromotionTargets(ORGANIZATION_ID, ENVIRONMENT_ID))
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

    @Test
    public void shouldProcessAcceptedPromotionCreateApi() throws Exception {
        final Promotion promotion = getAPromotion();
        final EnvironmentEntity targetEnvironment = new EnvironmentEntity();
        targetEnvironment.setId("target-env-id");

        when(objectMapper.readTree(anyString())).thenReturn(mock(ObjectNode.class));
        when(promotionRepository.findById(any())).thenReturn(Optional.of(promotion));
        when(environmentService.findByCockpitId(any())).thenReturn(targetEnvironment);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        Page<Promotion> promotionPage = new Page<>(emptyList(), 0, 1, 1);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        when(apiDuplicatorService.createWithImportedDefinition(any(), any())).thenReturn(new ApiEntity());

        CockpitReply<PromotionEntity> cockpitReply = new CockpitReply<>(null, CockpitReplyStatus.SUCCEEDED);
        when(
            cockpitPromotionService.processPromotion(
                argThat(context -> targetEnvironment.getId().equals(context.getEnvironmentId())),
                any()
            )
        )
            .thenReturn(cockpitReply);

        when(promotionRepository.update(any())).thenReturn(promotion);

        promotionService.processPromotion(GraviteeContext.getExecutionContext(), PROMOTION_ID, true);

        verify(apiDuplicatorService, times(1)).createWithImportedDefinition(any(), any());
        verify(promotionRepository, times(1)).update(any());
    }

    @Test
    public void shouldProcessAcceptedPromotionUpdateApi() throws Exception {
        final Promotion promotion = getAPromotion();
        final EnvironmentEntity targetEnvironment = new EnvironmentEntity();
        targetEnvironment.setId("target-env-id");

        when(objectMapper.readTree(anyString())).thenReturn(mock(ObjectNode.class));
        when(promotionRepository.findById(any())).thenReturn(Optional.of(promotion));
        when(environmentService.findByCockpitId(any())).thenReturn(targetEnvironment);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        Page<Promotion> promotionPage = new Page<>(singletonList(promotion), 0, 1, 1);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        when(apiDuplicatorService.updateWithImportedDefinition(any(), any(), any())).thenReturn(new ApiEntity());
        when(apiService.exists(any())).thenReturn(true);

        ApiEntity existingApi = new ApiEntity();
        existingApi.setId("api#existing");
        when(apiService.findIndexableApiById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(existingApi);

        CockpitReply<PromotionEntity> cockpitReply = new CockpitReply<>(null, CockpitReplyStatus.SUCCEEDED);
        when(
            cockpitPromotionService.processPromotion(
                argThat(context -> targetEnvironment.getId().equals(context.getEnvironmentId())),
                any()
            )
        )
            .thenReturn(cockpitReply);

        when(promotionRepository.update(any())).thenReturn(promotion);

        promotionService.processPromotion(GraviteeContext.getExecutionContext(), PROMOTION_ID, true);

        verify(apiDuplicatorService, times(1)).updateWithImportedDefinition(any(), any(), any());
        verify(promotionRepository, times(1)).update(any());
    }

    @Test
    public void shouldProcessRejectedPromotion() throws Exception {
        final Promotion promotion = getAPromotion();
        final EnvironmentEntity targetEnvironment = new EnvironmentEntity();
        targetEnvironment.setId("target-env-id");

        when(objectMapper.readTree(anyString())).thenReturn(mock(ObjectNode.class));
        when(promotionRepository.findById(any())).thenReturn(Optional.of(promotion));
        when(environmentService.findByCockpitId(any())).thenReturn(targetEnvironment);

        Page<Promotion> promotionPage = new Page<>(singletonList(promotion), 0, 1, 1);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        CockpitReply<PromotionEntity> cockpitReply = new CockpitReply<>(null, CockpitReplyStatus.SUCCEEDED);
        when(
            cockpitPromotionService.processPromotion(
                argThat(context -> targetEnvironment.getId().equals(context.getEnvironmentId())),
                any()
            )
        )
            .thenReturn(cockpitReply);

        when(promotionRepository.update(any())).thenReturn(promotion);

        promotionService.processPromotion(GraviteeContext.getExecutionContext(), PROMOTION_ID, false);

        verify(apiDuplicatorService, never()).createWithImportedDefinition(any(), any());
        verify(apiDuplicatorService, never()).updateWithImportedDefinition(any(), any(), any());
        verify(promotionRepository, times(1)).update(any());
    }

    @Test(expected = PromotionNotFoundException.class)
    public void shouldNotProcessPromotionIfPromotionNotFound() throws Exception {
        when(promotionRepository.findById(any())).thenReturn(Optional.empty());

        promotionService.processPromotion(GraviteeContext.getExecutionContext(), PROMOTION_ID, true);
    }

    @Test(expected = ForbiddenAccessException.class)
    public void shouldNotProcessPromotionIfNoPermissionForTargetEnvironment() throws Exception {
        when(objectMapper.readTree(anyString())).thenReturn(mock(ObjectNode.class));
        when(promotionRepository.findById(any())).thenReturn(Optional.of(getAPromotion()));
        when(environmentService.findByCockpitId(any())).thenReturn(new EnvironmentEntity());
        when(apiService.exists(any())).thenReturn(true);
        when(apiService.findIndexableApiById(any(), any())).thenReturn(mock(IndexableApi.class));
        Page<Promotion> promotionPage = new Page<>(singletonList(getAPromotion()), 0, 1, 1);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);

        promotionService.processPromotion(GraviteeContext.getExecutionContext(), PROMOTION_ID, true);
    }

    @Test(expected = BridgeOperationException.class)
    public void shouldNotProcessPromotionIfCockpitReplyError() throws Exception {
        final Promotion promotion = getAPromotion();
        final EnvironmentEntity targetEnvironment = new EnvironmentEntity();
        targetEnvironment.setId("target-env-id");

        when(objectMapper.readTree(anyString())).thenReturn(mock(ObjectNode.class));
        when(promotionRepository.findById(any())).thenReturn(Optional.of(getAPromotion()));
        when(environmentService.findByCockpitId(any())).thenReturn(targetEnvironment);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        Page<Promotion> promotionPage = new Page<>(singletonList(getAPromotion()), 0, 1, 1);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        when(apiService.exists(any())).thenReturn(true);
        when(apiDuplicatorService.updateWithImportedDefinition(any(), any(), any())).thenReturn(new ApiEntity());

        ApiEntity existingApi = new ApiEntity();
        existingApi.setId("api#existing");
        when(apiService.findIndexableApiById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(existingApi);

        CockpitReply<PromotionEntity> cockpitReply = new CockpitReply<>(null, CockpitReplyStatus.ERROR);

        when(
            cockpitPromotionService.processPromotion(
                argThat(context -> targetEnvironment.getId().equals(context.getEnvironmentId())),
                any()
            )
        )
            .thenReturn(cockpitReply);

        promotionService.processPromotion(GraviteeContext.getExecutionContext(), PROMOTION_ID, true);

        verify(apiDuplicatorService, times(1)).updateWithImportedDefinition(any(), any(), any());
        verify(promotionRepository, times(0)).update(any());
    }

    @Test
    public void shouldPromote() throws TechnicalException {
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(getAUserEntity());
        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setCockpitId("env#cockpit-1");
        environmentEntity.setName("Env 1");
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        Page<Promotion> promotionPage = new Page<>(emptyList(), 0, 0, 0);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        when(promotionRepository.create(any())).thenReturn(getAPromotion());
        when(cockpitPromotionService.requestPromotion(eq(GraviteeContext.getExecutionContext()), any()))
            .thenReturn(new CockpitReply<>(getAPromotionEntity(), CockpitReplyStatus.SUCCEEDED));

        when(promotionRepository.update(any())).thenReturn(mock(Promotion.class));

        final PromotionEntity promotionEntity = promotionService.promote(
            GraviteeContext.getExecutionContext(),
            GraviteeContext.getCurrentEnvironment(),
            "api#1",
            getAPromotionRequestEntity(),
            "user#1"
        );

        assertThat(promotionEntity).isNotNull();

        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq("api#1"),
                any(),
                eq(PROMOTION_CREATED),
                any(),
                isNull(),
                any()
            );
    }

    @Test(expected = PromotionAlreadyInProgressException.class)
    public void shouldNotPromoteIfThereIsAlreadyAnInProgressPromotionForTheSameEnv() throws TechnicalException {
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(getAUserEntity());
        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setCockpitId("env#cockpit-1");
        environmentEntity.setName("Env 1");
        when(environmentService.findById(any())).thenReturn(environmentEntity);

        String targetEnvCockpitId = "env#cockpit-target";
        Promotion promotion = getAPromotion();
        promotion.setApiId("api#1");
        promotion.setStatus(PromotionStatus.TO_BE_VALIDATED);
        promotion.setTargetEnvCockpitId(targetEnvCockpitId);

        Promotion promotion2 = getAPromotion();
        promotion2.setApiId("api#1");
        promotion2.setStatus(PromotionStatus.TO_BE_VALIDATED);
        promotion2.setTargetEnvCockpitId("env#another-env");
        Page<Promotion> promotionPage = new Page<>(List.of(promotion, promotion2), 0, 2, 2);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        PromotionRequestEntity promotionRequestEntity = getAPromotionRequestEntity();
        promotionRequestEntity.setTargetEnvCockpitId(targetEnvCockpitId);
        promotionService.promote(
            GraviteeContext.getExecutionContext(),
            GraviteeContext.getCurrentEnvironment(),
            "api#1",
            promotionRequestEntity,
            "user#1"
        );
    }

    @Test
    public void findAlreadyPromotedTargetApi_shouldFindApiByCrossId() throws Exception {
        JsonNode apiDefinition = new ObjectMapper().readTree("{\"crossId\": \"test-cross-id\"}");

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("env-id");

        // API is found by crossId
        String apiIdFoundByCrossId = UUID.randomUUID().toString();
        when(apiService.findApiIdByEnvironmentIdAndCrossId("env-id", "test-cross-id")).thenReturn(Optional.of(apiIdFoundByCrossId));

        String resultApiId = promotionService.findAlreadyPromotedTargetApi(
            GraviteeContext.getExecutionContext(),
            environment,
            new Promotion(),
            apiDefinition
        );

        assertSame(resultApiId, apiIdFoundByCrossId);
    }

    @Test
    public void findAlreadyPromotedTargetApi_shouldFindApiInLastPromotion() throws Exception {
        JsonNode apiDefinition = new ObjectMapper().readTree("{\"crossId\": \"test-cross-id\"}");

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("env-id");

        // API is not found by crossId
        when(apiService.findApiIdByEnvironmentIdAndCrossId("env-id", "test-cross-id")).thenReturn(Optional.empty());

        // Last promotions of this API are found
        Promotion aPromotion = getAPromotion();
        Page<Promotion> promotionPage = new Page<>(singletonList(aPromotion), 0, 1, 1);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        // Target API of last promotion is found
        IndexableApi apiFromLastPromotion = mock(IndexableApi.class);
        when(apiFromLastPromotion.getId()).thenReturn(aPromotion.getApiId());
        when(apiService.exists(aPromotion.getTargetApiId())).thenReturn(true);
        when(apiService.findIndexableApiById(GraviteeContext.getExecutionContext(), "api#1-Promoted")).thenReturn(apiFromLastPromotion);

        String resultApi = promotionService.findAlreadyPromotedTargetApi(
            GraviteeContext.getExecutionContext(),
            environment,
            aPromotion,
            apiDefinition
        );

        assertSame(resultApi, aPromotion.getApiId());
    }

    @Test
    public void findAlreadyPromotedTargetApi_shouldReturnNull_causeNoPreviousPromotion() throws Exception {
        JsonNode apiDefinition = new ObjectMapper().readTree("{\"crossId\": \"test-cross-id\"}");

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("env-id");

        // API is not found by crossId
        when(apiService.findApiIdByEnvironmentIdAndCrossId("env-id", "test-cross-id")).thenReturn(Optional.empty());

        // Searching for previous promotions of this API returns an empty list
        Page<Promotion> promotionPage = new Page<>(emptyList(), 0, 0, 0);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        String resultApiId = promotionService.findAlreadyPromotedTargetApi(
            GraviteeContext.getExecutionContext(),
            environment,
            getAPromotion(),
            apiDefinition
        );

        assertNull(resultApiId);
    }

    @Test
    public void findAlreadyPromotedTargetApi_shouldReturnNull_causePreviousPromotionPointsToNonExistingApi() throws Exception {
        JsonNode apiDefinition = new ObjectMapper().readTree("{\"crossId\": \"test-cross-id\"}");

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("env-id");

        // API is not found by crossId
        when(apiService.findApiIdByEnvironmentIdAndCrossId("env-id", "test-cross-id")).thenReturn(Optional.empty());

        // Last promotions of this API are found
        Promotion aPromotion = getAPromotion();
        Page<Promotion> promotionPage = new Page<>(singletonList(aPromotion), 0, 1, 1);
        when(promotionRepository.search(any(), any(), any())).thenReturn(promotionPage);

        // Target API of last promotion is not found
        when(apiService.exists(aPromotion.getTargetApiId())).thenReturn(false);

        String resultApiId = promotionService.findAlreadyPromotedTargetApi(
            GraviteeContext.getExecutionContext(),
            environment,
            aPromotion,
            apiDefinition
        );

        assertNull(resultApiId);
    }

    private UserEntity getAUserEntity() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user#1");
        userEntity.setEmail("user@gv.io");
        userEntity.setPicture("http://image.png");
        userEntity.setSource("cockpit");
        userEntity.setSource("id");

        return userEntity;
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
        promotionEntity.setApiId("api#1");
        promotionEntity.setTargetApiId("api#1-Promoted");
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
        promotion.setApiId("api#1");
        promotion.setTargetApiId("api#1-Promoted");
        promotion.setApiDefinition("apiDefinition");
        promotion.setSourceEnvCockpitId("sourceEnvId");
        promotion.setSourceEnvName("sourceEnv Name");
        promotion.setTargetEnvCockpitId("targetEnvironmentId");
        promotion.setTargetEnvName("targetEnv Name");
        promotion.setAuthor(promotionAuthor);

        return promotion;
    }

    private PromotionRequestEntity getAPromotionRequestEntity() {
        PromotionRequestEntity promotionRequestEntity = new PromotionRequestEntity();
        promotionRequestEntity.setTargetEnvCockpitId("targetEnvironmentId");
        promotionRequestEntity.setTargetEnvName("targetEnv Name");

        return promotionRequestEntity;
    }
}
