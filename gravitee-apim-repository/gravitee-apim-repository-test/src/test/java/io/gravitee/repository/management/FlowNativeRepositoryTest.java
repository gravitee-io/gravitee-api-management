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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * @author Jourdi WALLER (jourdi.waller at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowNativeRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/flow-native-tests/";
    }

    @Override
    protected String getModelPackage() {
        return super.getModelPackage() + "flow.";
    }

    @Test
    public void shouldFindByReference() throws TechnicalException {
        List<Flow> flows = flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-1");

        assertEquals(1, flows.size());
        Flow flow = flows.get(0);
        assertEquals("flow-tag1", flow.getId());
        assertEquals("tag-1", flow.getName());
        assertEquals(new Date(1470157767000L), flow.getCreatedAt());
        assertEquals(new Date(1470157767000L), flow.getUpdatedAt());
        assertEquals(1, flow.getOrder());
        assertEquals(2, flow.getTags().size());

        assertEquals(0, flow.getRequest().size());
        assertEquals(0, flow.getResponse().size());
        assertEquals(1, flow.getConnect().size());
        assertEquals(1, flow.getInteract().size());

        assertEquals("{#request.headers != null}", flow.getConnect().get(0).getCondition());
        assertEquals("{#message.content != null}", flow.getConnect().get(0).getMessageCondition());
        assertEquals("{#response.headers != null}", flow.getInteract().get(0).getCondition());
        assertEquals("{#message.headers != null}", flow.getInteract().get(0).getMessageCondition());

        assertEquals(0, flow.getSelectors().size());
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        Flow flow = new Flow();
        flow.setId("flow-create");
        flow.setName("tag-create");

        flow.setCreatedAt(new Date(1470157767000L));
        flow.setEnabled(false);

        FlowStep publishStep = new FlowStep();
        publishStep.setName("publish-step");
        publishStep.setPolicy("policy");
        publishStep.setCondition("publish-condition");
        publishStep.setOrder(1);
        flow.setPublish(List.of(publishStep));

        FlowStep subscribeStep = new FlowStep();
        subscribeStep.setName("subscribe-step");
        subscribeStep.setPolicy("policy");
        subscribeStep.setCondition("subscribe-condition");
        subscribeStep.setOrder(1);
        flow.setSubscribe(List.of(subscribeStep));

        FlowStep connectStep = new FlowStep();
        connectStep.setName("connect-step");
        connectStep.setPolicy("policy");
        connectStep.setCondition("connect-condition");
        connectStep.setOrder(1);
        flow.setConnect(List.of(connectStep));

        FlowStep interactStep = new FlowStep();
        interactStep.setName("interact-step");
        interactStep.setPolicy("policy");
        interactStep.setCondition("interact-condition");
        interactStep.setOrder(1);
        flow.setInteract(List.of(interactStep));

        flow.setReferenceId("my-orga");
        flow.setReferenceType(FlowReferenceType.ORGANIZATION);
        flow.setUpdatedAt(new Date(1470157767000L));
        flow.setOrder(2);
        flow.setTags(Set.of("tag-1"));

        Flow flowCreated = flowRepository.create(flow);

        assertEquals(flowCreated.getId(), flow.getId());
        assertEquals(flowCreated.getName(), flow.getName());
        assertEquals(flowCreated.getSelectors().size(), 0);

        assertEquals(flow.getCreatedAt(), flowCreated.getCreatedAt());
        assertEquals(flow.isEnabled(), flowCreated.isEnabled());
        assertEquals(flow.getReferenceId(), flowCreated.getReferenceId());
        assertEquals(flow.getUpdatedAt(), flowCreated.getUpdatedAt());
        assertEquals(flow.getTags().size(), flowCreated.getTags().size());
        assertEquals(flow.getTags(), flowCreated.getTags());
        assertEquals(flow.getOrder(), flowCreated.getOrder());
        assertEquals(flow.getPublish().get(0).getCondition(), flowCreated.getPublish().get(0).getCondition());
        assertEquals(flow.getSubscribe().get(0).getCondition(), flowCreated.getSubscribe().get(0).getCondition());
        assertEquals(flow.getInteract().get(0).getCondition(), flowCreated.getInteract().get(0).getCondition());
        assertEquals(flow.getConnect().get(0).getCondition(), flowCreated.getConnect().get(0).getCondition());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Flow flow = new Flow();
        flow.setId("tag-updated");
        flow.setName("Tag updated!");
        flow.setCreatedAt(new Date(1470157767000L));
        flow.setEnabled(false);

        FlowStep publishStep = new FlowStep();
        publishStep.setName("publish-step");
        publishStep.setPolicy("policy");
        flow.setPublish(List.of(publishStep));
        FlowStep subscribeStep = new FlowStep();
        subscribeStep.setName("subscribe-step");
        subscribeStep.setOrder(3);
        subscribeStep.setPolicy("policy");
        flow.setSubscribe(List.of(subscribeStep));
        flow.setReferenceId("my-orga");
        flow.setReferenceType(FlowReferenceType.ORGANIZATION);
        flow.setUpdatedAt(new Date(1470157797000L));
        flow.setOrder(5);
        flow.setTags(Set.of("tag-1"));

        Flow flowUpdated = flowRepository.update(flow);

        assertEquals(flowUpdated.getId(), flow.getId());
        assertEquals(flowUpdated.getName(), flow.getName());
        assertEquals(flowUpdated.getSelectors().size(), 0);

        assertEquals(flowUpdated.getCreatedAt(), flow.getCreatedAt());
        assertEquals(flowUpdated.isEnabled(), flow.isEnabled());
        assertEquals(flowUpdated.getReferenceId(), flow.getReferenceId());
        assertEquals(flowUpdated.getUpdatedAt(), flow.getUpdatedAt());
        assertEquals(flowUpdated.getTags(), flow.getTags());

        assertEquals(1, flowUpdated.getPublish().size());
        assertEquals(1, flowUpdated.getSubscribe().size());
        assertEquals(5, flowUpdated.getOrder());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        assertTrue(flowRepository.findById("tag-native-deleted").isPresent());

        flowRepository.delete("tag-native-deleted");

        assertFalse(flowRepository.findById("tag-native-deleted").isPresent());
    }

    @Test
    public void shouldDeleteByReference() throws TechnicalException {
        assertEquals(2, flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-v4-deleted").size());

        flowRepository.deleteByReferenceIdAndReferenceType("orga-v4-deleted", FlowReferenceType.ORGANIZATION);

        assertEquals(0, flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-v4-deleted").size());
    }

    @Test
    public void shouldDeleteByIds() throws TechnicalException {
        List<Flow> flows = flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-v4-deleted");
        assertEquals(2, flows.size());
        Set<String> ids = flows.stream().map(Flow::getId).collect(Collectors.toSet());

        flowRepository.deleteAllById(ids);

        assertEquals(0, flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-v4-deleted").size());
    }
}
