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
package io.gravitee.rest.api.service.v4.impl.validation;

import static io.gravitee.rest.api.model.WorkflowState.IN_REVIEW;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.ARCHIVED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.DEPRECATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.LifecycleStateChangeNotAllowedException;
import io.gravitee.rest.api.service.v4.ApiServicePluginService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.exception.ApiTypeException;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import io.gravitee.rest.api.service.v4.validation.GroupValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import io.gravitee.rest.api.service.v4.validation.PlanValidationService;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
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
public class ApiValidationServiceImplTest {

    @Mock
    private TagsValidationService tagsValidationService;

    @Mock
    private GroupValidationService groupValidationService;

    @Mock
    private ListenerValidationService listenerValidationService;

    @Mock
    private EndpointGroupsValidationService endpointGroupsValidationService;

    @Mock
    private FlowValidationService flowValidationService;

    @Mock
    private ResourcesValidationService resourcesValidationService;

    @Mock
    private AnalyticsValidationService loggingValidationService;

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private PlanValidationService planValidationService;

    @Mock
    private PathParametersValidationService pathParametersValidationService;

    @Mock
    private ApiServicePluginService apiServicePluginService;

    private ApiValidationService apiValidationService;

    @Before
    public void setUp() throws Exception {
        apiValidationService =
            new ApiValidationServiceImpl(
                tagsValidationService,
                groupValidationService,
                listenerValidationService,
                endpointGroupsValidationService,
                flowValidationService,
                resourcesValidationService,
                loggingValidationService,
                planSearchService,
                planValidationService,
                pathParametersValidationService,
                apiServicePluginService
            );
    }

    @Test
    public void shouldCallOtherServicesWhenValidatingNewApiEntity() {
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setType(ApiType.PROXY);
        apiValidationService.validateAndSanitizeNewApi(GraviteeContext.getExecutionContext(), newApiEntity, primaryOwnerEntity);

        verify(tagsValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), null, null);
        verify(groupValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), null, null, primaryOwnerEntity);
        verify(listenerValidationService, times(1)).validateAndSanitizeHttpV4(GraviteeContext.getExecutionContext(), null, null, null);
        verify(endpointGroupsValidationService, times(1)).validateAndSanitizeHttpV4(newApiEntity.getType(), null);
        verify(loggingValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), newApiEntity.getType(), null);
        verify(flowValidationService, times(1)).validateAndSanitize(newApiEntity.getType(), null);
        verify(resourcesValidationService, never()).validateAndSanitize(any());
        verify(planValidationService, never()).validateAndSanitize(any(), any());
        verify(pathParametersValidationService, times(1)).validate(any(), any(), any());
    }

    @Test
    public void shouldCallOtherServicesWhenValidatingImportApiForCreation() {
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setLifecycleState(CREATED);
        apiValidationService.validateAndSanitizeImportApiForCreation(GraviteeContext.getExecutionContext(), apiEntity, primaryOwnerEntity);

        assertNull(apiEntity.getLifecycleState());

        verify(tagsValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), null, Set.of());
        verify(groupValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), null, null, primaryOwnerEntity);
        verify(listenerValidationService, times(1)).validateAndSanitizeHttpV4(GraviteeContext.getExecutionContext(), null, null, null);
        verify(endpointGroupsValidationService, times(1)).validateAndSanitizeHttpV4(apiEntity.getType(), null);
        verify(loggingValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), apiEntity.getType(), null);
        verify(flowValidationService, times(1)).validateAndSanitize(apiEntity.getType(), null);
        verify(resourcesValidationService, times(1)).validateAndSanitize(List.of());
        verify(planValidationService, times(1)).validateAndSanitize(apiEntity.getType(), Set.of());
        verify(pathParametersValidationService, times(1)).validate(eq(apiEntity.getType()), any(Stream.class), any(Stream.class));
    }

    @Test(expected = InvalidDataException.class)
    public void shouldValidateNewApiThrowExceptionWhenDefinitionVersionNull() {
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setDefinitionVersion(null);

        apiValidationService.validateAndSanitizeNewApi(GraviteeContext.getExecutionContext(), newApiEntity, new PrimaryOwnerEntity());
    }

    @Test(expected = InvalidDataException.class)
    public void shouldValidateNewApiThrowExceptionWhenApiTypeNull() {
        NewApiEntity newApiEntity = new NewApiEntity();
        apiValidationService.validateAndSanitizeNewApi(GraviteeContext.getExecutionContext(), newApiEntity, new PrimaryOwnerEntity());
    }

    @Test(expected = InvalidDataException.class)
    public void shouldThrowExceptionBecauseMustNotSetDefinitionVersionToNull() {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(null);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);

        apiValidationService.validateAndSanitizeUpdateApi(
            GraviteeContext.getExecutionContext(),
            updateApiEntity,
            new PrimaryOwnerEntity(),
            existingApiEntity
        );
    }

    @Test(expected = InvalidDataException.class)
    public void shouldThrowExceptionBecauseDefinitionVersionNotV4() {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V2);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);

        apiValidationService.validateAndSanitizeUpdateApi(
            GraviteeContext.getExecutionContext(),
            updateApiEntity,
            new PrimaryOwnerEntity(),
            existingApiEntity
        );
    }

    @Test(expected = InvalidDataException.class)
    public void shouldThrowExceptionBecauseMustNotSetApiTypeToNull() {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(null);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.MESSAGE);

        apiValidationService.validateAndSanitizeUpdateApi(
            GraviteeContext.getExecutionContext(),
            updateApiEntity,
            new PrimaryOwnerEntity(),
            existingApiEntity
        );
    }

    @Test(expected = ApiTypeException.class)
    public void shouldThrowExceptionBecauseMustNotChangeApiType() {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.PROXY);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.MESSAGE);

        apiValidationService.validateAndSanitizeUpdateApi(
            GraviteeContext.getExecutionContext(),
            updateApiEntity,
            new PrimaryOwnerEntity(),
            existingApiEntity
        );
    }

    @Test
    public void shouldReUseExistingLifecycleIfNotProvided() {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.MESSAGE);
        updateApiEntity.setLifecycleState(null);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.MESSAGE);
        existingApiEntity.setLifecycleState(CREATED);

        apiValidationService.validateAndSanitizeUpdateApi(
            GraviteeContext.getExecutionContext(),
            updateApiEntity,
            new PrimaryOwnerEntity(),
            existingApiEntity
        );
        assertEquals(CREATED, updateApiEntity.getLifecycleState());
    }

    @Test
    public void shouldNotChangeLifecycleStateFromUnpublishedToCreated() {
        assertUpdate(UNPUBLISHED, CREATED, true);
        assertUpdate(UNPUBLISHED, PUBLISHED, false);
        assertUpdate(UNPUBLISHED, UNPUBLISHED, false);
        assertUpdate(UNPUBLISHED, ARCHIVED, false);
    }

    @Test
    public void shouldNotUpdateADeprecatedApi() {
        assertUpdate(DEPRECATED, CREATED, true);
        assertUpdate(DEPRECATED, PUBLISHED, true);
        assertUpdate(DEPRECATED, UNPUBLISHED, true);
        assertUpdate(DEPRECATED, ARCHIVED, true);
        assertUpdate(DEPRECATED, DEPRECATED, true);
    }

    @Test
    public void shouldNotChangeLifecycleStateFromArchived() {
        assertUpdate(ARCHIVED, CREATED, true);
        assertUpdate(ARCHIVED, PUBLISHED, true);
        assertUpdate(ARCHIVED, UNPUBLISHED, true);
        assertUpdate(ARCHIVED, DEPRECATED, true);
    }

    @Test(expected = LifecycleStateChangeNotAllowedException.class)
    public void shouldNotChangeLifecycleStateFromCreatedInReview() throws TechnicalException {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.MESSAGE);
        updateApiEntity.setLifecycleState(PUBLISHED);

        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.MESSAGE);
        existingApiEntity.setLifecycleState(CREATED);
        existingApiEntity.setWorkflowState(IN_REVIEW);

        apiValidationService.validateAndSanitizeUpdateApi(
            GraviteeContext.getExecutionContext(),
            updateApiEntity,
            new PrimaryOwnerEntity(),
            existingApiEntity
        );
    }

    @Test
    public void canDeployWithOnePublishedPlan() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final String apiId = "api-id";

        when(planSearchService.findByApi(executionContext, apiId))
            .thenReturn(Set.of(PlanEntity.builder().status(PlanStatus.PUBLISHED).build()));
        assertTrue(apiValidationService.canDeploy(executionContext, apiId));
    }

    @Test
    public void canDeployWithOneDeprecatedPlan() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final String apiId = "api-id";

        when(planSearchService.findByApi(executionContext, apiId))
            .thenReturn(Set.of(PlanEntity.builder().status(PlanStatus.DEPRECATED).build()));
        assertTrue(apiValidationService.canDeploy(executionContext, apiId));
    }

    @Test
    public void cannotDeployWithNoPlan() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final String apiId = "api-id";

        when(planSearchService.findByApi(executionContext, apiId)).thenReturn(Set.of());
        assertFalse(apiValidationService.canDeploy(executionContext, apiId));
    }

    @Test
    public void cannotDeployWithNoActivePlan() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final String apiId = "api-id";

        when(planSearchService.findByApi(executionContext, apiId))
            .thenReturn(
                Set.of(PlanEntity.builder().status(PlanStatus.STAGING).build(), PlanEntity.builder().status(PlanStatus.CLOSED).build())
            );
        assertFalse(apiValidationService.canDeploy(executionContext, apiId));
    }

    @Test
    public void shouldSanitizeApiDefinitionOnUpdate() {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.MESSAGE);
        updateApiEntity.setDescription("\"A<img src=\\\"../../../image.png\\\"> Description\"");

        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDescription("Old description");
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.MESSAGE);
        existingApiEntity.setLifecycleState(CREATED);

        assertEquals("\"A Description\"", updateApiEntity.getDescription());

        apiValidationService.validateAndSanitizeUpdateApi(
            GraviteeContext.getExecutionContext(),
            updateApiEntity,
            new PrimaryOwnerEntity(),
            existingApiEntity
        );
        assertEquals("\"A Description\"", updateApiEntity.getDescription());
    }

    @Test
    public void shouldSanitizeApiDefinitionOnCreateFromImport() {
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setLifecycleState(CREATED);
        apiEntity.setDescription("\"A<img src=\\\"../../../image.png\\\"> Description\"");

        apiValidationService.validateAndSanitizeImportApiForCreation(GraviteeContext.getExecutionContext(), apiEntity, primaryOwnerEntity);

        assertEquals("\"A Description\"", apiEntity.getDescription());
    }

    @Test
    public void shouldSanitizeApiDefinitionOnCreate() {
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setType(ApiType.PROXY);
        newApiEntity.setDescription("\"A<img src=\\\"../../../image.png\\\"> Description\"");

        apiValidationService.validateAndSanitizeNewApi(GraviteeContext.getExecutionContext(), newApiEntity, primaryOwnerEntity);

        assertEquals("\"A Description\"", newApiEntity.getDescription());
    }

    private void assertUpdate(
        final ApiLifecycleState fromLifecycleState,
        final ApiLifecycleState lifecycleState,
        final boolean shouldFail
    ) {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.MESSAGE);
        updateApiEntity.setLifecycleState(lifecycleState);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.MESSAGE);
        existingApiEntity.setLifecycleState(fromLifecycleState);

        boolean failed = false;
        try {
            apiValidationService.validateAndSanitizeUpdateApi(
                GraviteeContext.getExecutionContext(),
                updateApiEntity,
                new PrimaryOwnerEntity(),
                existingApiEntity
            );
        } catch (final LifecycleStateChangeNotAllowedException ise) {
            failed = true;
        }
        if (!failed && shouldFail) {
            fail("Should not be possible to change the lifecycle state of a " + fromLifecycleState + " API to " + lifecycleState);
        }
    }
}
