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
package io.gravitee.rest.api.service.v4.impl.validation;

import static io.gravitee.rest.api.model.WorkflowState.IN_REVIEW;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.ARCHIVED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.DEPRECATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DefinitionVersionException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.LifecycleStateChangeNotAllowedException;
import io.gravitee.rest.api.service.v4.exception.ApiTypeException;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import io.gravitee.rest.api.service.v4.validation.GroupValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
import io.gravitee.rest.api.service.v4.validation.ResourcesValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
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
                resourcesValidationService
            );
    }

    @Test
    public void shouldCallOtherServicesWhenValidatingNewApiEntity() {
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
        apiValidationService.validateAndSanitizeNewApi(GraviteeContext.getExecutionContext(), new NewApiEntity(), primaryOwnerEntity);

        verify(tagsValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), null, null);
        verify(groupValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), null, null, primaryOwnerEntity);
        verify(listenerValidationService, times(1)).validateAndSanitize(GraviteeContext.getExecutionContext(), null, null);
        verify(endpointGroupsValidationService, times(1)).validateAndSanitize(null);
        verify(flowValidationService, times(1)).validateAndSanitize(null);
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

    @Test(expected = DefinitionVersionException.class)
    public void shouldThrowExceptionBecauseMustNotDowngradeDefinitionVersion() {
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
        existingApiEntity.setType(ApiType.ASYNC);

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
        updateApiEntity.setType(ApiType.SYNC);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.ASYNC);

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
        updateApiEntity.setType(ApiType.ASYNC);
        updateApiEntity.setLifecycleState(null);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.ASYNC);
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
        updateApiEntity.setType(ApiType.ASYNC);
        updateApiEntity.setLifecycleState(PUBLISHED);

        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.ASYNC);
        existingApiEntity.setLifecycleState(CREATED);
        existingApiEntity.setWorkflowState(IN_REVIEW);

        apiValidationService.validateAndSanitizeUpdateApi(
            GraviteeContext.getExecutionContext(),
            updateApiEntity,
            new PrimaryOwnerEntity(),
            existingApiEntity
        );
    }

    private void assertUpdate(
        final ApiLifecycleState fromLifecycleState,
        final ApiLifecycleState lifecycleState,
        final boolean shouldFail
    ) {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.ASYNC);
        updateApiEntity.setLifecycleState(lifecycleState);
        ApiEntity existingApiEntity = new ApiEntity();
        existingApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        existingApiEntity.setType(ApiType.ASYNC);
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
