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
package io.gravitee.gateway.handlers.api.manager.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.manager.DeployedApprovalState;
import java.util.List;

/**
 * The JSON form of an approval state the distributed sync carries, with the organization the primary node resolved.
 * It is its own wire record, read ignoring the properties it does not know, so gateways of different versions can share
 * it.
 */
public final class ApprovalStateJson {

    private final ObjectMapper objectMapper;

    public ApprovalStateJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** The distributed sync payload: the state with the organization the license check needs. */
    public String write(String organizationId, DeployedApprovalState state) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new Payload(organizationId, State.of(state)));
    }

    /** @throws IllegalArgumentException when the payload names no organization or no state */
    public Entry read(String payload) throws JsonProcessingException {
        Payload read = objectMapper.readValue(payload, Payload.class);
        if (isBlank(read.organizationId()) || read.state() == null || isBlank(read.state().id())) {
            throw new IllegalArgumentException("Distributed approval state without organization or state");
        }
        return new Entry(read.organizationId(), read.state().toDeployed());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** A state read from the store, with the organization it belongs to. */
    public record Entry(String organizationId, DeployedApprovalState state) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Payload(String organizationId, State state) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record State(
        String id,
        List<String> aliasIds,
        String environmentId,
        String apiId,
        String ruleId,
        String ruleUpdatedAt,
        String openKey,
        String status,
        Long expiresAt,
        String reason,
        Grant grant,
        ExecutorLease executor,
        long keepUntil,
        long createdAt,
        Long startedAt,
        long updatedAt,
        long version
    ) {
        static State of(DeployedApprovalState state) {
            return new State(
                state.id(),
                state.aliasIds(),
                state.environmentId(),
                state.apiId(),
                state.ruleId(),
                state.ruleUpdatedAt(),
                state.openKey(),
                state.status(),
                state.expiresAt(),
                state.reason(),
                state.grant() == null ? null : Grant.of(state.grant()),
                state.executor() == null ? null : ExecutorLease.of(state.executor()),
                state.keepUntil(),
                state.createdAt(),
                state.startedAt(),
                state.updatedAt(),
                state.version()
            );
        }

        DeployedApprovalState toDeployed() {
            return new DeployedApprovalState(
                id,
                aliasIds,
                environmentId,
                apiId,
                ruleId,
                ruleUpdatedAt,
                openKey,
                status,
                expiresAt,
                reason,
                grant == null ? null : grant.toDeployed(),
                executor == null ? null : executor.toDeployed(),
                keepUntil,
                createdAt,
                startedAt,
                updatedAt,
                version
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Grant(String scope, Long coversUntil, String applicationId, String subject, String argumentsDigest) {
        static Grant of(DeployedApprovalState.Grant grant) {
            return new Grant(grant.scope(), grant.coversUntil(), grant.applicationId(), grant.subject(), grant.argumentsDigest());
        }

        DeployedApprovalState.Grant toDeployed() {
            return new DeployedApprovalState.Grant(scope, coversUntil, applicationId, subject, argumentsDigest);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExecutorLease(String nodeId, String leaseId, long leaseUntil) {
        static ExecutorLease of(DeployedApprovalState.ExecutorLease executor) {
            return new ExecutorLease(executor.nodeId(), executor.leaseId(), executor.leaseUntil());
        }

        DeployedApprovalState.ExecutorLease toDeployed() {
            return new DeployedApprovalState.ExecutorLease(nodeId, leaseId, leaseUntil);
        }
    }
}
