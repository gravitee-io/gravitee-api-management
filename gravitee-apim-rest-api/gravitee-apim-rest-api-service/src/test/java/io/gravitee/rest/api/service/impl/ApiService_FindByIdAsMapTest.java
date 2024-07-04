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

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_FindByIdAsMapTest {

    @InjectMocks
    private ApiServiceImpl apiService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private UserService userService;

    @Mock
    private ObjectMapper objectMapper;

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

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper(mock(CategoryService.class));

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(
        new ApiConverter(objectMapper, planService, flowService, categoryMapper, parameterService, mock(WorkflowService.class))
    );

    @Test
    public void shouldFindByIdAsMap() throws TechnicalException {
        Api api = new Api();
        api.setId("api-id");
        api.setName("test api");
        api.setEnvironmentId("env-id");

        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setId("env-id");
        environment.setOrganizationId("org-id");

        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId("member-id");

        when(apiRepository.findById("api-id")).thenReturn(Optional.of(api));
        when(environmentService.findById("env-id")).thenReturn(environment);
        when(objectMapper.convertValue(any(), any(Map.class.getClass())))
            .thenAnswer(i -> getObjectMapper().convertValue(i.getArgument(0), Map.class));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity(userEntity);
        when(primaryOwnerService.getPrimaryOwner(any(), any())).thenReturn(primaryOwner);
        when(apiMetadataService.findAllByApi("api-id")).thenReturn(buildMetadatas());

        Map resultMap = apiService.findByIdAsMap("api-id");

        assertEquals("api-id", resultMap.get("id"));
        assertEquals("test api", resultMap.get("name"));
        assertNotNull(resultMap.get("primaryOwner"));
        assertEquals(
            Map.of("metadata3", "metadataValue3", "metadata2", "metadataValue2", "metadata1", "metadataValue1"),
            resultMap.get("metadata")
        );

        // ensure those API data have been explicitly removed from map
        assertFalse(resultMap.containsKey("picture"));
        assertFalse(resultMap.containsKey("proxy"));
        assertFalse(resultMap.containsKey("paths"));
        assertFalse(resultMap.containsKey("properties"));
        assertFalse(resultMap.containsKey("services"));
        assertFalse(resultMap.containsKey("resources"));
        assertFalse(resultMap.containsKey("response_templates"));
        assertFalse(resultMap.containsKey("path_mappings"));
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

    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", new ApiPermissionFilter()))
        );
        return objectMapper;
    }
}
