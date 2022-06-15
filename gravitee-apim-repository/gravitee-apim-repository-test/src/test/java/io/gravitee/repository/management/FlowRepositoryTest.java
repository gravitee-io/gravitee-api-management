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
package io.gravitee.repository.management;

import static org.junit.Assert.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.flow.*;
import java.util.*;
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
        return "flow.";
    }

    @Test
    public void shouldFindByReference() throws TechnicalException {
        List<Flow> flows = flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-1");

        assertEquals(1, flows.size());
        assertEquals(flows.get(0).getPre().size(), 2);
        assertEquals(flows.get(0).getPost().size(), 3);
        assertEquals(flows.get(0).getCondition(), "my-condition");
        assertEquals(flows.get(0).getCreatedAt(), new Date(1470157767000L));
        assertEquals(flows.get(0).getId(), "flow-tag1");
        assertEquals(flows.get(0).getMethods().size(), 2);
        assertEquals(flows.get(0).getName(), "tag-1");
        assertEquals(flows.get(0).getOrder(), 1);
        assertEquals(flows.get(0).getUpdatedAt(), new Date(1470157767000L));
        assertEquals(flows.get(0).getConsumers().size(), 2);
        assertEquals(flows.get(0).getPath(), "/");
        assertEquals(flows.get(0).getOperator(), FlowOperator.STARTS_WITH);
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

        assertEquals(flow.getId(), flowCreated.getId());
        assertEquals(flow.getName(), flowCreated.getName());
        assertEquals(flow.getCondition(), flowCreated.getCondition());

        assertEquals(flow.getCreatedAt(), flowCreated.getCreatedAt());
        assertEquals(flow.isEnabled(), flowCreated.isEnabled());
        assertEquals(flow.getMethods(), flowCreated.getMethods());
        assertEquals(flow.getPath(), flowCreated.getPath());
        assertEquals(flow.getOperator(), flowCreated.getOperator());
        assertEquals(flow.getReferenceId(), flowCreated.getReferenceId());
        assertEquals(flow.getUpdatedAt(), flowCreated.getUpdatedAt());
        assertEquals(flow.getConsumers().size(), flowCreated.getConsumers().size());
        assertEquals(flow.getConsumers().get(0), flowCreated.getConsumers().get(0));
        assertEquals(flow.getOrder(), flowCreated.getOrder());
        assertEquals(flow.getPre().get(0).getOrder(), flowCreated.getPre().get(0).getOrder());
        assertEquals(flow.getPre().get(0).getCondition(), flowCreated.getPre().get(0).getCondition());
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

        assertEquals(flow.getId(), flowUpdated.getId());
        assertEquals(flow.getName(), flowUpdated.getName());
        assertEquals(flow.getCondition(), flowUpdated.getCondition());

        assertEquals(flow.getCreatedAt(), flowUpdated.getCreatedAt());
        assertEquals(flow.isEnabled(), flowUpdated.isEnabled());
        assertEquals(flow.getMethods(), flowUpdated.getMethods());
        assertEquals(flow.getPath(), flowUpdated.getPath());
        assertEquals(flow.getOperator(), flowUpdated.getOperator());
        assertEquals(flow.getReferenceId(), flowUpdated.getReferenceId());
        assertEquals(flow.getUpdatedAt(), flowUpdated.getUpdatedAt());
        assertEquals(flow.getConsumers().size(), flowUpdated.getConsumers().size());
        assertEquals(flow.getConsumers().get(0), flowUpdated.getConsumers().get(0));
        assertEquals(flow.getPre().size(), 1);
        assertEquals(flow.getPost().size(), 1);
        assertEquals(flow.getOrder(), 5);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        assertTrue(flowRepository.findById("tag-deleted").isPresent());

        flowRepository.delete("tag-deleted");

        assertFalse(flowRepository.findById("tag-deleted").isPresent());
    }

    @Test
    public void shouldDeleteByReference() throws TechnicalException {
        assertEquals(flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-deleted").size(), 2);

        flowRepository.deleteByReference(FlowReferenceType.ORGANIZATION, "orga-deleted");

        assertEquals(flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-deleted").size(), 0);
    }
}
