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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.model.*;
import io.gravitee.rest.api.model.MessageChannel;
import io.gravitee.rest.api.model.MessageEntity;
import io.gravitee.rest.api.model.MessageRecipientEntity;
import io.gravitee.rest.api.service.exceptions.MessageRecipientFormatException;
import io.gravitee.rest.api.service.impl.MessageServiceImpl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    MembershipRepository mockMembershipRepository;

    @Mock
    SubscriptionRepository mockSubscriptionRepository;

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
            messageService.getRecipientsId(api, message);
            fail("should throw MessageRecipientFormatException");
        } catch(MessageRecipientFormatException ex) {
            // ok
        }
    }

    @Test
    public void shouldNotGetGlobal() throws Exception {
        shouldNotGetGlobal("API");
        shouldNotGetGlobal("APPLICATION");
        shouldNotGetGlobal("PORTAL");
    }

    private void shouldNotGetGlobal(String scope) throws Exception {

        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope(scope);
        messageRecipientEntity.setRoleValues(Collections.singletonList("API_PUBLISHER"));
        messageEntity.setRecipient(messageRecipientEntity);

        messageService.getRecipientsId(messageEntity);

        verify(mockMembershipRepository, never()).findByRole(any(), any());
        verify(mockApiRepository, never()).findById(any());
        verify(mockSubscriptionRepository, never()).search(any());
        verify(mockMembershipRepository, never()).findByReferencesAndRole(any(), any(), any(), any());
    }

    @Test
    public void shouldGetGlobalAPIPublisher() throws Exception {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope("MANAGEMENT");
        messageRecipientEntity.setRoleValues(Collections.singletonList("API_PUBLISHER"));
        messageEntity.setRecipient(messageRecipientEntity);
        Membership membership = new Membership();
        membership.setUserId("user-id");
        when(mockMembershipRepository.findByRole(RoleScope.MANAGEMENT, "API_PUBLISHER"))
                .thenReturn(Collections.singleton(membership));

        Set<String> recipientIds = messageService.getRecipientsId(messageEntity);

        assertNotNull("not null", recipientIds);
        assertEquals("size=1", 1, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        verify(mockMembershipRepository, times(1)).findByRole(RoleScope.MANAGEMENT, "API_PUBLISHER");
        verify(mockApiRepository, never()).findById(any());
        verify(mockSubscriptionRepository, never()).search(any());
        verify(mockMembershipRepository, never()).findByReferencesAndRole(any(), any(), any(), any());
    }

    @Test
    public void shouldNotGetSpecific() throws Exception {
        shouldNotGetSpecific("API");
        shouldNotGetSpecific("PORTAL");
        shouldNotGetSpecific("MANAGEMENT");
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

        messageService.getRecipientsId(api, messageEntity);

        verify(mockMembershipRepository, never()).findByRole(any(), any());
        verify(mockApiRepository, never()).findById(any());
        verify(mockSubscriptionRepository, never()).search(any());
        verify(mockMembershipRepository, never()).findByReferencesAndRole(any(), any(), any(), any());
    }

    @Test
    public void shouldGetApiConsumersWithoutGroups() throws TechnicalException {
        Api api = new Api();
        api.setId("api-id");
        api.setGroups(Collections.emptySet());
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope("APPLICATION");
        messageRecipientEntity.setRoleValues(Collections.singletonList("OWNER"));
        messageEntity.setRecipient(messageRecipientEntity);
        Membership membership = new Membership();
        membership.setUserId("user-id");
        Subscription subscription = new Subscription();
        subscription.setApplication("app-id");
        when(mockSubscriptionRepository.search(any()))
                .thenReturn(Collections.singletonList(subscription));
        when(mockMembershipRepository.findByReferencesAndRole(eq(MembershipReferenceType.APPLICATION), any(), any(), any()))
                .thenReturn(Collections.singleton(membership));

        Set<String> recipientIds = messageService.getRecipientsId(api, messageEntity);

        assertNotNull("not null", recipientIds);
        assertEquals("size=1", 1, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        verify(mockMembershipRepository, never()).findByRole(any(), any());
        verify(mockSubscriptionRepository, times(1)).search(any());
        verify(mockMembershipRepository, never()).findByReferencesAndRole(eq(MembershipReferenceType.GROUP), any(), any(), any());
        verify(mockMembershipRepository, times(1)).findByReferencesAndRole(eq(MembershipReferenceType.APPLICATION), any(), any(), any());
    }

    @Test
    public void shouldGetApiConsumersWithGroups() throws TechnicalException {
        Api api = new Api();
        api.setId("api-id");
        api.setGroups(Collections.singleton("group-id"));
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChannel(MessageChannel.MAIL);
        MessageRecipientEntity messageRecipientEntity = new MessageRecipientEntity();
        messageRecipientEntity.setRoleScope("APPLICATION");
        messageRecipientEntity.setRoleValues(Collections.singletonList("OWNER"));
        messageEntity.setRecipient(messageRecipientEntity);
        Membership membershipGroup = new Membership();
        membershipGroup.setUserId("user-group-id");
        Membership membership = new Membership();
        membership.setUserId("user-id");
        Subscription subscription = new Subscription();
        subscription.setApplication("app-id");
        when(mockSubscriptionRepository.search(any()))
                .thenReturn(Collections.singletonList(subscription));
        when(mockMembershipRepository.findByReferencesAndRole(eq(MembershipReferenceType.APPLICATION), any(), any(), any()))
                .thenReturn(Collections.singleton(membership));
        when(mockMembershipRepository.findByReferencesAndRole(eq(MembershipReferenceType.GROUP), any(), any(), any()))
                .thenReturn(Collections.singleton(membershipGroup));

        Set<String> recipientIds = messageService.getRecipientsId(api, messageEntity);

        assertNotNull("not null", recipientIds);
        assertEquals("size=2", 2, recipientIds.size());
        assertTrue("user=user-id", recipientIds.contains("user-id"));
        assertTrue("user=user-group-id", recipientIds.contains("user-group-id"));
        verify(mockMembershipRepository, never()).findByRole(any(), any());
        verify(mockSubscriptionRepository, times(1)).search(any());
        verify(mockMembershipRepository, times(1)).findByReferencesAndRole(eq(MembershipReferenceType.GROUP), any(), any(), any());
        verify(mockMembershipRepository, times(1)).findByReferencesAndRole(eq(MembershipReferenceType.APPLICATION), any(), any(), any());
    }
}
