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
package io.gravitee.rest.api.service;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.MessageRecipientFormatException;
import io.gravitee.rest.api.service.impl.MessageServiceImpl;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageService_GetRecipientIdsTest {

    @InjectMocks
    private MessageServiceImpl messageService = new MessageServiceImpl();

    @Mock
    ApiRepository mockApiRepository;

    @Mock
    GroupService mockGroupService;

    @Mock
    RoleService mockRoleService;

    @Mock
    MembershipService mockMembershipService;

    @Mock
    ApplicationService mockApplicationService;

    @Mock
    SubscriptionRepository mockSubscriptionRepository;

    @Mock
    SubscriptionService subscriptionService;

    @Before
    public void init() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldThrowExceptionIfNull() {
        shouldThrowException(null, null);
        shouldThrowException("xxx", null);

        MessageEntity messageEntity = new MessageEntity();
        shouldThrowException(null, messageEntity);
        shouldThrowException("xxx", messageEntity);

        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageEntity.setRecipient(messageRecipientEntity);
        shouldThrowException("xxx", messageEntity);

        messageRecipientEntity.setRoleScope("API");
        messageRecipientEntity.setRoleValues(Collections.emptyList());
        shouldThrowException("xxx", messageEntity);
    }

    private void shouldThrowException(String apiId, MessageEntity message) {
        try {
            Api api = new Api();
            api.setId(apiId);
            messageService.getRecipientsId(GraviteeContext.getExecutionContext(), api, message);
            fail("should throw MessageRecipientFormatException");
        } catch (MessageRecipientFormatException ex) {
            // ok
        }
    }

    @Test
    public void shouldNotGetGlobal() throws Exception {
        shouldNotGetGlobal("API");
        shouldNotGetGlobal("APPLICATION");
        shouldNotGetGlobal("ORGANIZATIION");
    }

    private void shouldNotGetGlobal(String scope) throws Exception {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope(scope);
        messageRecipientEntity.setRoleValues(Collections.singletonList("API_PUBLISHER"));
        messageEntity.setRecipient(messageRecipientEntity);

        messageService.getRecipientsId(GraviteeContext.getExecutionContext(), messageEntity);

        verify(mockGroupService, never()).findById(eq(GraviteeContext.getExecutionContext().getEnvironmentId()), any());
        verify(mockMembershipService, never()).getMembershipsByReferenceAndRole(any(), any(), any());
        verify(mockMembershipService, never()).getMembershipsByReferencesAndRole(any(), any(), any());
        verify(mockRoleService, never()).findByScopeAndName(any(), any());
        verify(mockSubscriptionRepository, never()).search(any());
    }

    @Test
    public void shouldGetGlobalAPIPublisher() throws Exception {
        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId("user-id");
        membership.setMemberType(MembershipMemberType.USER);
        when(mockMembershipService.getMembershipsByReferenceAndRole(eq(MembershipReferenceType.ENVIRONMENT), eq("DEFAULT"), any()))
            .thenReturn(Collections.singleton(membership));

        when(mockRoleService.findByScopeAndName(RoleScope.ENVIRONMENT, "API_PUBLISHER")).thenReturn(Optional.of(mock(RoleEntity.class)));

        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope("ENVIRONMENT");
        messageRecipientEntity.setRoleValues(Collections.singletonList("API_PUBLISHER"));
        messageEntity.setRecipient(messageRecipientEntity);

        Set<String> recipientIds = messageService.getRecipientsId(GraviteeContext.getExecutionContext(), messageEntity);

        assertNotNull("not null", recipientIds);
        assertEquals("size=1", 1, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        verify(mockGroupService, never()).findById(eq(GraviteeContext.getCurrentEnvironment()), any());
        verify(mockMembershipService, times(1)).getMembershipsByReferenceAndRole(any(), any(), any());
        verify(mockMembershipService, never()).getMembershipsByReferencesAndRole(any(), any(), any());
        verify(mockRoleService, times(1)).findByScopeAndName(RoleScope.ENVIRONMENT, "API_PUBLISHER");
        verify(mockSubscriptionRepository, never()).search(any());
    }

    @Test
    public void shouldNotGetSpecific() throws Exception {
        shouldNotGetSpecific("API");
        shouldNotGetSpecific("ENVIRONMENT");
    }

    private void shouldNotGetSpecific(String scope) throws Exception {
        Api api = new Api();
        api.setId("api-id");
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope(scope);
        messageRecipientEntity.setRoleValues(Collections.singletonList("API_PUBLISHER"));
        messageEntity.setRecipient(messageRecipientEntity);

        messageService.getRecipientsId(GraviteeContext.getExecutionContext(), api, messageEntity);

        verify(mockGroupService, never()).findById(eq(GraviteeContext.getCurrentEnvironment()), any());
        verify(mockMembershipService, never()).getMembershipsByReferenceAndRole(any(), any(), any());
        verify(mockMembershipService, never()).getMembershipsByReferencesAndRole(any(), any(), any());
        verify(mockRoleService, never()).findByScopeAndName(any(), any());
        verify(mockSubscriptionRepository, never()).search(any());
    }

    @Test
    public void shouldGetApiConsumersWithoutGroups() throws TechnicalException {
        // given
        Subscription subscription = new Subscription();
        subscription.setApplication("app-id");
        when(mockSubscriptionRepository.search(any())).thenReturn(Collections.singletonList(subscription));
        when(mockRoleService.findByScopeAndName(RoleScope.APPLICATION, "OWNER")).thenReturn(Optional.of(mock(RoleEntity.class)));

        MembershipEntity membership = new MembershipEntity();
        membership.setMemberId("user-id");
        membership.setMemberType(MembershipMemberType.USER);
        when(
            mockMembershipService.getMembershipsByReferencesAndRole(
                eq(MembershipReferenceType.APPLICATION),
                eq(Arrays.asList("app-id")),
                any()
            )
        )
            .thenReturn(Collections.singleton(membership));

        // when
        Api api = new Api();
        api.setId("api-id");
        api.setGroups(Collections.emptySet());

        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope("APPLICATION");
        messageRecipientEntity.setRoleValues(Collections.singletonList("OWNER"));
        messageEntity.setRecipient(messageRecipientEntity);

        Set<String> recipientIds = messageService.getRecipientsId(GraviteeContext.getExecutionContext(), api, messageEntity);

        // then
        assertNotNull("not null", recipientIds);
        assertEquals("size=1", 1, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        verify(mockGroupService, never()).findById(eq(GraviteeContext.getCurrentEnvironment()), any());
        verify(mockMembershipService, never()).getMembershipsByReferenceAndRole(any(), any(), any());
        verify(mockMembershipService, times(1)).getMembershipsByReferencesAndRole(any(), any(), any());
        verify(mockRoleService, times(1)).findByScopeAndName(RoleScope.APPLICATION, "OWNER");
        verify(mockSubscriptionRepository, times(1)).search(any());
    }

    @Test
    public void shouldGetApiConsumersWithGroups() throws TechnicalException {
        Application app = new Application();
        app.setId("app-id");
        app.setGroups(new HashSet<>(Arrays.asList("group-id")));

        // given
        Subscription subscription = new Subscription();
        subscription.setApplication(app.getId());
        when(mockSubscriptionRepository.search(any())).thenReturn(Collections.singletonList(subscription));
        when(mockRoleService.findByScopeAndName(RoleScope.APPLICATION, "OWNER")).thenReturn(Optional.of(mock(RoleEntity.class)));

        ApplicationListItem appListItem = new ApplicationListItem();
        appListItem.setId(app.getId());
        appListItem.setGroups(app.getGroups());
        when(mockApplicationService.findByIds(GraviteeContext.getExecutionContext(), List.of(app.getId()))).thenReturn(Set.of(appListItem));

        MembershipEntity membershipGroup = new MembershipEntity();
        membershipGroup.setId("membership-group-id");
        membershipGroup.setMemberId("user-group-id");
        membershipGroup.setMemberType(MembershipMemberType.USER);

        MembershipEntity membership = new MembershipEntity();
        membershipGroup.setId("membership-user-id");
        membership.setMemberId("user-id");
        membership.setMemberType(MembershipMemberType.USER);
        when(
            mockMembershipService.getMembershipsByReferencesAndRole(
                eq(MembershipReferenceType.APPLICATION),
                eq(Arrays.asList("app-id")),
                any()
            )
        )
            .thenReturn(new HashSet(Arrays.asList(membership)));
        when(
            mockMembershipService.getMembershipsByReferencesAndRole(eq(MembershipReferenceType.GROUP), eq(Arrays.asList("group-id")), any())
        )
            .thenReturn(new HashSet(Arrays.asList(membershipGroup)));

        // when
        Api api = new Api();
        api.setId("api-id");

        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope("APPLICATION");
        messageRecipientEntity.setRoleValues(Collections.singletonList("OWNER"));
        messageEntity.setRecipient(messageRecipientEntity);

        Set<String> recipientIds = messageService.getRecipientsId(GraviteeContext.getExecutionContext(), api, messageEntity);

        // then
        assertNotNull("not null", recipientIds);
        assertEquals("size=2", 2, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        assertTrue("user=user-group-id", recipientIds.contains("user-group-id"));
        verify(mockMembershipService, never()).getMembershipsByReferenceAndRole(any(), any(), any());
        verify(mockMembershipService, times(2)).getMembershipsByReferencesAndRole(any(), any(), any());
        verify(mockRoleService, times(1)).findByScopeAndName(RoleScope.APPLICATION, "OWNER");
        verify(mockSubscriptionRepository, times(1)).search(any());
    }

    @Test
    public void shouldGetApiSubscribers() throws TechnicalException {
        // given
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setApplication("app-id");
        subscription.setSubscribedBy("user-id");
        when(subscriptionService.findByApi("api-id")).thenReturn(Collections.singletonList(subscription));

        MembershipEntity membershipGroup = new MembershipEntity();
        membershipGroup.setId("membership-group-id");
        membershipGroup.setMemberId("user-group-id");
        membershipGroup.setMemberType(MembershipMemberType.USER);

        MembershipEntity membership = new MembershipEntity();
        membershipGroup.setId("membership-user-id");
        membership.setMemberId("user-id");
        membership.setMemberType(MembershipMemberType.USER);

        // when
        Api api = new Api();
        api.setId("api-id");
        api.setGroups(new HashSet<>(Arrays.asList("group-id")));
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope("APPLICATION");
        messageRecipientEntity.setRoleValues(Collections.singletonList("API_SUBSCRIBERS"));
        messageEntity.setRecipient(messageRecipientEntity);

        Set<String> recipientIds = messageService.getRecipientsId(GraviteeContext.getExecutionContext(), api, messageEntity);

        // then
        assertNotNull("not null", recipientIds);
        assertEquals("size=1", 1, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        verify(mockMembershipService, never()).getMembershipsByReferenceAndRole(any(), any(), any());
    }
}
