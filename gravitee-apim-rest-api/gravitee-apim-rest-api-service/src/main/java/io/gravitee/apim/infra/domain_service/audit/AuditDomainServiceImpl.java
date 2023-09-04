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
package io.gravitee.apim.infra.domain_service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.gravitee.apim.core.audit.crud_service.AuditCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.*;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditDomainServiceImpl implements AuditDomainService {

    private final AuditCrudService auditCrudService;
    private final UserCrudService userCrudService;
    private final ObjectMapper mapper;

    public AuditDomainServiceImpl(AuditCrudService auditCrudService, UserCrudService userCrudService, ObjectMapper mapper) {
        this.auditCrudService = auditCrudService;
        this.userCrudService = userCrudService;
        this.mapper = mapper;
    }

    @Override
    public void createApiAuditLog(ApiAuditLogEntity audit) {
        try {
            var entity = AuditEntity
                .builder()
                .id(UuidString.generateRandom())
                .organizationId(audit.organizationId())
                .environmentId(audit.environmentId())
                .createdAt(audit.createdAt())
                .user(createActor(audit.actor()))
                .properties(adaptAuditLogProperties(audit.properties()))
                .referenceType(AuditEntity.AuditReferenceType.API)
                .referenceId(audit.apiId())
                .event(audit.event().name())
                .patch(createDiff(audit.oldValue(), audit.newValue()))
                .build();

            auditCrudService.create(entity);
        } catch (TechnicalManagementException e) {
            log.error("Error occurs during the creation of an API Audit Log.", e);
        }
    }

    @Override
    public void createApplicationAuditLog(ApplicationAuditLogEntity audit) {
        try {
            var entity = AuditEntity
                .builder()
                .id(UuidString.generateRandom())
                .organizationId(audit.organizationId())
                .environmentId(audit.environmentId())
                .createdAt(audit.createdAt())
                .user(createActor(audit.actor()))
                .properties(adaptAuditLogProperties(audit.properties()))
                .referenceType(AuditEntity.AuditReferenceType.APPLICATION)
                .referenceId(audit.applicationId())
                .event(audit.event().name())
                .patch(createDiff(audit.oldValue(), audit.newValue()))
                .build();

            auditCrudService.create(entity);
        } catch (TechnicalManagementException e) {
            log.error("Error occurs during the creation of an Application Audit Log.", e);
        }
    }

    /**
     * Create a JSON patch from the old and new values of the audit.
     * @param oldValue the old value.
     * @param newValue the new value.
     * @return the JSON patch showing the diff between old value and new one.
     */
    private String createDiff(Object oldValue, Object newValue) {
        ObjectNode oldNode = oldValue == null
            ? mapper.createObjectNode()
            : mapper.convertValue(oldValue, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt"));
        ObjectNode newNode = newValue == null
            ? mapper.createObjectNode()
            : mapper.convertValue(newValue, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt"));
        return JsonDiff.asJson(oldNode, newNode).toString();
    }

    private String createActor(AuditActor actor) {
        String user = actor.userId();
        if ("token".equals(actor.userSource())) {
            user =
                userCrudService.getBaseUser(actor.userId()).displayName() + String.format(" - (using token \"%s\")", actor.userSourceId());
        }
        return user;
    }

    private Map<String, String> adaptAuditLogProperties(Map<AuditProperties, String> audit) {
        return audit
            .entrySet()
            .stream()
            .map(entry -> Map.entry(entry.getKey().name(), entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
