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
package io.gravitee.rest.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.BadNotificationConfigException;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class ApiService_FindByIdAsMapTest {

    public static final String ENV_ID = "env-id";
    public static final String ORG_ID = "org-id";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ApiServiceImpl apiService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiSearchService apiSearchService;

    @BeforeEach
    void setup() {
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", new ApiPermissionFilter()))
        );
        CategoryMapper categoryMapper = spy(new CategoryMapper(mock(CategoryService.class)));
        ApiConverter apiConverter = Mockito.spy(
            new ApiConverter(objectMapper, planService, flowService, categoryMapper, parameterService, mock(WorkflowService.class))
        );
        apiService.setApiConverter(apiConverter);
        apiService.setObjectMapper(objectMapper);
    }

    @Test
    void shouldFindByIdAsMap() throws TechnicalException {
        Api api = new Api();
        api.setId("api-id");
        api.setName("test api");
        api.setEnvironmentId(ENV_ID);
        api.setDefinitionVersion(DefinitionVersion.V2);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENV_ID);
        environment.setOrganizationId(ORG_ID);

        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId("member-id");

        when(apiRepository.findById("api-id")).thenReturn(Optional.of(api));
        when(environmentService.findById(ENV_ID)).thenReturn(environment);
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        when(primaryOwnerService.getPrimaryOwner(any(), any())).thenReturn(primaryOwner);
        when(apiMetadataService.findAllByApi(new ExecutionContext(environment), "api-id")).thenReturn(buildMetadatas());

        var resultMap = apiService.findByIdAsMap("api-id");

        assertThat(resultMap)
            .containsEntry("id", "api-id")
            .containsEntry("name", "test api")
            .containsKey("primaryOwner")
            .containsEntry("metadata", Map.of("metadata3", "metadataValue3", "metadata2", "metadataValue2", "metadata1", "metadataValue1"))
            // ensure those API data have been explicitly removed from map
            .doesNotContainKeys("picture", "proxy", "paths", "properties", "services", "resources", "response_templates", "path_mappings");
    }

    @Test
    void shouldDefaultToV2WhenDefinitionVersionIsNull() throws TechnicalException {
        Api api = new Api();
        api.setId("api-id");
        api.setName("test api with null definitionVersion");
        api.setEnvironmentId(ENV_ID);
        api.setDefinitionVersion(null);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENV_ID);
        environment.setOrganizationId(ORG_ID);

        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId("member-id");

        when(apiRepository.findById("api-id")).thenReturn(Optional.of(api));
        when(environmentService.findById(ENV_ID)).thenReturn(environment);

        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        when(primaryOwnerService.getPrimaryOwner(any(), any())).thenReturn(primaryOwner);
        when(apiMetadataService.findAllByApi(new ExecutionContext(environment), "api-id")).thenReturn(buildMetadatas());
        var resultMap = apiService.findByIdAsMap("api-id");

        assertThat(resultMap)
            .containsEntry("id", "api-id")
            .containsEntry("name", "test api with null definitionVersion")
            .containsKey("primaryOwner")
            .containsEntry("metadata", Map.of("metadata3", "metadataValue3", "metadata2", "metadataValue2", "metadata1", "metadataValue1"))
            .doesNotContainKeys("picture", "proxy", "paths", "properties", "services", "resources", "response_templates", "path_mappings");

        assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
    }

    @Test
    void shouldFindByIdAsMapWithV4Definition() throws TechnicalException {
        Api api = new Api();
        api.setId("api-id-v4");
        api.setName("test api v4");
        api.setEnvironmentId(ENV_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId(ENV_ID);
        environment.setOrganizationId(ORG_ID);

        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");

        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);

        io.gravitee.rest.api.model.v4.api.ApiEntity mockApiEntity = mock(io.gravitee.rest.api.model.v4.api.ApiEntity.class);
        when(mockApiEntity.getId()).thenReturn("api-id-v4");
        when(mockApiEntity.getName()).thenReturn("test api v4");
        when(mockApiEntity.getDefinitionVersion()).thenReturn(DefinitionVersion.V4);
        when(mockApiEntity.getPrimaryOwner()).thenReturn(primaryOwner);

        when(apiRepository.findById("api-id-v4")).thenReturn(Optional.of(api));
        when(environmentService.findById(ENV_ID)).thenReturn(environment);
        when(apiSearchService.findById(new ExecutionContext(environment), "api-id-v4")).thenReturn(mockApiEntity);
        when(apiMetadataService.findAllByApi(new ExecutionContext(environment), "api-id-v4")).thenReturn(buildMetadatas());

        var resultMap = apiService.findByIdAsMap("api-id-v4");

        assertThat(resultMap)
            .containsEntry("id", "api-id-v4")
            .containsEntry("name", "test api v4")
            .containsEntry("definitionVersion", "4.0.0")
            .containsKey("primaryOwner")
            .containsEntry("metadata", Map.of("metadata3", "metadataValue3", "metadata2", "metadataValue2", "metadata1", "metadataValue1"))
            .doesNotContainKeys("picture", "proxy", "paths", "properties", "services", "resources", "response_templates", "path_mappings");
        verifyNoMoreInteractions(apiSearchService);
    }

    private List<ApiMetadataEntity> buildMetadatas() {
        return List.of(
            buildMetadata("metadata1", "metadataValue1"),
            buildMetadata("metadata2", "metadataValue2"),
            buildMetadata("metadata3", "metadataValue3")
        );
    }

    private ApiMetadataEntity buildMetadata(String key, String value) {
        ApiMetadataEntity apiMetadataEntity = new ApiMetadataEntity();
        apiMetadataEntity.setKey(key);
        apiMetadataEntity.setValue(value);
        return apiMetadataEntity;
    }
}
