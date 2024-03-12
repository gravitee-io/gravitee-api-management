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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.mapper.FlowMapper;
import java.util.List;
import java.util.Set;
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

    @Mock
    private FlowRepository flowRepository;

    @Mock
    private TagService tagService;

    @Before
    public void before() {
        flowService = new FlowServiceImpl(flowRepository, tagService, new FlowMapper());
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
    public void shouldSaveNewFlows() throws TechnicalException {
        io.gravitee.definition.model.v4.flow.Flow flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setName("flow1");
        io.gravitee.definition.model.v4.flow.Flow flow2 = new io.gravitee.definition.model.v4.flow.Flow();
        flow2.setName("flow2");
        String referenceId = "apiId";

        when(flowRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = flowService.save(
            FlowReferenceType.API,
            referenceId,
            List.of(flow1, flow2)
        );
        assertThat(flowServiceByReference).isNotNull();
        assertThat(flowServiceByReference.size()).isEqualTo(2);
        assertThat(flowServiceByReference.get(0).getName()).isEqualTo("flow1");
        assertThat(flowServiceByReference.get(1).getName()).isEqualTo("flow2");

        verify(flowRepository, never()).deleteByReference(FlowReferenceType.API, referenceId);
        verify(flowRepository, never()).delete(anyString());
        verify(flowRepository, times(2)).create(any());
    }

    @Test
    public void shouldDeleteOneExistingAndCreateNewFlow() throws TechnicalException {
        FlowMapper flowMapper = new FlowMapper();
        String referenceId = "apiId";
        io.gravitee.definition.model.v4.flow.Flow flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setId("id1");
        flow1.setName("flow1");
        Flow repoFlow1 = flowMapper.toRepository(flow1, FlowReferenceType.API, referenceId, 0);
        repoFlow1.setId(flow1.getId());
        io.gravitee.definition.model.v4.flow.Flow flow2 = new io.gravitee.definition.model.v4.flow.Flow();
        flow2.setName("flow2");

        when(flowRepository.findByReference(any(), eq(referenceId))).thenAnswer(invocation -> List.of(repoFlow1));
        when(flowRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = flowService.save(
            FlowReferenceType.API,
            referenceId,
            List.of(flow2)
        );
        assertThat(flowServiceByReference).isNotNull();
        assertThat(flowServiceByReference.size()).isEqualTo(1);
        assertThat(flowServiceByReference.get(0).getName()).isEqualTo("flow2");

        verify(flowRepository, never()).deleteByReference(FlowReferenceType.API, referenceId);
        verify(flowRepository, times(1)).deleteAllById(Set.of(flow1.getId()));
        verify(flowRepository, times(1)).create(any());
    }

    @Test
    public void shouldDeleteMultipleExistingAndCreateNewFlow() throws TechnicalException {
        FlowMapper flowMapper = new FlowMapper();
        String referenceId = "apiId";
        io.gravitee.definition.model.v4.flow.Flow flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setId("id1");
        flow1.setName("flow1");
        Flow repoFlow1 = flowMapper.toRepository(flow1, FlowReferenceType.API, referenceId, 0);
        repoFlow1.setId(flow1.getId());
        io.gravitee.definition.model.v4.flow.Flow flow2 = new io.gravitee.definition.model.v4.flow.Flow();
        flow2.setName("flow2");
        io.gravitee.definition.model.v4.flow.Flow flow3 = new io.gravitee.definition.model.v4.flow.Flow();
        flow3.setId("id3");
        flow3.setName("flow3");
        Flow repoFlow3 = flowMapper.toRepository(flow3, FlowReferenceType.API, referenceId, 1);
        repoFlow3.setId(flow3.getId());

        when(flowRepository.findByReference(any(), eq(referenceId))).thenAnswer(invocation -> List.of(repoFlow1, repoFlow3));
        when(flowRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = flowService.save(
            FlowReferenceType.API,
            referenceId,
            List.of(flow2)
        );
        assertThat(flowServiceByReference).isNotNull();
        assertThat(flowServiceByReference.size()).isEqualTo(1);
        assertThat(flowServiceByReference.get(0).getName()).isEqualTo("flow2");

        verify(flowRepository, never()).deleteByReference(FlowReferenceType.API, referenceId);
        verify(flowRepository, times(1)).deleteAllById(Set.of(flow1.getId(), flow3.getId()));
        verify(flowRepository, times(1)).create(any());
    }

    @Test
    public void shouldUpdateOneAndCreateFlow() throws TechnicalException {
        FlowMapper flowMapper = new FlowMapper();
        String referenceId = "apiId";
        io.gravitee.definition.model.v4.flow.Flow flow1 = new io.gravitee.definition.model.v4.flow.Flow();
        flow1.setId("id1");
        flow1.setName("flow1");
        Flow repoFlow1 = flowMapper.toRepository(flow1, FlowReferenceType.API, referenceId, 0);
        repoFlow1.setId(flow1.getId());
        io.gravitee.definition.model.v4.flow.Flow flow2 = new io.gravitee.definition.model.v4.flow.Flow();
        flow2.setName("flow2");

        when(flowRepository.findByReference(any(), eq(referenceId))).thenAnswer(invocation -> List.of(repoFlow1));
        when(flowRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(flowRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = flowService.save(
            FlowReferenceType.API,
            referenceId,
            List.of(flow1, flow2)
        );
        assertThat(flowServiceByReference).isNotNull();
        assertThat(flowServiceByReference.size()).isEqualTo(2);
        assertThat(flowServiceByReference.get(0).getName()).isEqualTo("flow1");
        assertThat(flowServiceByReference.get(1).getName()).isEqualTo("flow2");

        verify(flowRepository, never()).deleteByReference(FlowReferenceType.API, referenceId);
        verify(flowRepository, never()).delete(anyString());
        verify(flowRepository, times(1)).create(any());
        verify(flowRepository, times(1)).update(any());
    }

    @Test
    public void shouldDeleteAllFlows() throws TechnicalException {
        String referenceId = "apiId";

        List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = flowService.save(
            FlowReferenceType.API,
            referenceId,
            List.of()
        );
        assertThat(flowServiceByReference).isNotNull();
        assertThat(flowServiceByReference.size()).isEqualTo(0);

        verify(flowRepository).deleteByReference(FlowReferenceType.API, referenceId);
        verify(flowRepository, never()).delete(any());
        verify(flowRepository, never()).create(any());
    }
}
