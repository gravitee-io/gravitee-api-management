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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.Rule;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.PropertyEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiEntrypointEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceImplTest {

    private final ExecutionContext executionContext = new ExecutionContext("DEFAULT", "DEFAULT");

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SynchronizationService synchronizationService;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PlanService planService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiEntrypointService apiEntrypointService;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private ApiServiceImpl apiService;

    @Mock
    private DataEncryptor dataEncryptor;

    @Test
    public void encryptProperties_should_call_data_encryptor_for_each_encryptable_property_not_yet_encrypted()
        throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();

        apiService.encryptProperties(properties);

        verify(dataEncryptor, times(1)).encrypt("value2");
        verify(dataEncryptor, times(1)).encrypt("value4");
        verifyNoMoreInteractions(dataEncryptor);
    }

    @Test
    public void encryptProperties_should_set_encrypted_boolean_true_for_each_encrypted_property() throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();

        apiService.encryptProperties(properties);

        assertFalse(properties.get(0).isEncrypted());
        assertTrue(properties.get(1).isEncrypted());
        assertTrue(properties.get(2).isEncrypted());
        assertTrue(properties.get(3).isEncrypted());
    }

    @Test
    public void encryptProperties_should_set_value_of_each_encrypted_property() throws GeneralSecurityException {
        List<PropertyEntity> properties = buildProperties();
        when(dataEncryptor.encrypt("value2")).thenReturn("encryptedValue2");
        when(dataEncryptor.encrypt("value4")).thenReturn("encryptedValue4");

        apiService.encryptProperties(properties);

        assertEquals("value1", properties.get(0).getValue());
        assertEquals("encryptedValue2", properties.get(1).getValue());
        assertEquals("value3", properties.get(2).getValue());
        assertEquals("encryptedValue4", properties.get(3).getValue());
    }

    @Test
    public void isSynchronized_should_return_true_when_last_event_is_start_and_api_is_synced() throws Exception {
        setupIsSynchronizedTest(EventType.START_API);
    }

    @Test
    public void isSynchronized_should_return_true_when_last_event_is_stop_and_api_is_synced() throws Exception {
        setupIsSynchronizedTest(EventType.STOP_API);
    }

    private void setupIsSynchronizedTest(EventType eventType) throws Exception {
        ApiEntity currentApi = mock(ApiEntity.class);
        PrimaryOwnerEntity primaryOwnerEntity = mock(PrimaryOwnerEntity.class);
        Event event = new Event();
        event.setType(eventType);

        Api api = new Api();
        api.setId("api-id");
        api.setName("My API");
        api.setEnvironmentId("DEFAULT");
        event.setPayload(objectMapper.writeValueAsString(api));

        List<ApiEntrypointEntity> entrypointEntityList = new ArrayList<>();
        Map<String, List<Rule>> paths = new HashMap<>();

        when(apiRepository.findById("api-id")).thenReturn(Optional.of(api));
        when(currentApi.getDefinitionContext()).thenReturn(new DefinitionContext());
        when(eventLatestRepository.search(any(), any(), anyLong(), anyLong())).thenReturn(List.of(event));
        when(apiEntrypointService.getApiEntrypoints(executionContext, currentApi)).thenReturn(entrypointEntityList);
        when(apiConverter.toApiEntity(executionContext, api, primaryOwnerEntity, true)).thenReturn(currentApi);
        when(apiConverter.toApiEntity(executionContext, null, null, false)).thenReturn(currentApi);
        when(primaryOwnerService.getPrimaryOwner(executionContext.getOrganizationId(), api.getId())).thenReturn(primaryOwnerEntity);
        when(synchronizationService.checkSynchronization(any(), any(), any())).thenReturn(true);
        when(currentApi.getId()).thenReturn("api-id");
        when(planService.findByApi(executionContext, "api-id")).thenReturn(Set.of());
        when(currentApi.getPaths()).thenReturn(paths);

        boolean result = apiService.isSynchronized(executionContext, "api-id");

        assertTrue(result);
    }

    @Test
    public void createWithApiDefinition_should_add_default_groups_when_primary_owner_is_null() throws Exception {
        // Given
        PrimaryOwnerEntity primaryOwner = null;

        // Create default groups
        GroupEntity group1 = createGroupEntity("group1", "Group 1", null);
        GroupEntity group2 = createGroupEntity("group2", "Group 2", null);
        GroupEntity groupWithApiPrimaryOwner = createGroupEntity("group3", "Group 3", "some-user-id");

        Set<GroupEntity> defaultGroupEntities = new HashSet<>(Arrays.asList(group1, group2, groupWithApiPrimaryOwner));

        when(groupService.findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE))).thenReturn(defaultGroupEntities);

        // When
        Set<String> result = getDefaultGroupsForApiCreation(primaryOwner);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("group1"));
        assertTrue(result.contains("group2"));
        assertTrue(result.contains("group3"));

        verify(groupService, times(1)).findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE));
    }

    @Test
    public void createWithApiDefinition_should_filter_out_groups_with_api_primary_owner_when_primary_owner_exists() throws Exception {
        // Given
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity();
        primaryOwner.setId("primary-owner-id");
        primaryOwner.setType("USER");

        // Create default groups
        GroupEntity group1 = createGroupEntity("group1", "Group 1", null);
        GroupEntity group2 = createGroupEntity("group2", "Group 2", null);
        GroupEntity groupWithApiPrimaryOwner = createGroupEntity("group3", "Group 3", "some-user-id");

        Set<GroupEntity> defaultGroupEntities = new HashSet<>(Arrays.asList(group1, group2, groupWithApiPrimaryOwner));

        when(groupService.findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE))).thenReturn(defaultGroupEntities);

        // When
        Set<String> result = getDefaultGroupsForApiCreation(primaryOwner);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("group1"));
        assertTrue(result.contains("group2"));
        assertFalse(result.contains("group3")); // Should be filtered out because it has apiPrimaryOwner

        verify(groupService, times(1)).findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE));
    }

    @Test
    public void createWithApiDefinition_should_include_all_groups_when_primary_owner_has_empty_id() throws Exception {
        // Given
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity();
        primaryOwner.setId(""); // Empty ID
        primaryOwner.setType("USER");

        // Create default groups
        GroupEntity group1 = createGroupEntity("group1", "Group 1", null);
        GroupEntity group2 = createGroupEntity("group2", "Group 2", null);
        GroupEntity groupWithApiPrimaryOwner = createGroupEntity("group3", "Group 3", "some-user-id");

        Set<GroupEntity> defaultGroupEntities = new HashSet<>(Arrays.asList(group1, group2, groupWithApiPrimaryOwner));

        when(groupService.findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE))).thenReturn(defaultGroupEntities);

        // When
        Set<String> result = getDefaultGroupsForApiCreation(primaryOwner);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("group1"));
        assertTrue(result.contains("group2"));
        assertTrue(result.contains("group3")); // Should be included because empty ID is treated as no primary owner

        verify(groupService, times(1)).findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE));
    }

    @Test
    public void createWithApiDefinition_should_return_empty_set_when_no_default_groups_exist() throws Exception {
        // Given
        PrimaryOwnerEntity primaryOwner = new PrimaryOwnerEntity();
        primaryOwner.setId("primary-owner-id");
        primaryOwner.setType("USER");

        Set<GroupEntity> defaultGroupEntities = new HashSet<>(); // Empty set

        when(groupService.findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE))).thenReturn(defaultGroupEntities);

        // When
        Set<String> result = getDefaultGroupsForApiCreation(primaryOwner);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(groupService, times(1)).findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE));
    }

    @Test
    public void createWithApiDefinition_should_include_all_groups_when_primary_owner_is_null_and_groups_have_api_primary_owner()
        throws Exception {
        // Given
        PrimaryOwnerEntity primaryOwner = null;

        // Create default groups - all have apiPrimaryOwner
        GroupEntity group1 = createGroupEntity("group1", "Group 1", "user1");
        GroupEntity group2 = createGroupEntity("group2", "Group 2", "user2");

        Set<GroupEntity> defaultGroupEntities = new HashSet<>(Arrays.asList(group1, group2));

        when(groupService.findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE))).thenReturn(defaultGroupEntities);

        // When
        Set<String> result = getDefaultGroupsForApiCreation(primaryOwner);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("group1"));
        assertTrue(result.contains("group2"));

        verify(groupService, times(1)).findByEvent(eq(executionContext.getEnvironmentId()), eq(GroupEvent.API_CREATE));
    }

    /**
     * Helper method to simulate the default groups logic from createWithApiDefinition method
     */
    private Set<String> getDefaultGroupsForApiCreation(PrimaryOwnerEntity primaryOwner) {
        Set<GroupEntity> defaultGroupEntities = groupService.findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE);

        Set<String> defaultGroups;
        // Filter out groups with apiPrimaryOwner if primaryOwner is not null and has a value
        if (primaryOwner != null && !org.apache.commons.lang3.StringUtils.isEmpty(primaryOwner.getId())) {
            defaultGroups = defaultGroupEntities
                .stream()
                .filter(group -> org.apache.commons.lang3.StringUtils.isEmpty(group.getApiPrimaryOwner()))
                .map(GroupEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        } else {
            defaultGroups = defaultGroupEntities.stream().map(GroupEntity::getId).collect(java.util.stream.Collectors.toSet());
        }

        return defaultGroups;
    }

    /**
     * Helper method to create GroupEntity for testing
     */
    private GroupEntity createGroupEntity(String id, String name, String apiPrimaryOwner) {
        GroupEntity group = new GroupEntity();
        group.setId(id);
        group.setName(name);
        group.setApiPrimaryOwner(apiPrimaryOwner);
        return group;
    }

    private List<PropertyEntity> buildProperties() {
        return List.of(
            new PropertyEntity("key1", "value1", false, false),
            new PropertyEntity("key2", "value2", true, false),
            new PropertyEntity("key3", "value3", true, true),
            new PropertyEntity("key4", "value4", true, false)
        );
    }
}
