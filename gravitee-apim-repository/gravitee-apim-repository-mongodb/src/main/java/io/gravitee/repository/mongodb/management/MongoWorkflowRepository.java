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
package io.gravitee.repository.mongodb.management;

import static java.util.stream.Collectors.toList;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.WorkflowRepository;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.repository.mongodb.management.internal.api.WorkflowMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.WorkflowMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoWorkflowRepository implements WorkflowRepository {

    @Autowired
    private WorkflowMongoRepository internalWorkflowRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Workflow> findById(String workflowId) throws TechnicalException {
        log.debug("Find workflow by ID [{}]", workflowId);

        final WorkflowMongo workflow = internalWorkflowRepo.findById(workflowId).orElse(null);

        log.debug("Find workflow by ID [{}] - Done", workflowId);
        return Optional.ofNullable(mapper.map(workflow));
    }

    @Override
    public Workflow create(Workflow workflow) throws TechnicalException {
        log.debug("Create workflow [{}]", workflow.getId());

        WorkflowMongo workflowMongo = mapper.map(workflow);
        WorkflowMongo createdWorkflowMongo = internalWorkflowRepo.insert(workflowMongo);

        Workflow res = mapper.map(createdWorkflowMongo);

        log.debug("Create workflow [{}] - Done", workflow.getId());

        return res;
    }

    @Override
    public Workflow update(Workflow workflow) throws TechnicalException {
        if (workflow == null || workflow.getId() == null) {
            throw new IllegalStateException("Workflow to update must have an id");
        }

        final WorkflowMongo workflowMongo = internalWorkflowRepo.findById(workflow.getId()).orElse(null);

        if (workflowMongo == null) {
            throw new IllegalStateException(String.format("No workflow found with id [%s]", workflow.getId()));
        }

        try {
            //Update
            workflowMongo.setReferenceType(workflow.getReferenceType());
            workflowMongo.setReferenceId(workflow.getReferenceId());
            workflowMongo.setType(workflow.getType());
            workflowMongo.setState(workflow.getState());
            workflowMongo.setComment(workflow.getComment());
            workflowMongo.setCreatedAt(workflow.getCreatedAt());

            WorkflowMongo workflowMongoUpdated = internalWorkflowRepo.save(workflowMongo);
            return mapper.map(workflowMongoUpdated);
        } catch (Exception e) {
            log.error("An error occured when updating workflow", e);
            throw new TechnicalException("An error occured when updating workflow");
        }
    }

    @Override
    public void delete(String workflowId) throws TechnicalException {
        try {
            internalWorkflowRepo.deleteById(workflowId);
        } catch (Exception e) {
            log.error("An error occured when deleting workflow [{}]", workflowId, e);
            throw new TechnicalException("An error occured when deleting workflow");
        }
    }

    @Override
    public Set<Workflow> findAll() {
        final List<WorkflowMongo> workflows = internalWorkflowRepo.findAll();
        return workflows.stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public List<Workflow> findByReferenceAndType(String referenceType, String referenceId, String type) {
        log.debug("Find workflow by reference and type '{}' / '{}' / '{}'", referenceType, referenceId, type);

        final List<WorkflowMongo> workflows = internalWorkflowRepo.findByReferenceTypeAndReferenceIdAndTypeOrderByCreatedAtDesc(
            referenceType,
            referenceId,
            type
        );

        log.debug("Find workflow by reference and type '{}' / '{}' / '{}' done", referenceType, referenceId, type);
        return workflows.stream().map(this::map).collect(toList());
    }

    @Override
    public List<Workflow> findByReferencesAndType(String referenceType, Collection<String> referenceIds, String type) {
        log.debug("Find workflows by references and type '{}' / '{}' / '{}'", referenceType, referenceIds, type);
        if (referenceIds == null || referenceIds.isEmpty()) {
            return List.of();
        }
        final List<WorkflowMongo> workflows = internalWorkflowRepo.findByReferenceTypeAndReferenceIdInAndTypeOrderByCreatedAtDesc(
            referenceType,
            referenceIds,
            type
        );
        log.debug("Find workflows by references and type '{}' / '{}' / '{}' done", referenceType, referenceIds, type);
        return workflows.stream().map(this::map).collect(toList());
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, String referenceType) throws TechnicalException {
        log.debug("Delete workflow by reference {}/{}", referenceId, referenceType);

        final var workflows = internalWorkflowRepo
            .deleteByReferenceIdAndReferenceType(referenceId, referenceType)
            .stream()
            .map(WorkflowMongo::getId)
            .toList();

        log.debug("Delete workflow by reference and type {}/{} - Done", referenceType, referenceId);
        return workflows;
    }

    private Workflow map(final WorkflowMongo workflowMongo) {
        if (workflowMongo == null) {
            return null;
        }
        final Workflow workflow = new Workflow();
        workflow.setId(workflowMongo.getId());
        workflow.setReferenceType(workflowMongo.getReferenceType());
        workflow.setReferenceId(workflowMongo.getReferenceId());
        workflow.setType(workflowMongo.getType());
        workflow.setState(workflowMongo.getState());
        workflow.setUser(workflowMongo.getUser());
        workflow.setComment(workflowMongo.getComment());
        workflow.setCreatedAt(workflowMongo.getCreatedAt());
        return workflow;
    }
}
