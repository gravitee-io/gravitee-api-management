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
package io.gravitee.repository.mock.management;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.*;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import io.gravitee.repository.management.model.flow.selector.FlowSelector;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.*;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowRepositoryMock extends AbstractRepositoryMock<FlowRepository> {

    public FlowRepositoryMock() {
        super(FlowRepository.class);
    }

    @Override
    protected void prepare(FlowRepository repository) throws Exception {
        Flow flow1 = mock(Flow.class);

        when(flow1.getId()).thenReturn("flow-tag1");
        when(flow1.getName()).thenReturn("tag-1");
        when(flow1.getCreatedAt()).thenReturn(new Date(1470157767000L));
        when(flow1.getUpdatedAt()).thenReturn(new Date(1470157767000L));

        when(flow1.getReferenceType()).thenReturn(FlowReferenceType.ORGANIZATION);
        when(flow1.getReferenceId()).thenReturn("orga-1");
        FlowStep tag1PreStep1 = mock(FlowStep.class);
        when(tag1PreStep1.getOrder()).thenReturn(1);
        when(tag1PreStep1.getCondition()).thenReturn("pre-condition");
        FlowStep tag1PreStep2 = mock(FlowStep.class);

        FlowStep tag1PostStep1 = mock(FlowStep.class);
        FlowStep tag1PostStep2 = mock(FlowStep.class);
        FlowStep tag1PostStep3 = mock(FlowStep.class);

        lenient().when(flow1.getPre()).thenReturn(Arrays.asList(tag1PreStep1, tag1PreStep2));
        lenient().when(flow1.getRequest()).thenReturn(Arrays.asList(tag1PreStep1, tag1PreStep2));
        lenient().when(flow1.getPost()).thenReturn(Arrays.asList(tag1PostStep1, tag1PostStep2, tag1PostStep3));
        lenient().when(flow1.getResponse()).thenReturn(Arrays.asList(tag1PostStep1, tag1PostStep2, tag1PostStep3));

        lenient().when(flow1.getMethods()).thenReturn(new HashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST)));
        lenient().when(flow1.getPath()).thenReturn("/");
        lenient().when(flow1.getOperator()).thenReturn(FlowOperator.STARTS_WITH);
        lenient().when(flow1.getCondition()).thenReturn("my-condition");
        lenient().when(flow1.getRequest()).thenReturn(Arrays.asList(tag1PreStep1, tag1PreStep2));

        FlowHttpSelector flowHttpSelector = new FlowHttpSelector();
        flowHttpSelector.setPath("/");
        flowHttpSelector.setMethods(Set.of(HttpMethod.GET, HttpMethod.POST));
        flowHttpSelector.setPathOperator(FlowOperator.STARTS_WITH);
        FlowConditionSelector flowConditionSelector = new FlowConditionSelector();
        flowConditionSelector.setCondition("my-condition");
        lenient().when(flow1.getSelectors()).thenReturn(List.of(flowHttpSelector, flowConditionSelector));

        lenient().when(flow1.getConsumers()).thenReturn(Arrays.asList(mock(FlowConsumer.class), mock(FlowConsumer.class)));
        lenient().when(flow1.getTags()).thenReturn(Set.of("tag1", "tag2"));

        when(flow1.getOrder()).thenReturn(1);
        when(repository.findByReference(FlowReferenceType.ORGANIZATION, "orga-1")).thenReturn(Arrays.asList(flow1));

        Flow created = mock(Flow.class);
        when(created.getId()).thenReturn("flow-create");
        when(created.getName()).thenReturn("tag-create");
        when(created.getReferenceType()).thenReturn(FlowReferenceType.ORGANIZATION);
        when(created.getReferenceId()).thenReturn("my-orga");
        when(created.getCreatedAt()).thenReturn(new Date(1470157767000L));
        when(created.getUpdatedAt()).thenReturn(new Date(1470157767000L));
        when(created.getOrder()).thenReturn(2);

        lenient().when(created.getMethods()).thenReturn(new HashSet<>(Arrays.asList(HttpMethod.CONNECT)));
        lenient().when(created.getPath()).thenReturn("/");
        lenient().when(created.getOperator()).thenReturn(FlowOperator.STARTS_WITH);
        lenient().when(created.getCondition()).thenReturn("my-condition");

        FlowHttpSelector createdFlowHttpSelector = new FlowHttpSelector();
        createdFlowHttpSelector.setPath("/");
        createdFlowHttpSelector.setMethods(Set.of(HttpMethod.CONNECT));
        createdFlowHttpSelector.setPathOperator(FlowOperator.STARTS_WITH);
        FlowConditionSelector createdFlowConditionSelector = new FlowConditionSelector();
        createdFlowConditionSelector.setCondition("my-condition");
        lenient().when(created.getSelectors()).thenReturn(List.of(createdFlowHttpSelector, createdFlowConditionSelector));

        lenient().when(created.getPre()).thenReturn(Arrays.asList(tag1PreStep1, tag1PreStep2));
        lenient().when(created.getRequest()).thenReturn(Arrays.asList(tag1PreStep1, tag1PreStep2));

        List<FlowConsumer> consumers = new ArrayList<>();
        consumers.add(new FlowConsumer(FlowConsumerType.TAG, "tag-1"));
        lenient().when(created.getConsumers()).thenReturn(consumers);
        lenient().when(created.getTags()).thenReturn(Set.of("tag-1"));

        when(repository.create(any())).thenReturn(created);

        Flow updated = mock(Flow.class);
        when(updated.getId()).thenReturn("tag-updated");
        when(updated.getName()).thenReturn("Tag updated!");
        when(updated.getReferenceId()).thenReturn("my-orga");
        when(updated.getOrder()).thenReturn(5);
        when(updated.getReferenceType()).thenReturn(FlowReferenceType.ORGANIZATION);
        when(updated.getCreatedAt()).thenReturn(new Date(1470157767000L));
        when(updated.getUpdatedAt()).thenReturn(new Date(1470157797000L));

        lenient().when(updated.getMethods()).thenReturn(new HashSet<>(Arrays.asList(HttpMethod.CONNECT)));
        lenient().when(updated.getPath()).thenReturn("/");
        lenient().when(updated.getOperator()).thenReturn(FlowOperator.STARTS_WITH);
        lenient().when(updated.getCondition()).thenReturn("my-condition");

        FlowHttpSelector updatedFlowHttpSelector = new FlowHttpSelector();
        updatedFlowHttpSelector.setPath("/");
        updatedFlowHttpSelector.setMethods(Set.of(HttpMethod.CONNECT));
        updatedFlowHttpSelector.setPathOperator(FlowOperator.STARTS_WITH);
        FlowConditionSelector updatedFlowConditionSelector = new FlowConditionSelector();
        updatedFlowConditionSelector.setCondition("my-condition");
        lenient().when(updated.getSelectors()).thenReturn(List.of(updatedFlowHttpSelector, updatedFlowConditionSelector));

        lenient().when(updated.getPre()).thenReturn(Arrays.asList(tag1PreStep1));
        lenient().when(updated.getPost()).thenReturn(Arrays.asList(tag1PostStep1));
        lenient().when(updated.getRequest()).thenReturn(Arrays.asList(tag1PreStep1));
        lenient().when(updated.getResponse()).thenReturn(Arrays.asList(tag1PostStep1));

        lenient().when(updated.getConsumers()).thenReturn(consumers);
        lenient().when(updated.getTags()).thenReturn(Set.of("tag-1"));
        when(repository.update(any())).thenReturn(updated);

        when(repository.findById("tag-deleted")).thenReturn(of(mock(Flow.class)), empty());

        when(repository.findByReference(FlowReferenceType.ORGANIZATION, "orga-deleted"))
            .thenReturn(Arrays.asList(flow1, mock(Flow.class)), Collections.emptyList());

        when(repository.findById("tag-v4-deleted")).thenReturn(of(mock(Flow.class)), empty());

        when(repository.findByReference(FlowReferenceType.ORGANIZATION, "orga-v4-deleted"))
            .thenReturn(Arrays.asList(flow1, mock(Flow.class)), Collections.emptyList());
    }
}
