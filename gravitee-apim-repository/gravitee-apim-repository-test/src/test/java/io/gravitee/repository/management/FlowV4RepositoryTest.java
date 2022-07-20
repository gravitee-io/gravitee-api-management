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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.flow.Flow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.management.model.flow.FlowStep;
import io.gravitee.repository.management.model.flow.selector.FlowConditionSelector;
import io.gravitee.repository.management.model.flow.selector.FlowHttpSelector;
import io.gravitee.repository.management.model.flow.selector.FlowOperator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FlowV4RepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/flow-v4-tests/";
    }

    @Override
    protected String getModelPackage() {
        return super.getModelPackage() + "flow.";
    }

    @Test
    public void shouldFindByReference() throws TechnicalException {
        List<Flow> flows = flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-1");

        assertEquals(flows.size(), 1);
        Flow flow = flows.get(0);
        assertEquals("flow-tag1", flow.getId());
        assertEquals("tag-1", flow.getName());
        assertEquals(new Date(1470157767000L), flow.getCreatedAt());
        assertEquals(new Date(1470157767000L), flow.getUpdatedAt());
        assertEquals(1, flow.getOrder());
        assertEquals(2, flow.getTags().size());

        assertEquals(2, flow.getRequest().size());
        assertEquals(3, flow.getResponse().size());

        assertEquals(2, flow.getSelectors().size());
        flow
            .getSelectors()
            .forEach(
                flowSelector -> {
                    if (flowSelector instanceof FlowHttpSelector) {
                        FlowHttpSelector httpSelector = (FlowHttpSelector) flowSelector;
                        assertEquals(2, httpSelector.getMethods().size());
                        assertEquals("/", httpSelector.getPath());
                        assertEquals(FlowOperator.STARTS_WITH, httpSelector.getPathOperator());
                    } else if (flowSelector instanceof FlowConditionSelector) {
                        FlowConditionSelector conditionSelector = (FlowConditionSelector) flowSelector;
                        assertEquals("my-condition", conditionSelector.getCondition());
                    }
                }
            );
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        Flow flow = new Flow();
        flow.setId("flow-create");
        flow.setName("tag-create");

        flow.setCreatedAt(new Date(1470157767000L));
        flow.setEnabled(false);
        FlowHttpSelector flowHttpSelector = new FlowHttpSelector();
        flowHttpSelector.setPath("/");
        flowHttpSelector.setMethods(Set.of(HttpMethod.CONNECT));
        flowHttpSelector.setPathOperator(FlowOperator.STARTS_WITH);
        FlowConditionSelector flowConditionSelector = new FlowConditionSelector();
        flowConditionSelector.setCondition("my-condition");
        flow.setSelectors(List.of(flowHttpSelector, flowConditionSelector));

        FlowStep postStep = new FlowStep();
        postStep.setName("post-step");
        postStep.setPolicy("policy");
        flow.setResponse(List.of(postStep));
        FlowStep preStep = new FlowStep();
        preStep.setName("pre-step");
        preStep.setPolicy("policy");
        preStep.setCondition("pre-condition");
        preStep.setOrder(1);
        flow.setRequest(List.of(preStep));
        flow.setReferenceId("my-orga");
        flow.setReferenceType(FlowReferenceType.ORGANIZATION);
        flow.setUpdatedAt(new Date(1470157767000L));
        flow.setOrder(2);
        flow.setTags(Set.of("tag-1"));

        Flow flowCreated = flowRepository.create(flow);

        assertEquals(flowCreated.getId(), flow.getId());
        assertEquals(flowCreated.getName(), flow.getName());
        assertTrue(
            flow
                .getSelectors()
                .stream()
                .allMatch(
                    flowSelector ->
                        flowCreated.getSelectors().stream().anyMatch(flowSelectorCreated -> flowSelectorCreated.equals(flowSelector))
                )
        );

        assertEquals(flowCreated.getCreatedAt(), flow.getCreatedAt());
        assertEquals(flowCreated.isEnabled(), flow.isEnabled());
        assertEquals(flowCreated.getReferenceId(), flow.getReferenceId());
        assertEquals(flowCreated.getUpdatedAt(), flow.getUpdatedAt());
        assertEquals(flowCreated.getTags().size(), flow.getTags().size());
        assertEquals(flowCreated.getTags(), flow.getTags());
        assertEquals(flowCreated.getOrder(), flow.getOrder());
        assertEquals(flowCreated.getRequest().get(0).getOrder(), flow.getRequest().get(0).getOrder());
        assertEquals(flowCreated.getRequest().get(0).getCondition(), flow.getRequest().get(0).getCondition());
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        Flow flow = new Flow();
        flow.setId("tag-updated");
        flow.setName("Tag updated!");
        flow.setCreatedAt(new Date(1470157767000L));
        flow.setEnabled(false);

        FlowHttpSelector flowHttpSelector = new FlowHttpSelector();
        flowHttpSelector.setPath("/");
        flowHttpSelector.setMethods(Set.of(HttpMethod.CONNECT));
        flowHttpSelector.setPathOperator(FlowOperator.STARTS_WITH);
        FlowConditionSelector flowConditionSelector = new FlowConditionSelector();
        flowConditionSelector.setCondition("my-condition");
        flow.setSelectors(List.of(flowHttpSelector, flowConditionSelector));

        FlowStep postStep = new FlowStep();
        postStep.setName("post-step");
        postStep.setPolicy("policy");
        flow.setResponse(List.of(postStep));
        FlowStep preStep = new FlowStep();
        preStep.setName("pre-step");
        preStep.setOrder(3);
        preStep.setPolicy("policy");
        flow.setRequest(List.of(preStep));
        flow.setReferenceId("my-orga");
        flow.setReferenceType(FlowReferenceType.ORGANIZATION);
        flow.setUpdatedAt(new Date(1470157797000L));
        flow.setOrder(5);
        flow.setTags(Set.of("tag-1"));

        Flow flowUpdated = flowRepository.update(flow);

        assertEquals(flowUpdated.getId(), flow.getId());
        assertEquals(flowUpdated.getName(), flow.getName());

        assertTrue(
            flow
                .getSelectors()
                .stream()
                .allMatch(
                    flowSelector ->
                        flowUpdated.getSelectors().stream().anyMatch(flowSelectorUpdated -> flowSelectorUpdated.equals(flowSelector))
                )
        );

        assertEquals(flowUpdated.getCreatedAt(), flow.getCreatedAt());
        assertEquals(flowUpdated.isEnabled(), flow.isEnabled());
        assertEquals(flowUpdated.getReferenceId(), flow.getReferenceId());
        assertEquals(flowUpdated.getUpdatedAt(), flow.getUpdatedAt());
        assertEquals(flowUpdated.getTags(), flow.getTags());
        assertEquals(2, flowUpdated.getRequest().size());
        assertEquals(3, flowUpdated.getResponse().size());
        assertEquals(5, flowUpdated.getOrder());
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        assertTrue(flowRepository.findById("tag-v4-deleted").isPresent());

        flowRepository.delete("tag-v4-deleted");

        assertFalse(flowRepository.findById("tag-v4-deleted").isPresent());
    }

    @Test
    public void shouldDeleteByReference() throws TechnicalException {
        assertEquals(2, flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-v4-deleted").size());

        flowRepository.deleteByReference(FlowReferenceType.ORGANIZATION, "orga-v4-deleted");

        assertEquals(0, flowRepository.findByReference(FlowReferenceType.ORGANIZATION, "orga-v4-deleted").size());
    }
}
