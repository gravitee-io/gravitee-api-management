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
package io.gravitee.repository.redis.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.WorkflowRepository;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.repository.redis.management.internal.WorkflowRedisRepository;
import io.gravitee.repository.redis.management.model.RedisWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class RedisWorkflowRepository implements WorkflowRepository {

    @Autowired
    private WorkflowRedisRepository workflowRedisRepository;

    @Override
    public Optional<Workflow> findById(final String workflowId) throws TechnicalException {
        final RedisWorkflow redisWorkflow = workflowRedisRepository.findById(workflowId);
        return Optional.ofNullable(convert(redisWorkflow));
    }

    @Override
    public Workflow create(final Workflow workflow) throws TechnicalException {
        final RedisWorkflow redisWorkflow = workflowRedisRepository.saveOrUpdate(convert(workflow));
        return convert(redisWorkflow);
    }

    @Override
    public Workflow update(final Workflow workflow) throws TechnicalException {
        if (workflow == null || workflow.getId() == null) {
            throw new IllegalStateException("Workflow to update must have an id");
        }

        final RedisWorkflow redisWorkflow = workflowRedisRepository.findById(workflow.getId());

        if (redisWorkflow == null) {
            throw new IllegalStateException(String.format("No workflow found with id [%s]", workflow.getId()));
        }

        final RedisWorkflow redisWorkflowUpdated = workflowRedisRepository.saveOrUpdate(convert(workflow));
        return convert(redisWorkflowUpdated);
    }

    @Override
    public Set<Workflow> findAll() {
        final Set<RedisWorkflow> workflows = workflowRedisRepository.findAll();

        return workflows.stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Workflow> findByReferenceAndType(String referenceType, String referenceId, String type) {
        final List<RedisWorkflow> workflows = workflowRedisRepository.findByReferenceAndType(referenceType, referenceId, type);
        return workflows.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final String workflowId) throws TechnicalException {
        workflowRedisRepository.delete(workflowId);
    }

    private Workflow convert(final RedisWorkflow redisWorkflow) {
        final Workflow workflow = new Workflow();
        workflow.setId(redisWorkflow.getId());
        workflow.setReferenceType(redisWorkflow.getReferenceType());
        workflow.setReferenceId(redisWorkflow.getReferenceId());
        workflow.setType(redisWorkflow.getType());
        workflow.setState(redisWorkflow.getState());
        workflow.setComment(redisWorkflow.getComment());
        workflow.setUser(redisWorkflow.getUser());
        workflow.setCreatedAt(redisWorkflow.getCreatedAt());
        return workflow;
    }

    private RedisWorkflow convert(final Workflow workflow) {
        final RedisWorkflow redisWorkflow = new RedisWorkflow();
        redisWorkflow.setId(workflow.getId());
        redisWorkflow.setReferenceType(workflow.getReferenceType());
        redisWorkflow.setReferenceId(workflow.getReferenceId());
        redisWorkflow.setType(workflow.getType());
        redisWorkflow.setState(workflow.getState());
        redisWorkflow.setComment(workflow.getComment());
        redisWorkflow.setUser(workflow.getUser());
        redisWorkflow.setCreatedAt(workflow.getCreatedAt());
        return redisWorkflow;
    }
}
