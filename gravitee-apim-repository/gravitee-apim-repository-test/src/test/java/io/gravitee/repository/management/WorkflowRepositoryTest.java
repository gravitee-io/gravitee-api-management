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

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static org.junit.Assert.*;

import io.gravitee.repository.management.model.Workflow;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class WorkflowRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/workflow-tests/";
    }

    @Test
    public void shouldFindAll() throws Exception {
        final Set<Workflow> workflows = workflowRepository.findAll();

        assertNotNull(workflows);
        assertEquals(6, workflows.size());
        final Optional<Workflow> optionalWorkflow = workflows
            .stream()
            .filter(workflow -> "old-workflow".equals(workflow.getId()))
            .findAny();
        assertTrue(optionalWorkflow.isPresent());
        assertEquals("API", optionalWorkflow.get().getReferenceType());
        assertEquals("api-id", optionalWorkflow.get().getReferenceId());
        assertEquals("REVIEW", optionalWorkflow.get().getType());
        assertEquals("DRAFT", optionalWorkflow.get().getState());
        assertEquals("User", optionalWorkflow.get().getUser());
        assertEquals("Comment", optionalWorkflow.get().getComment());
        assertTrue(compareDate(1518357357000L, optionalWorkflow.get().getCreatedAt().getTime()));
    }

    @Test
    public void shouldCreate() throws Exception {
        final Workflow workflow = new Workflow();
        workflow.setId("new-workflow");
        workflow.setReferenceType("API");
        workflow.setReferenceId("api-id");
        workflow.setType("REVIEW");
        workflow.setState("DRAFT");
        workflow.setComment("test");
        workflow.setCreatedAt(new Date(1439022010883L));

        int nbWorkflowsBeforeCreation = workflowRepository.findAll().size();
        workflowRepository.create(workflow);
        int nbWorkflowsAfterCreation = workflowRepository.findAll().size();

        assertEquals(nbWorkflowsBeforeCreation + 1, nbWorkflowsAfterCreation);

        Optional<Workflow> optional = workflowRepository.findById("new-workflow");
        Assert.assertTrue("Workflow saved not found", optional.isPresent());
        final Workflow fetchedWorkflow = optional.get();
        assertEquals(workflow.getReferenceType(), fetchedWorkflow.getReferenceType());
        assertEquals(workflow.getReferenceId(), fetchedWorkflow.getReferenceId());
        assertEquals(workflow.getType(), fetchedWorkflow.getType());
        assertEquals(workflow.getState(), fetchedWorkflow.getState());
        assertEquals(workflow.getComment(), fetchedWorkflow.getComment());
        assertTrue(compareDate(workflow.getCreatedAt(), fetchedWorkflow.getCreatedAt()));
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Workflow> optional = workflowRepository.findById("workflow");
        Assert.assertTrue("Workflow to update not found", optional.isPresent());

        final Workflow workflow = optional.get();
        workflow.setReferenceType("New reference type");
        workflow.setReferenceId("New reference id");
        workflow.setType("New type");
        workflow.setState("IN_REVIEW");
        workflow.setComment("New comment");
        final Date date = new Date(1439022010883L);
        workflow.setCreatedAt(date);

        int nbWorkflowsBeforeUpdate = workflowRepository.findAll().size();
        workflowRepository.update(workflow);
        int nbWorkflowsAfterUpdate = workflowRepository.findAll().size();

        assertEquals(nbWorkflowsBeforeUpdate, nbWorkflowsAfterUpdate);

        Optional<Workflow> optionalUpdated = workflowRepository.findById("workflow");
        Assert.assertTrue("Workflow to update not found", optionalUpdated.isPresent());
        final Workflow fetchedWorkflow = optionalUpdated.get();
        assertEquals(workflow.getReferenceType(), fetchedWorkflow.getReferenceType());
        assertEquals(workflow.getReferenceId(), fetchedWorkflow.getReferenceId());
        assertEquals(workflow.getType(), fetchedWorkflow.getType());
        assertEquals(workflow.getState(), fetchedWorkflow.getState());
        assertEquals(workflow.getComment(), fetchedWorkflow.getComment());
        assertTrue(compareDate(workflow.getCreatedAt(), fetchedWorkflow.getCreatedAt()));
    }

    @Test
    public void shouldDelete() throws Exception {
        int nbWorkflowsBeforeDeletion = workflowRepository.findAll().size();
        workflowRepository.delete("workflow");
        int nbWorkflowsAfterDeletion = workflowRepository.findAll().size();

        assertEquals(nbWorkflowsBeforeDeletion - 1, nbWorkflowsAfterDeletion);
    }

    @Test
    public void shouldFindByReferenceAndType() throws Exception {
        final List<Workflow> workflows = workflowRepository.findByReferenceAndType("API", "api-id", "REVIEW");
        assertNotNull(workflows);
        assertEquals(3, workflows.size());
        assertEquals("workflow", workflows.get(0).getId());
    }

    @Test
    public void should_delete_by_reference_id_and_reference_type() throws Exception {
        final var nbBeforeDeletion = workflowRepository.findByReferenceAndType("APPLICATION", "ToBeDeleted", "REVIEW").size();
        final var deleted = workflowRepository.deleteByReferenceIdAndReferenceType("ToBeDeleted", "APPLICATION").size();
        final var nbAfterDeletion = workflowRepository.findByReferenceAndType("APPLICATION", "ToBeDeleted", "REVIEW").size();

        assertEquals(2, nbBeforeDeletion);
        assertEquals(2, deleted);
        assertEquals(0, nbAfterDeletion);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownWorkflow() throws Exception {
        Workflow unknownWorkflow = new Workflow();
        unknownWorkflow.setId("unknown");
        workflowRepository.update(unknownWorkflow);
        fail("An unknown workflow should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        workflowRepository.update(null);
        fail("A null workflow should not be updated");
    }
}
