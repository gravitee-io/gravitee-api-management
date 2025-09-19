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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.Workflow.AuditEvent.API_REVIEW_ACCEPTED;
import static io.gravitee.repository.management.model.Workflow.AuditEvent.API_REVIEW_ASKED;
import static io.gravitee.repository.management.model.Workflow.AuditEvent.API_REVIEW_REJECTED;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
import static java.util.stream.Collectors.toSet;

import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.ReviewEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.WorkflowReferenceType;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.NotificationParamsBuilder;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiWorkflowStateService;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class ApiWorkflowStateServiceImpl implements ApiWorkflowStateService {

    private final AuditService auditService;
    private final ApiMetadataService apiMetadataService;
    private final WorkflowService workflowService;
    private final RoleService roleService;
    private final UserService userService;
    private final NotifierService notifierService;
    private final MembershipService membershipService;
    private final EmailService emailService;
    private final ApiSearchService apiSearchService;
    private final ParameterService parameterService;

    public ApiWorkflowStateServiceImpl(
        final AuditService auditService,
        @Lazy final ApiMetadataService apiMetadataService,
        final WorkflowService workflowService,
        @Lazy final RoleService roleService,
        final UserService userService,
        final NotifierService notifierService,
        final MembershipService membershipService,
        final EmailService emailService,
        final ApiSearchService apiSearchService,
        final ParameterService parameterService
    ) {
        this.auditService = auditService;
        this.apiMetadataService = apiMetadataService;
        this.workflowService = workflowService;
        this.roleService = roleService;
        this.userService = userService;
        this.notifierService = notifierService;
        this.membershipService = membershipService;
        this.emailService = emailService;
        this.apiSearchService = apiSearchService;
        this.parameterService = parameterService;
    }

    @Override
    public GenericApiEntity askForReview(
        ExecutionContext executionContext,
        final String apiId,
        final String userId,
        final ReviewEntity reviewEntity
    ) {
        log.debug("Ask for review API {}", apiId);
        return updateWorkflowReview(executionContext, apiId, userId, ApiHook.ASK_FOR_REVIEW, WorkflowState.IN_REVIEW, reviewEntity);
    }

    @Override
    public GenericApiEntity acceptReview(
        ExecutionContext executionContext,
        final String apiId,
        final String userId,
        final ReviewEntity reviewEntity
    ) {
        log.debug("Accept review API {}", apiId);
        return updateWorkflowReview(executionContext, apiId, userId, ApiHook.REVIEW_OK, WorkflowState.REVIEW_OK, reviewEntity);
    }

    @Override
    public GenericApiEntity rejectReview(
        ExecutionContext executionContext,
        final String apiId,
        final String userId,
        final ReviewEntity reviewEntity
    ) {
        log.debug("Reject review API {}", apiId);
        return updateWorkflowReview(
            executionContext,
            apiId,
            userId,
            ApiHook.REQUEST_FOR_CHANGES,
            WorkflowState.REQUEST_FOR_CHANGES,
            reviewEntity
        );
    }

    private GenericApiEntity updateWorkflowReview(
        ExecutionContext executionContext,
        final String apiId,
        final String userId,
        final ApiHook hook,
        final WorkflowState workflowState,
        final ReviewEntity reviewEntity
    ) {
        final String workflowMessage = reviewEntity == null ? null : reviewEntity.getMessage();
        Workflow workflow = workflowService.create(WorkflowReferenceType.API, apiId, REVIEW, userId, workflowState, workflowMessage);

        // Get updated API with new workflow state for notification
        GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiId);

        final UserEntity user = userService.findById(executionContext, userId);
        GenericApiEntity apiWithMetadata = apiMetadataService.fetchMetadataForApi(executionContext, genericApiEntity);
        notifierService.trigger(
            executionContext,
            hook,
            genericApiEntity.getId(),
            new NotificationParamsBuilder().api(apiWithMetadata).user(user).build()
        );

        // Find all reviewers of the API and send them a notification email
        if (hook.equals(ApiHook.ASK_FOR_REVIEW)) {
            List<String> reviewersEmail = findAllReviewersEmail(executionContext, genericApiEntity);
            if (reviewersEmail.size() > 0) {
                this.emailService.sendAsyncEmailNotification(
                    executionContext,
                    new EmailNotificationBuilder()
                        .params(new NotificationParamsBuilder().api(genericApiEntity).user(user).build())
                        .to(reviewersEmail.toArray(new String[reviewersEmail.size()]))
                        .template(EmailNotificationBuilder.EmailTemplate.API_ASK_FOR_REVIEW)
                        .build()
                );
            }
        }

        Map<Audit.AuditProperties, String> properties = new HashMap<>();
        properties.put(Audit.AuditProperties.USER, userId);
        properties.put(Audit.AuditProperties.API, genericApiEntity.getId());

        Workflow.AuditEvent evtType = null;
        switch (workflowState) {
            case REQUEST_FOR_CHANGES:
                evtType = API_REVIEW_REJECTED;
                break;
            case REVIEW_OK:
                evtType = API_REVIEW_ACCEPTED;
                break;
            default:
                evtType = API_REVIEW_ASKED;
                break;
        }

        auditService.createApiAuditLog(executionContext, genericApiEntity.getId(), properties, evtType, new Date(), null, workflow);
        return genericApiEntity;
    }

    private List<String> findAllReviewersEmail(ExecutionContext executionContext, GenericApiEntity genericApiEntity) {
        final RolePermissionAction[] acls = { RolePermissionAction.UPDATE };
        final boolean isTrialInstance = parameterService.findAsBoolean(executionContext, Key.TRIAL_INSTANCE, ParameterReferenceType.SYSTEM);
        final Predicate<UserEntity> excludeIfTrialAndNotOptedIn = userEntity -> !isTrialInstance || userEntity.optedIn();

        // find direct members of the API
        Set<String> reviewerEmails = roleService
            .findByScope(RoleScope.API, executionContext.getOrganizationId())
            .stream()
            .filter(role -> this.roleService.hasPermission(role.getPermissions(), ApiPermission.REVIEWS, acls))
            .flatMap(role ->
                this.membershipService.getMembershipsByReferenceAndRole(
                    MembershipReferenceType.API,
                    genericApiEntity.getId(),
                    role.getId()
                ).stream()
            )
            .filter(m -> m.getMemberType().equals(MembershipMemberType.USER))
            .map(MembershipEntity::getMemberId)
            .distinct()
            .map(id -> this.userService.findById(executionContext, id))
            .filter(excludeIfTrialAndNotOptedIn)
            .map(UserEntity::getEmail)
            .filter(Objects::nonNull)
            .collect(toSet());

        // find reviewers in group attached to the API
        final Set<String> groups = genericApiEntity.getGroups();
        if (groups != null && !groups.isEmpty()) {
            groups.forEach(group -> {
                reviewerEmails.addAll(
                    roleService
                        .findByScope(RoleScope.API, executionContext.getOrganizationId())
                        .stream()
                        .filter(role -> this.roleService.hasPermission(role.getPermissions(), ApiPermission.REVIEWS, acls))
                        .flatMap(role ->
                            this.membershipService.getMembershipsByReferenceAndRole(
                                MembershipReferenceType.GROUP,
                                group,
                                role.getId()
                            ).stream()
                        )
                        .filter(m -> m.getMemberType().equals(MembershipMemberType.USER))
                        .map(MembershipEntity::getMemberId)
                        .distinct()
                        .map(id -> this.userService.findById(executionContext, id))
                        .filter(excludeIfTrialAndNotOptedIn)
                        .map(UserEntity::getEmail)
                        .filter(Objects::nonNull)
                        .collect(toSet())
                );
            });
        }

        return new ArrayList<>(reviewerEmails);
    }
}
