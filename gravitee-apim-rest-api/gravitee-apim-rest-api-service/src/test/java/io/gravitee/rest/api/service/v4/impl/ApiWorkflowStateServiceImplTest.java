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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.model.Workflow;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.ReviewEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.WorkflowReferenceType;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EmailNotification;
import io.gravitee.rest.api.service.EmailService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiWorkflowStateService;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private ParameterService parameterService;

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
        apiWorkflowStateService = new ApiWorkflowStateServiceImpl(
            auditService,
            apiMetadataService,
            workflowService,
            roleService,
            userService,
            notifierService,
            membershipService,
            emailService,
            apiSearchService,
            parameterService
        );
    }

    @Test
    public void shouldAskForReview() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");

        when(
            workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, reviewEntity.getMessage())
        ).thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(new UserEntity());

        apiWorkflowStateService.askForReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService).create(
            WorkflowReferenceType.API,
            API_ID,
            REVIEW,
            USER_ID,
            WorkflowState.IN_REVIEW,
            reviewEntity.getMessage()
        );
        verify(auditService).createApiAuditLog(
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
    public void shouldAskForReviewAndSendMail() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");

        when(
            workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, reviewEntity.getMessage())
        ).thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(
            UserEntity.builder().email("test@gio.gio").status("ACTIVE").password("password").build()
        );
        when(roleService.findByScope(any(), any())).thenReturn(List.of(new RoleEntity()));
        when(roleService.hasPermission(any(), eq(ApiPermission.REVIEWS), any())).thenReturn(true);
        when(membershipService.getMembershipsByReferenceAndRole(eq(MembershipReferenceType.API), eq(API_ID), any())).thenReturn(
            Set.of(MembershipEntity.builder().id(USER_ID).memberType(MembershipMemberType.USER).build())
        );
        when(parameterService.findAsBoolean(any(), eq(Key.TRIAL_INSTANCE), eq(ParameterReferenceType.SYSTEM))).thenReturn(false);
        apiWorkflowStateService.askForReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService).create(
            WorkflowReferenceType.API,
            API_ID,
            REVIEW,
            USER_ID,
            WorkflowState.IN_REVIEW,
            reviewEntity.getMessage()
        );
        verify(auditService).createApiAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(apiId -> apiId.equals(API_ID)),
            anyMap(),
            argThat(evt -> Workflow.AuditEvent.API_REVIEW_ASKED.equals(evt)),
            any(),
            any(),
            any()
        );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
        final ArgumentCaptor<EmailNotification> emailNotificationArgumentCaptor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(emailService).sendAsyncEmailNotification(
            eq(GraviteeContext.getExecutionContext()),
            emailNotificationArgumentCaptor.capture()
        );
        assertThat(emailNotificationArgumentCaptor.getValue()).satisfies(emailNotification ->
            assertThat(emailNotification.getTo()).containsExactly("test@gio.gio")
        );
    }

    @Test
    public void shouldAskForReviewAndSendMailForOptedInUserInTrialInstance() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");

        when(
            workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, reviewEntity.getMessage())
        ).thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(
            UserEntity.builder().email("test@gio.gio").status("ACTIVE").password("password").build()
        );
        when(roleService.findByScope(any(), any())).thenReturn(List.of(new RoleEntity()));
        when(roleService.hasPermission(any(), eq(ApiPermission.REVIEWS), any())).thenReturn(true);
        when(membershipService.getMembershipsByReferenceAndRole(eq(MembershipReferenceType.API), eq(API_ID), any())).thenReturn(
            Set.of(MembershipEntity.builder().id(USER_ID).memberType(MembershipMemberType.USER).build())
        );
        when(parameterService.findAsBoolean(any(), eq(Key.TRIAL_INSTANCE), eq(ParameterReferenceType.SYSTEM))).thenReturn(true);
        apiWorkflowStateService.askForReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService).create(
            WorkflowReferenceType.API,
            API_ID,
            REVIEW,
            USER_ID,
            WorkflowState.IN_REVIEW,
            reviewEntity.getMessage()
        );
        verify(auditService).createApiAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(apiId -> apiId.equals(API_ID)),
            anyMap(),
            argThat(evt -> Workflow.AuditEvent.API_REVIEW_ASKED.equals(evt)),
            any(),
            any(),
            any()
        );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
        final ArgumentCaptor<EmailNotification> emailNotificationArgumentCaptor = ArgumentCaptor.forClass(EmailNotification.class);
        verify(emailService).sendAsyncEmailNotification(
            eq(GraviteeContext.getExecutionContext()),
            emailNotificationArgumentCaptor.capture()
        );
        assertThat(emailNotificationArgumentCaptor.getValue()).satisfies(emailNotification ->
            assertThat(emailNotification.getTo()).containsExactly("test@gio.gio")
        );
    }

    @Test
    public void shouldAskForReviewAndNotSendMailForNonOptedInUserInTrialInstance() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");

        when(
            workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, reviewEntity.getMessage())
        ).thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(
            UserEntity.builder().email("test@gio.gio").status("PENDING").password("password").build()
        );
        when(roleService.findByScope(any(), any())).thenReturn(List.of(new RoleEntity()));
        when(roleService.hasPermission(any(), eq(ApiPermission.REVIEWS), any())).thenReturn(true);
        when(membershipService.getMembershipsByReferenceAndRole(eq(MembershipReferenceType.API), eq(API_ID), any())).thenReturn(
            Set.of(MembershipEntity.builder().id(USER_ID).memberType(MembershipMemberType.USER).build())
        );
        when(parameterService.findAsBoolean(any(), eq(Key.TRIAL_INSTANCE), eq(ParameterReferenceType.SYSTEM))).thenReturn(true);
        apiWorkflowStateService.askForReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService).create(
            WorkflowReferenceType.API,
            API_ID,
            REVIEW,
            USER_ID,
            WorkflowState.IN_REVIEW,
            reviewEntity.getMessage()
        );
        verify(auditService).createApiAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(apiId -> apiId.equals(API_ID)),
            anyMap(),
            argThat(evt -> Workflow.AuditEvent.API_REVIEW_ASKED.equals(evt)),
            any(),
            any(),
            any()
        );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
        verify(emailService, never()).sendAsyncEmailNotification(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldAskForReviewWithNoMessage() {
        final GenericApiEntity genericApiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        genericApiEntity.setId(API_ID);

        when(workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, null)).thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(new UserEntity());

        apiWorkflowStateService.askForReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, null);

        verify(workflowService).create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.IN_REVIEW, null);
        verify(auditService).createApiAuditLog(
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

        when(
            workflowService.create(WorkflowReferenceType.API, API_ID, REVIEW, USER_ID, WorkflowState.REVIEW_OK, reviewEntity.getMessage())
        ).thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(new UserEntity());

        apiWorkflowStateService.acceptReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService).create(
            WorkflowReferenceType.API,
            API_ID,
            REVIEW,
            USER_ID,
            WorkflowState.REVIEW_OK,
            reviewEntity.getMessage()
        );
        verify(auditService).createApiAuditLog(
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
        ).thenReturn(null);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(genericApiEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(new UserEntity());

        apiWorkflowStateService.rejectReview(GraviteeContext.getExecutionContext(), API_ID, USER_ID, reviewEntity);

        verify(workflowService).create(
            WorkflowReferenceType.API,
            API_ID,
            REVIEW,
            USER_ID,
            WorkflowState.REQUEST_FOR_CHANGES,
            reviewEntity.getMessage()
        );
        verify(auditService).createApiAuditLog(
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
