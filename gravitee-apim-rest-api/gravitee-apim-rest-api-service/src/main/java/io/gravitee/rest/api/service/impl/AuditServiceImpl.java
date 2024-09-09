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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.rest.api.service.impl.MetadataServiceImpl.getDefaultReferenceId;
import static java.util.Map.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.*;
import io.gravitee.repository.management.api.search.AuditCriteria.Builder;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import io.gravitee.rest.api.model.audit.AuditReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AuditServiceImpl extends AbstractService implements AuditService {

    private final Logger LOGGER = LoggerFactory.getLogger(AuditServiceImpl.class);

    private static final Map<Audit.AuditReferenceType, AuditReferenceType> AUDIT_REFERENCE_TYPE_AUDIT_REFERENCE_TYPE_MAP = Map.ofEntries(
        entry(Audit.AuditReferenceType.ORGANIZATION, AuditReferenceType.ORGANIZATION),
        entry(Audit.AuditReferenceType.ENVIRONMENT, AuditReferenceType.ENVIRONMENT),
        entry(Audit.AuditReferenceType.APPLICATION, AuditReferenceType.APPLICATION),
        entry(Audit.AuditReferenceType.API, AuditReferenceType.API)
    );

    @Lazy
    @Autowired
    private AuditRepository auditRepository;

    @Lazy
    @Autowired
    private PageRepository pageRepository;

    @Lazy
    @Autowired
    private PlanRepository planRepository;

    @Lazy
    @Autowired
    private MetadataRepository metadataRepository;

    @Lazy
    @Autowired
    private GroupRepository groupRepository;

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Lazy
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Lazy
    @Autowired
    private OrganizationRepository organizationRepository;

    @Lazy
    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    @Lazy
    private PermissionService permissionService;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public MetadataPage<AuditEntity> search(final ExecutionContext executionContext, AuditQuery query) {
        Builder criteria = new Builder().from(query.getFrom()).to(query.getTo());

        criteria.organizationId(executionContext.getOrganizationId());
        if (executionContext.hasEnvironmentId()) {
            criteria.environmentIds(Collections.singletonList(executionContext.getEnvironmentId()));
        }

        if (
            permissionService.hasPermission(
                executionContext,
                RolePermission.ORGANIZATION_AUDIT,
                executionContext.getOrganizationId(),
                RolePermissionAction.READ
            ) &&
            query.getReferenceType() != null &&
            query.getReferenceType().equals(AuditReferenceType.ORGANIZATION)
        ) {
            criteria.references(Audit.AuditReferenceType.ORGANIZATION, null);
        } else if (
            (query.getEnvironmentIds() != null && !query.getEnvironmentIds().isEmpty()) ||
            query.getReferenceType() != null &&
            query.getReferenceType().equals(AuditReferenceType.ENVIRONMENT)
        ) {
            criteria.references(Audit.AuditReferenceType.ENVIRONMENT, query.getEnvironmentIds());
        } else if (
            (query.getApplicationIds() != null && !query.getApplicationIds().isEmpty()) ||
            query.getReferenceType() != null &&
            query.getReferenceType().equals(AuditReferenceType.APPLICATION)
        ) {
            criteria.references(Audit.AuditReferenceType.APPLICATION, query.getApplicationIds());
        } else if (
            (query.getApiIds() != null && !query.getApiIds().isEmpty()) ||
            query.getReferenceType() != null &&
            query.getReferenceType().equals(AuditReferenceType.API)
        ) {
            criteria.references(Audit.AuditReferenceType.API, query.getApiIds());
        }

        if (query.getEvents() != null && !query.getEvents().isEmpty()) {
            criteria.events(query.getEvents());
        }

        Page<Audit> auditPage = auditRepository.search(
            criteria.build(),
            new PageableBuilder().pageNumber(query.getPage() - 1).pageSize(query.getSize()).build()
        );

        List<AuditEntity> content = auditPage.getContent().stream().map(this::convert).collect(Collectors.toList());

        return new MetadataPage<>(
            content,
            query.getPage(),
            query.getSize(),
            auditPage.getTotalElements(),
            getMetadata(executionContext, content)
        );
    }

    private Map<String, String> getMetadata(ExecutionContext executionContext, List<AuditEntity> content) {
        Map<String, String> metadata = new HashMap<>();
        for (AuditEntity auditEntity : content) {
            //add user's display name
            String metadataKey = "USER:" + auditEntity.getUser() + ":name";
            try {
                UserEntity user = userService.findById(executionContext, auditEntity.getUser());
                metadata.put(metadataKey, user.getDisplayName());
            } catch (TechnicalManagementException e) {
                LOGGER.error("Error finding metadata {}", auditEntity.getUser());
            } catch (UserNotFoundException unfe) {
                metadata.put(metadataKey, auditEntity.getUser());
            }

            if (Audit.AuditReferenceType.ORGANIZATION.name().equals(auditEntity.getReferenceType().name())) {
                metadataKey = "ORGANIZATION:" + auditEntity.getReferenceId() + ":name";
                if (!metadata.containsKey(metadataKey)) {
                    try {
                        Optional<Organization> optOrganization = organizationRepository.findById(auditEntity.getReferenceId());
                        if (optOrganization.isPresent()) {
                            metadata.put(metadataKey, optOrganization.get().getName());
                        }
                    } catch (TechnicalException e) {
                        LOGGER.error("Error finding metadata {}", metadataKey);
                        metadata.put(metadataKey, auditEntity.getReferenceId());
                    }
                }
            } else if (Audit.AuditReferenceType.ENVIRONMENT.name().equals(auditEntity.getReferenceType().name())) {
                metadataKey = "ENVIRONMENT:" + auditEntity.getReferenceId() + ":name";
                if (!metadata.containsKey(metadataKey)) {
                    try {
                        Optional<Environment> optEnvironment = environmentRepository.findById(auditEntity.getReferenceId());
                        if (optEnvironment.isPresent()) {
                            metadata.put(metadataKey, optEnvironment.get().getName());
                        }
                    } catch (TechnicalException e) {
                        LOGGER.error("Error finding metadata {}", metadataKey);
                        metadata.put(metadataKey, auditEntity.getReferenceId());
                    }
                }
            } else if (Audit.AuditReferenceType.APPLICATION.name().equals(auditEntity.getReferenceType().name())) {
                metadataKey = "APPLICATION:" + auditEntity.getReferenceId() + ":name";
                if (!metadata.containsKey(metadataKey)) {
                    try {
                        Optional<Application> optApp = applicationRepository.findById(auditEntity.getReferenceId());
                        if (optApp.isPresent()) {
                            metadata.put(metadataKey, optApp.get().getName());
                        }
                    } catch (TechnicalException e) {
                        LOGGER.error("Error finding metadata {}", metadataKey);
                        metadata.put(metadataKey, auditEntity.getReferenceId());
                    }
                }
            } else if (Audit.AuditReferenceType.API.name().equals(auditEntity.getReferenceType().name())) {
                metadataKey = "API:" + auditEntity.getReferenceId() + ":name";
                if (!metadata.containsKey(metadataKey)) {
                    try {
                        Optional<Api> optApi = apiRepository.findById(auditEntity.getReferenceId());
                        if (optApi.isPresent()) {
                            metadata.put(metadataKey, optApi.get().getName());
                        }
                    } catch (TechnicalException e) {
                        LOGGER.error("Error finding metadata {}", metadataKey);
                        metadata.put(metadataKey, auditEntity.getReferenceId());
                    }
                }
            }

            //add property metadata
            String name;
            if (auditEntity.getProperties() != null) {
                for (Map.Entry<String, String> property : auditEntity.getProperties().entrySet()) {
                    metadataKey = new StringJoiner(":").add(property.getKey()).add(property.getValue()).add("name").toString();
                    if (!metadata.containsKey(metadataKey)) {
                        name = property.getValue();
                        try {
                            switch (Audit.AuditProperties.valueOf(property.getKey())) {
                                case API:
                                    Optional<Api> optApi = apiRepository.findById(property.getValue());
                                    if (optApi.isPresent()) {
                                        name = optApi.get().getName();
                                    }
                                    break;
                                case APPLICATION:
                                    Optional<Application> optApp = applicationRepository.findById(property.getValue());
                                    if (optApp.isPresent()) {
                                        name = optApp.get().getName();
                                    }
                                    break;
                                case PAGE:
                                    Optional<io.gravitee.repository.management.model.Page> optPage = pageRepository.findById(
                                        property.getValue()
                                    );
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
                                    MetadataReferenceType refType = MetadataReferenceType.parse(auditEntity.getReferenceType().name());
                                    Optional<Metadata> optMetadata = metadataRepository.findById(
                                        property.getValue(),
                                        auditEntity.getReferenceId(),
                                        refType
                                    );
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
                                        UserEntity user = userService.findById(executionContext, property.getValue());
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
    public void createApiAuditLog(
        ExecutionContext executionContext,
        String apiId,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        createAuditLog(executionContext, Audit.AuditReferenceType.API, apiId, properties, event, createdAt, oldValue, newValue);
    }

    @Override
    public void createApplicationAuditLog(
        ExecutionContext executionContext,
        String applicationId,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        createAuditLog(
            executionContext,
            Audit.AuditReferenceType.APPLICATION,
            applicationId,
            properties,
            event,
            createdAt,
            oldValue,
            newValue
        );
    }

    @Override
    public void createAuditLog(
        ExecutionContext executionContext,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        if (executionContext.hasEnvironmentId()) {
            createEnvironmentAuditLog(
                executionContext,
                executionContext.getEnvironmentId(),
                properties,
                event,
                createdAt,
                oldValue,
                newValue
            );
        } else {
            createOrganizationAuditLog(
                executionContext,
                executionContext.getOrganizationId(),
                properties,
                event,
                createdAt,
                oldValue,
                newValue
            );
        }
    }

    private void createEnvironmentAuditLog(
        ExecutionContext executionContext,
        String environmentId,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        createAuditLog(
            executionContext,
            Audit.AuditReferenceType.ENVIRONMENT,
            environmentId,
            properties,
            event,
            createdAt,
            oldValue,
            newValue
        );
    }

    @Override
    public void createOrganizationAuditLog(
        ExecutionContext executionContext,
        final String organizationId,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        createAuditLog(
            executionContext,
            Audit.AuditReferenceType.ORGANIZATION,
            organizationId,
            properties,
            event,
            createdAt,
            oldValue,
            newValue
        );
    }

    @Async
    @Override
    public void createAuditLog(
        ExecutionContext executionContext,
        Audit.AuditReferenceType referenceType,
        String referenceId,
        Map<Audit.AuditProperties, String> properties,
        Audit.AuditEvent event,
        Date createdAt,
        Object oldValue,
        Object newValue
    ) {
        Audit audit = new Audit();
        audit.setId(UuidString.generateRandom());
        audit.setOrganizationId(executionContext.getOrganizationId());
        if (executionContext.hasEnvironmentId() && !referenceType.equals(Audit.AuditReferenceType.ORGANIZATION)) {
            audit.setEnvironmentId(executionContext.getEnvironmentId());
        }
        audit.setCreatedAt(createdAt == null ? new Date() : createdAt);

        final UserDetails authenticatedUser = getAuthenticatedUser();
        final String user;
        if (authenticatedUser != null && "token".equals(authenticatedUser.getSource())) {
            user =
                userService.findById(executionContext, authenticatedUser.getUsername()).getDisplayName() +
                " - (using token \"" +
                authenticatedUser.getSourceId() +
                "\")";
        } else {
            user = getAuthenticatedUsernameOrSystem();
        }
        audit.setUser(user);

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

        auditEntity.setReferenceType(AUDIT_REFERENCE_TYPE_AUDIT_REFERENCE_TYPE_MAP.get(audit.getReferenceType()));
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
