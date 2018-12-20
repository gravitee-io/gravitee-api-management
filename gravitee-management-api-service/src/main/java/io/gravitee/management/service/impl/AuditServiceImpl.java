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
package io.gravitee.management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.UUID;
import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.audit.AuditEntity;
import io.gravitee.management.model.audit.AuditQuery;
import io.gravitee.management.service.AuditService;
import io.gravitee.management.service.UserService;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.management.service.exceptions.UserNotFoundException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.api.search.AuditCriteria.Builder;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.management.service.impl.MetadataServiceImpl.getDefautReferenceId;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AuditServiceImpl extends AbstractService implements AuditService {

    private final Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public MetadataPage<AuditEntity> search(AuditQuery query) {

        Audit.AuditReferenceType referenceType =
                query.isManagementLogsOnly() ? Audit.AuditReferenceType.PORTAL :
                        (query.getApiIds() != null && !query.getApiIds().isEmpty()) ? Audit.AuditReferenceType.API :
                                (query.getApplicationIds() != null && !query.getApplicationIds().isEmpty()) ? Audit.AuditReferenceType.APPLICATION :
                                        null;

        Builder criteria = new Builder().from(query.getFrom()).to(query.getTo());
        if (referenceType != null) {
            List<String> referenceIds;
            switch (referenceType) {
                case API:
                    referenceIds = query.getApiIds();
                    break;
                case APPLICATION:
                    referenceIds = query.getApplicationIds();
                    break;
                default:
                    referenceIds = Collections.singletonList("DEFAULT");
            }
            criteria.references(referenceType, referenceIds);
        }

        if (query.getEvents() != null && !query.getEvents().isEmpty()) {
            criteria.events(query.getEvents());
        }

        Page<Audit> auditPage = auditRepository.search(
                criteria.build(),
                new PageableBuilder().pageNumber(query.getPage() - 1).pageSize(query.getSize()).build());

        List<AuditEntity> content = auditPage.getContent().stream().map(this::convert).collect(Collectors.toList());

        return new MetadataPage<>(content, query.getPage(), query.getSize() , auditPage.getTotalElements(), getMetadata(content));
    }

    private Map<String, String> getMetadata(List<AuditEntity> content) {
        Map<String, String> metadata = new HashMap<>();
        for (AuditEntity auditEntity : content) {
            //add user's display name
            String metadataKey = "USER:"+auditEntity.getUser()+":name";
            try {
                UserEntity user = userService.findById(auditEntity.getUser());
                metadata.put(metadataKey, user.getDisplayName());
            } catch (TechnicalManagementException e) {
                LOGGER.error("Error finding metadata {}", auditEntity.getUser());
            } catch (UserNotFoundException unfe) {
                metadata.put(metadataKey, auditEntity.getUser());
            }

            //add property metadata
            String name;
            if (auditEntity.getProperties() != null) {
                for (Map.Entry<String, String> property : auditEntity.getProperties().entrySet()) {
                    metadataKey = new StringJoiner(":").
                            add(property.getKey()).
                            add(property.getValue()).
                            add("name").
                            toString();
                    if (!metadata.containsKey(metadataKey)) {
                        name = property.getValue();
                        try {
                            switch (Audit.AuditProperties.valueOf(property.getKey())) {
                                case PAGE:
                                    Optional<io.gravitee.repository.management.model.Page> optPage = pageRepository.findById(property.getValue());
                                    if (optPage.isPresent()) {
                                        name = optPage.get().getName();
                                    }
                                    break;
                                case PLAN:
                                    Optional<Plan> optPlan = planRepository.findById(property.getValue());
                                    if (optPlan.isPresent()) {
                                        name = optPlan.get().getName();
                                    }
                                    break;
                                case METADATA:
                                    MetadataReferenceType refType = (Audit.AuditReferenceType.API.name().equals(auditEntity.getReferenceType()))
                                            ? MetadataReferenceType.API :
                                            (Audit.AuditReferenceType.APPLICATION.name().equals(auditEntity.getReferenceType())) ?
                                                    MetadataReferenceType.APPLICATION :
                                                    MetadataReferenceType.DEFAULT;
                                    String refId = refType.equals(MetadataReferenceType.DEFAULT) ? getDefautReferenceId() : auditEntity.getReferenceId();

                                    Optional<Metadata> optMetadata = metadataRepository.findById(property.getValue(), refId, refType);
                                    if (optMetadata.isPresent()) {
                                        name = optMetadata.get().getName();
                                    }
                                    break;
                                case GROUP:
                                    Optional<Group> optGroup = groupRepository.findById(property.getValue());
                                    if (optGroup.isPresent()) {
                                        name = optGroup.get().getName();
                                    }
                                    break;
                                case USER:
                                    try {
                                        UserEntity user = userService.findById(property.getValue());
                                        name = user.getDisplayName();
                                    } catch (UserNotFoundException unfe) {
                                        name = property.getValue();
                                    }
                                default:
                                    break;
                            }
                        } catch (TechnicalException e) {
                            LOGGER.error("Error finding metadata {}", metadataKey);
                            name = property.getValue();
                        }
                        metadata.put(metadataKey, name);
                    }
                }
            }
        }
        return metadata;
    }

    @Override
    public void createApiAuditLog(String apiId, Map<Audit.AuditProperties,String> properties, Audit.AuditEvent event, Date createdAt,
                                  Object oldValue, Object newValue) {
        create(Audit.AuditReferenceType.API,
                apiId,
                properties,
                event,
                getAuthenticatedUsernameOrSystem(),
                createdAt==null ? new Date() : createdAt,
                oldValue,
                newValue);
    }

    @Override
    public void createApplicationAuditLog(String applicationId, Map<Audit.AuditProperties,String> properties, Audit.AuditEvent event, Date createdAt,
                                          Object oldValue, Object newValue) {
        createApplicationAuditLog(
                applicationId,
                properties,
                event,
                getAuthenticatedUsernameOrSystem(),
                createdAt,
                oldValue,
                newValue);
    }

    @Override
    public void createApplicationAuditLog(String applicationId, Map<Audit.AuditProperties,String> properties, Audit.AuditEvent event, String userId, Date createdAt,
                                          Object oldValue, Object newValue) {
        create(Audit.AuditReferenceType.APPLICATION,
                applicationId,
                properties,
                event,
                userId,
                createdAt==null ? new Date() : createdAt,
                oldValue,
                newValue);
    }

    @Override
    public void createPortalAuditLog(Map<Audit.AuditProperties, String> properties, Audit.AuditEvent event, Date createdAt, Object oldValue, Object newValue) {
       createPortalAuditLog(
               properties,
               event,
               getAuthenticatedUsernameOrSystem(),
               createdAt,
               oldValue,
               newValue);
    }
    @Override
    public void createPortalAuditLog(Map<Audit.AuditProperties, String> properties, Audit.AuditEvent event, String userId, Date createdAt, Object oldValue, Object newValue) {
        create(Audit.AuditReferenceType.PORTAL,
                "DEFAULT",
                properties,
                event,
                userId,
                createdAt==null ? new Date() : createdAt,
                oldValue,
                newValue);
    }

    @Async
    protected void create(Audit.AuditReferenceType referenceType, String referenceId, Map<Audit.AuditProperties,String> properties,
                          Audit.AuditEvent event, String userId, Date createdAt,
                          Object oldValue, Object newValue) {

        Audit audit = new Audit();
        audit.setId(UUID.toString(UUID.random()));
        audit.setUser(userId);
        audit.setCreatedAt(createdAt);
        if (properties != null) {
            Map<String, String> stringStringMap = new HashMap<>(properties.size());
            properties.forEach((auditProperties, s) -> stringStringMap.put(auditProperties.name(), s));
            audit.setProperties(stringStringMap);
        }
        audit.setReferenceType(referenceType);
        audit.setReferenceId(referenceId);
        audit.setEvent(event.name());

        ObjectNode oldNode = oldValue == null
                ? mapper.createObjectNode()
                : mapper.convertValue(oldValue, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt"));
        ObjectNode newNode = newValue == null
                ? mapper.createObjectNode()
                : mapper.convertValue(newValue, ObjectNode.class).remove(Arrays.asList("updatedAt", "createdAt"));

        audit.setPatch(JsonDiff.asJson(oldNode, newNode).toString());

        try {
            auditRepository.create(audit);
        } catch (TechnicalException e) {
            LOGGER.error("Error occurs during the creation of an Audit Log {}.", e);
        }
    }

    private AuditEntity convert(Audit audit) {
        AuditEntity auditEntity = new AuditEntity();

        auditEntity.setReferenceType(audit.getReferenceType().name());
        auditEntity.setReferenceId(audit.getReferenceId());
        auditEntity.setEvent(audit.getEvent());
        auditEntity.setProperties(audit.getProperties());
        auditEntity.setUser(audit.getUser());
        auditEntity.setId(audit.getId());
        auditEntity.setPatch(audit.getPatch());
        auditEntity.setCreatedAt(audit.getCreatedAt());

        return auditEntity;
    }

    private String getAuthenticatedUsernameOrSystem() {
        return isAuthenticated() ? getAuthenticatedUsername() : "system";
    }
}
