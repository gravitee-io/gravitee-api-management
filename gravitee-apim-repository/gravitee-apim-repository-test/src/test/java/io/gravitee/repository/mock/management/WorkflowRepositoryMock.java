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

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.WorkflowRepository;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WorkflowRepositoryMock extends AbstractRepositoryMock<WorkflowRepository> {

    public WorkflowRepositoryMock() {
        super(WorkflowRepository.class);
    }

    @Override
    protected void prepare(WorkflowRepository workflowRepository) throws Exception {
        final Date date = new Date(1439022010883L);

        final Workflow workflow = new Workflow();
        workflow.setId("new-workflow");
        workflow.setReferenceType("API");
        workflow.setReferenceId("api-id");
        workflow.setType("REVIEW");
        workflow.setState("DRAFT");
        workflow.setComment("test");
        workflow.setCreatedAt(date);

        final Workflow workflow2 = new Workflow();
        workflow2.setId("workflow");

        final Workflow workflowBeforUpdate = new Workflow();
        workflowBeforUpdate.setId("workflow");
        workflowBeforUpdate.setState("IN_REVIEW");

        final Workflow workflow2Updated = new Workflow();
        workflow2Updated.setId("workflow");
        workflow2Updated.setReferenceType("New reference type");
        workflow2Updated.setReferenceId("New reference id");
        workflow2Updated.setType("New type");
        workflow2Updated.setState("IN_REVIEW");
        workflow2Updated.setComment("New comment");
        workflow2Updated.setCreatedAt(date);

        final Workflow oldWorkflow = new Workflow();
        oldWorkflow.setId("old-workflow");
        oldWorkflow.setReferenceType("API");
        oldWorkflow.setReferenceId("api-id");
        oldWorkflow.setType("REVIEW");
        oldWorkflow.setState("DRAFT");
        oldWorkflow.setComment("Comment");
        oldWorkflow.setUser("User");
        oldWorkflow.setCreatedAt(new Date(1518357357000L));

        final Workflow workflow3 = new Workflow();
        workflow3.setId("workflow-api");

        final Set<Workflow> workflows = newSet(workflow, oldWorkflow, workflow2Updated, workflow3);
        final Set<Workflow> workflowsAfterDelete = newSet(workflow, oldWorkflow, workflow3);
        final Set<Workflow> workflowsAfterAdd = newSet(workflow, workflow2, workflow3, mock(Workflow.class), mock(Workflow.class));

        when(workflowRepository.findAll()).thenReturn(workflows, workflowsAfterAdd, workflows, workflowsAfterDelete, workflows);

        when(workflowRepository.create(any(Workflow.class))).thenReturn(workflow);

        when(workflowRepository.findById("new-workflow")).thenReturn(of(workflow));
        when(workflowRepository.findById("workflow")).thenReturn(of(workflowBeforUpdate), of(workflow2Updated));

        when(workflowRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(workflowRepository.findByReferenceAndType("API", "api-id", "REVIEW")).thenReturn(asList(workflow2, oldWorkflow, workflow3));
    }
}
