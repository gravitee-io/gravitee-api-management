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
package io.gravitee.repository.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowConsumer;
import io.gravitee.repository.management.model.flow.FlowConsumerType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/flow-tests/";
    }

    @Override
    protected String getModelPackage() {
        return super.getModelPackage() + "flow.";
    }

    @Test
    public void shouldFindByReference() throws TechnicalException {
        List<Flow> flows = flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-1");

        assertEquals(1, flows.size());
        assertEquals(2, flows.get(0).getPre().size());
        assertEquals(3, flows.get(0).getPost().size());
        assertEquals("my-condition", flows.get(0).getCondition());
        assertEquals(new Date(1470157767000L), flows.get(0).getCreatedAt());
        assertEquals("flow-tag1", flows.get(0).getId());
        assertEquals(0, flows.get(0).getMethods().size());
        assertEquals("tag-1", flows.get(0).getName());
        assertEquals(1, flows.get(0).getOrder());
        assertEquals(new Date(1470157767000L), flows.get(0).getUpdatedAt());
        assertEquals(2, flows.get(0).getConsumers().size());
        assertEquals("/", flows.get(0).getPath());
        assertEquals(FlowOperator.STARTS_WITH, flows.get(0).getOperator());
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        Flow flow = new Flow();
        flow.setId("flow-create");
        flow.setName("tag-create");
        flow.setCondition("my-condition");
        flow.setCreatedAt(new Date(1470157767000L));
        flow.setEnabled(false);
        flow.setMethods(new HashSet<>(List.of(HttpMethod.CONNECT)));
        flow.setPath("/");
        flow.setOperator(FlowOperator.STARTS_WITH);

        FlowStep postStep = new FlowStep();
        postStep.setName("post-step");
        postStep.setPolicy("policy");
        flow.setPost(List.of(postStep));
        FlowStep preStep = new FlowStep();
        preStep.setName("pre-step");
        preStep.setPolicy("policy");
        preStep.setCondition("pre-condition");
        preStep.setOrder(1);
        flow.setPre(List.of(preStep));
        flow.setReferenceId("my-orga");
        flow.setReferenceType(FlowReferenceType.ORGANIZATION);
        flow.setUpdatedAt(new Date(1470157767000L));
        flow.setOrder(2);
        List<FlowConsumer> consumers = new ArrayList<>();
        consumers.add(new FlowConsumer(FlowConsumerType.TAG, "tag-1"));
        flow.setConsumers(consumers);

        Flow flowCreated = flowRepository.create(flow);

        assertEquals(flowCreated.getId(), flow.getId());
        assertEquals(flowCreated.getName(), flow.getName());
        assertEquals(flowCreated.getCondition(), flow.getCondition());

        assertEquals(flowCreated.getCreatedAt(), flow.getCreatedAt());
        assertEquals(flowCreated.isEnabled(), flow.isEnabled());
        assertEquals(flowCreated.getMethods(), flow.getMethods());
        assertEquals(flowCreated.getPath(), flow.getPath());
        assertEquals(flowCreated.getOperator(), flow.getOperator());
        assertEquals(flowCreated.getReferenceId(), flow.getReferenceId());
        assertEquals(flowCreated.getUpdatedAt(), flow.getUpdatedAt());
        assertEquals(flowCreated.getConsumers().size(), flow.getConsumers().size());
        assertEquals(flowCreated.getConsumers().get(0), flow.getConsumers().get(0));
        assertEquals(flowCreated.getOrder(), flow.getOrder());
        assertEquals(flowCreated.getPre().get(0).getOrder(), flow.getPre().get(0).getOrder());
        assertEquals(flowCreated.getPre().get(0).getCondition(), flow.getPre().get(0).getCondition());
    }

    @Test
    public void shouldCreateWithBigValues() throws TechnicalException {
        Flow flow = new Flow();
        // Mandatory fields
        flow.setId("flow-create-big-values");
        flow.setOrder(1);
        flow.setCreatedAt(new Date(1470157767000L));
        flow.setReferenceId("my-orga");
        flow.setReferenceType(FlowReferenceType.ORGANIZATION);

        // Fields with big capacities
        flow.setName(
            "A big name with 256 characters-A big name with 256 characters-A big name with 256 characters-A big name with 256 characters-A big name with 256 characters-A big name with 256 characters-A big name with 256 characters-A big name with 256 characters---------"
        );
        flow.setCondition(
            "A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters-A huge condition with 512 characters--------------------------------"
        );
        flow.setPath(
            "A big path with 256 characters-A big path with 256 characters-A big path with 256 characters-A big path with 256 characters-A big path with 256 characters-A big path with 256 characters-A big path with 256 characters-A big path with 256 characters---------"
        );

        Flow flowCreated = flowRepository.create(flow);
        assertEquals(flowCreated.getId(), flow.getId());
        assertEquals(flowCreated.getName(), flow.getName());
        assertEquals(flowCreated.getCondition(), flow.getCondition());
        assertEquals(flowCreated.getPath(), flow.getPath());
        assertEquals(flowCreated.getOrder(), flow.getOrder());
        assertEquals(flowCreated.getCreatedAt(), flow.getCreatedAt());
        assertEquals(flowCreated.getReferenceId(), flow.getReferenceId());
        assertEquals(flowCreated.getReferenceType(), flow.getReferenceType());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Flow flow = new Flow();
        flow.setId("tag-updated");
        flow.setName("Tag updated!");
        flow.setCondition("my-condition");
        flow.setCreatedAt(new Date(1470157767000L));
        flow.setEnabled(false);
        flow.setMethods(new HashSet<>(List.of(HttpMethod.CONNECT)));
        flow.setPath("/");
        flow.setOperator(FlowOperator.STARTS_WITH);
        FlowStep postStep = new FlowStep();
        postStep.setName("post-step");
        postStep.setPolicy("policy");
        flow.setPost(List.of(postStep));
        FlowStep preStep = new FlowStep();
        preStep.setName("pre-step");
        preStep.setOrder(3);
        preStep.setPolicy("policy");
        flow.setPre(List.of(preStep));
        flow.setReferenceId("my-orga");
        flow.setReferenceType(FlowReferenceType.ORGANIZATION);
        flow.setUpdatedAt(new Date(1470157797000L));
        flow.setOrder(5);
        List<FlowConsumer> consumers = new ArrayList<>();
        consumers.add(new FlowConsumer(FlowConsumerType.TAG, "tag-1"));
        flow.setConsumers(consumers);

        Flow flowUpdated = flowRepository.update(flow);

        assertEquals(flowUpdated.getId(), flow.getId());
        assertEquals(flowUpdated.getName(), flow.getName());
        assertEquals(flowUpdated.getCondition(), flow.getCondition());

        assertEquals(flowUpdated.getCreatedAt(), flow.getCreatedAt());
        assertEquals(flowUpdated.isEnabled(), flow.isEnabled());
        assertEquals(flowUpdated.getMethods(), flow.getMethods());
        assertEquals(flowUpdated.getPath(), flow.getPath());
        assertEquals(flowUpdated.getOperator(), flow.getOperator());
        assertEquals(flowUpdated.getReferenceId(), flow.getReferenceId());
        assertEquals(flowUpdated.getUpdatedAt(), flow.getUpdatedAt());
        assertEquals(flowUpdated.getConsumers().size(), flow.getConsumers().size());
        assertEquals(flowUpdated.getConsumers().get(0), flow.getConsumers().get(0));
        assertEquals(1, flowUpdated.getPre().size());
        assertEquals(1, flowUpdated.getPost().size());
        assertEquals(5, flowUpdated.getOrder());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        assertTrue(flowRepository.findById("tag-deleted").isPresent());

        flowRepository.delete("tag-deleted");

        assertFalse(flowRepository.findById("tag-deleted").isPresent());
    }

    @Test
    public void shouldDeleteByReference() throws TechnicalException {
        assertEquals(2, flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-deleted").size());

        flowRepository.deleteByReferenceIdAndReferenceType("orga-deleted", FlowReferenceType.ORGANIZATION);

        assertEquals(0, flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-deleted").size());
    }

    @Test
    public void shouldDeleteByIds() throws TechnicalException {
        List<Flow> flows = flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-deleted");
        assertEquals(2, flows.size());
        Set<String> ids = flows.stream().map(Flow::getId).collect(Collectors.toSet());

        flowRepository.deleteAllById(ids);

        assertEquals(0, flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-deleted").size());
    }
}
