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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.infra.crud_service.flow.FlowCrudServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.mapper.FlowMapper;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class FlowServiceImplTest {

    private FlowService flowService;

    private FlowCrudService flowCrudService;

    @Mock
    private FlowRepository flowRepository;

    @Mock
    private TagService tagService;

    @Before
    public void before() {
        flowService = new FlowServiceImpl(flowRepository, tagService, new FlowMapper());
        flowCrudService = new FlowCrudServiceImpl(flowRepository);
    }

    @Test
    public void shouldGetConfigurationSchemaForm() {
        String apiFlowSchemaForm = flowService.getApiFlowSchemaForm();
        assertNotNull(apiFlowSchemaForm);
    }

    @Test
    public void shouldGetApiFlowSchemaForm() {
        String configurationSchemaForm = flowService.getConfigurationSchemaForm();
        assertNotNull(configurationSchemaForm);
    }

    @Test
    public void shouldReturnFlowsInOrder() throws TechnicalException {
        Flow flow5 = new Flow();
        flow5.setName("flow5");
        flow5.setOrder(5);
        Flow flow1 = new Flow();
        flow1.setName("flow1");
        flow1.setOrder(1);

        when(flowRepository.findByReference(FlowReferenceType.API, "apiId")).thenReturn(List.of(flow5, flow1));
        List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = flowService.findByReference(
            FlowReferenceType.API,
            "apiId"
        );
        assertThat(flowServiceByReference).isNotNull();
        assertThat(flowServiceByReference.size()).isEqualTo(2);
        assertThat(flowServiceByReference.get(0).getName()).isEqualTo("flow1");
        assertThat(flowServiceByReference.get(1).getName()).isEqualTo("flow5");
        verify(flowRepository).findByReference(FlowReferenceType.API, "apiId");
    }

    @Test
    public void shouldDeleteAndSaveNewFlows() throws TechnicalException {
        io.gravitee.definition.model.v4.flow.Flow flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setName("flow1");
        io.gravitee.definition.model.v4.flow.Flow flow2 = new io.gravitee.definition.model.v4.flow.Flow();
        flow2.setName("flow2");

        when(flowRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = flowCrudService.saveApiFlows(
            "apiId",
            List.of(flow1, flow2)
        );
        assertThat(flowServiceByReference).isNotNull();
        assertThat(flowServiceByReference.size()).isEqualTo(2);
        assertThat(flowServiceByReference.get(0).getName()).isEqualTo("flow1");
        assertThat(flowServiceByReference.get(1).getName()).isEqualTo("flow2");

        verify(flowRepository, never()).deleteByReferenceIdAndReferenceType("apiId", FlowReferenceType.API);
        verify(flowRepository, times(2)).create(any());
    }
}
