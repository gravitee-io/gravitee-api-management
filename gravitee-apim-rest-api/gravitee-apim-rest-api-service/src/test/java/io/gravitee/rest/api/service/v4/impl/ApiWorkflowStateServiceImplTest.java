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

import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.model.ReviewEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.WorkflowReferenceType;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiWorkflowStateService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiWorkflowStateServiceImplTest {

    private static final String API_ID = "id-api";
    private static final String USER_ID = "id-user";

    @Mock
    private AuditService auditService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private RoleService roleService;

    @Mock
    private UserService userService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private EmailService emailService;

    private ApiWorkflowStateService apiWorkflowStateService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Before
    public void setUp() {
        apiWorkflowStateService =
            new ApiWorkflowStateServiceImpl(
                auditService,
                apiMetadataService,
                workflowService,
                roleService,
                userService,
                notifierService,
                membershipService,
                emailService,
                apiSearchService
            );
    }

    @Test
    public void shouldAskForReview() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");

        when(workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, reviewEntity.getMessage()))
            .thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(any())).thenReturn(new UserEntity());

        apiWorkflowStateService.askForReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService)
            .create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, reviewEntity.getMessage());
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiId -> apiId.equals(API_ID)),
                anyMap(),
                argThat(evt -> Workflow.AuditEvent.API_REVIEW_ASKED.equals(evt)),
                any(),
                any(),
                any()
            );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
    }

    @Test
    public void shouldAskForReviewWithNoMessage() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        when(workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, null)).thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(any())).thenReturn(new UserEntity());

        apiWorkflowStateService.askForReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, null);

        verify(workflowService).create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, null);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiId -> apiId.equals(API_ID)),
                anyMap(),
                argThat(evt -> Workflow.AuditEvent.API_REVIEW_ASKED.equals(evt)),
                any(),
                any(),
                any()
            );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
    }

    @Test
    public void shouldAcceptReview() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");

        when(workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.REVIEW_OK, reviewEntity.getMessage()))
            .thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(any())).thenReturn(new UserEntity());

        apiWorkflowStateService.acceptReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService)
            .create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.REVIEW_OK, reviewEntity.getMessage());
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiId -> apiId.equals(API_ID)),
                anyMap(),
                argThat(evt -> Workflow.AuditEvent.API_REVIEW_ACCEPTED.equals(evt)),
                any(),
                any(),
                any()
            );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
    }

    @Test
    public void shouldRejectReview() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");

        when(
            workflowService.create(
                WorkflowReferenceType.API,
                API_ID,
                REVIEW,
                USER_ID,
                WorkflowState.REQUEST_FOR_CHANGES,
                reviewEntity.getMessage()
            )
        )
            .thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(any())).thenReturn(new UserEntity());

        apiWorkflowStateService.rejectReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService)
            .create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.REQUEST_FOR_CHANGES, reviewEntity.getMessage());
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiId -> apiId.equals(API_ID)),
                anyMap(),
                argThat(evt -> Workflow.AuditEvent.API_REVIEW_REJECTED.equals(evt)),
                any(),
                any(),
                any()
            );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
    }
}
