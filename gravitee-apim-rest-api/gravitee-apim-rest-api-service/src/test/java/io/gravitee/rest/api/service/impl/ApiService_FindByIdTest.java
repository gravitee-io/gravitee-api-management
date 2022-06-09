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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_FindByIdTest {

    private static final String API_ID = "id-api";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private Api api;

    @Mock
    private UserService userService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private EntrypointService entrypointService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Spy
    private ApiConverter apiConverter;

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
        GraviteeContext.setCurrentEnvironment("DEFAULT");
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        when(membershipService.getPrimaryOwner(GraviteeContext.getCurrentOrganization(), MembershipReferenceType.API, API_ID))
            .thenReturn(po);
        when(userService.findById(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(mock(UserEntity.class));

        final ApiEntity apiEntity = apiService.findById(GraviteeContext.getExecutionContext(), API_ID);

        assertNotNull(apiEntity);
    }

    @Test
    public void shouldFindById_withV2Flows() throws TechnicalException {
        ReflectionTestUtils.setField(apiConverter, "objectMapper", new ObjectMapper());

        api = new Api();
        api.setId(API_ID);
        api.setDefinition("{\"gravitee\" : \"2.0.0\", \"proxy\": {\"virtual_hosts\": []}}");
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        List<Flow> apiFlows = List.of(mock(Flow.class), mock(Flow.class));
        when(flowService.findByReference(FlowReferenceType.API, API_ID)).thenReturn(apiFlows);

        MembershipEntity po = new MembershipEntity();
        po.setMemberId(USER_NAME);
        when(membershipService.getPrimaryOwner(GraviteeContext.getCurrentOrganization(), MembershipReferenceType.API, API_ID))
            .thenReturn(po);
        when(userService.findById(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(mock(UserEntity.class));

        final ApiEntity apiEntity = apiService.findById(GraviteeContext.getExecutionContext(), API_ID);

        assertSame(apiFlows, apiEntity.getFlows());
        verify(flowService, times(1)).findByReference(FlowReferenceType.API, API_ID);
        verifyNoMoreInteractions(flowService);
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotFindByNameBecauseNotExists() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiService.findById(GraviteeContext.getExecutionContext(), API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByNameBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);

        apiService.findById(GraviteeContext.getExecutionContext(), API_ID);
    }
}
