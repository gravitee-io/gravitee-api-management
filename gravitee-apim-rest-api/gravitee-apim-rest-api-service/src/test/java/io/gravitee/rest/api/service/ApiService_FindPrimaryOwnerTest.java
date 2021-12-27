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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.NoPrimaryOwnerGroupForUserException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_FindPrimaryOwnerTest {

    private static final String CURRENT_USER = "myCurrentUser";
    private static final String CURRENT_USER_PO_GROUP = "myCurrentUserPoGroup";
    private static final String PO_GROUP_ID = "myPoGroup";
    private static final String PO_USER_ID = "myPoUser";

    @InjectMocks
    private final ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ParameterService parameterService;

    @Mock
    private GroupService groupService;

    @Mock
    private UserService userService;

    @Spy
    private final ObjectMapper objectMapper = (new ServiceConfiguration()).objectMapper();

    // HYBRID + import with PO GROUP
    @Test
    public void testHybridModeWithExistingPOGroup() {
        setPrimaryOwnerMode("HYBRID");
        defineGroup(PO_GROUP_ID);

        JsonNode definition = poGroupDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(PO_GROUP_ID, primaryOwner.getId());
        assertEquals("GROUP", primaryOwner.getType());
    }

    @Test
    public void testHybridModeWithNonExistingPOGroupAndCurrentUserBelongsToAPoGroup() {
        setPrimaryOwnerMode("HYBRID");
        setPoGroupNonExisting();
        addUserInPOGroup(CURRENT_USER, CURRENT_USER_PO_GROUP);

        JsonNode definition = poGroupDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER_PO_GROUP, primaryOwner.getId());
        assertEquals("GROUP", primaryOwner.getType());
    }

    @Test
    public void testHybridModeWithNonExistingPOGroupAndCurrentUserDoesNotBelongToAPoGroup() {
        setPrimaryOwnerMode("HYBRID");
        setPoGroupNonExisting();
        setCurrentUserInNoPOGroup();
        defineUser(CURRENT_USER);

        JsonNode definition = poGroupDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER, primaryOwner.getId());
        assertEquals("USER", primaryOwner.getType());
    }

    // HYBRID + import with PO User
    @Test
    public void testHybridModeWithExistingPOUser() {
        setPrimaryOwnerMode("HYBRID");
        defineUser(PO_USER_ID);

        JsonNode definition = poUserDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(PO_USER_ID, primaryOwner.getId());
        assertEquals("USER", primaryOwner.getType());
    }

    @Test
    public void testHybridModeWithNonExistingPOUser() {
        setPrimaryOwnerMode("HYBRID");
        setPoUserNonExisting();
        defineUser(CURRENT_USER);

        JsonNode definition = poUserDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER, primaryOwner.getId());
        assertEquals("USER", primaryOwner.getType());
    }

    // HYBRID + import with no PO
    @Test
    public void testHybridModeWithNoPO() {
        setPrimaryOwnerMode("HYBRID");
        defineUser(CURRENT_USER);

        JsonNode definition = noPODefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER, primaryOwner.getId());
        assertEquals("USER", primaryOwner.getType());
    }

    // GROUP + import with PO GROUP
    @Test
    public void testGroupModeWithExistingPOGroup() {
        setPrimaryOwnerMode("GROUP");
        defineGroup(PO_GROUP_ID);

        JsonNode definition = poGroupDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(PO_GROUP_ID, primaryOwner.getId());
        assertEquals("GROUP", primaryOwner.getType());
    }

    @Test
    public void testGroupModeWithNonExistingPOGroupAndCurrentUserBelongsToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        setPoGroupNonExisting();
        addUserInPOGroup(CURRENT_USER, CURRENT_USER_PO_GROUP);

        JsonNode definition = poGroupDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER_PO_GROUP, primaryOwner.getId());
        assertEquals("GROUP", primaryOwner.getType());
    }

    @Test(expected = NoPrimaryOwnerGroupForUserException.class)
    public void testGroupModeWithNonExistingPOGroupAndCurrentUserDoesNotBelongToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        setPoGroupNonExisting();
        setCurrentUserInNoPOGroup();

        JsonNode definition = poGroupDefinition();

        apiService.findPrimaryOwner(definition, CURRENT_USER);
    }

    // GROUP + import with PO User
    @Test
    public void testGroupModeWithExistingPOUserAndPoUserBelongsToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        defineUser(PO_USER_ID);
        addUserInPOGroup(PO_USER_ID, PO_GROUP_ID);

        JsonNode definition = poUserDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(PO_GROUP_ID, primaryOwner.getId());
        assertEquals("GROUP", primaryOwner.getType());
    }

    @Test
    public void testGroupModeWithExistingPOUserAndPoUserDoesNotBelongToAPoGroupAndCurrentUserBelongsToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        defineUser(PO_USER_ID);
        setPoUserInNoPOGroup();
        addUserInPOGroup(CURRENT_USER, CURRENT_USER_PO_GROUP);

        JsonNode definition = poUserDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER_PO_GROUP, primaryOwner.getId());
        assertEquals("GROUP", primaryOwner.getType());
    }

    @Test(expected = NoPrimaryOwnerGroupForUserException.class)
    public void testGroupModeWithExistingPOUserAndPoUserDoesNotBelongToAPoGroupAndCurrentUserDoesNotBelongToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        defineUser(PO_USER_ID);
        setCurrentUserInNoPOGroup();
        setPoUserInNoPOGroup();

        JsonNode definition = poUserDefinition();

        apiService.findPrimaryOwner(definition, CURRENT_USER);
    }

    @Test
    public void testGroupModeWithNonExistingPOUserAndCurrentUserBelongsToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        addUserInPOGroup(CURRENT_USER, CURRENT_USER_PO_GROUP);
        setPoUserNonExisting();

        JsonNode definition = poUserDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER_PO_GROUP, primaryOwner.getId());
        assertEquals("GROUP", primaryOwner.getType());
    }

    @Test(expected = NoPrimaryOwnerGroupForUserException.class)
    public void testGroupModeWithNonExistingPOUserAndCurrentUserDoesNotBelongToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        setPoUserNonExisting();
        setCurrentUserInNoPOGroup();

        JsonNode definition = poUserDefinition();

        apiService.findPrimaryOwner(definition, CURRENT_USER);
    }

    // GROUP + import with no PO
    @Test
    public void testGroupModeWithNoPOAndCurrentUserBelongsToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        addUserInPOGroup(CURRENT_USER, CURRENT_USER_PO_GROUP);

        JsonNode definition = noPODefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER_PO_GROUP, primaryOwner.getId());
        assertEquals("GROUP", primaryOwner.getType());
    }

    @Test(expected = NoPrimaryOwnerGroupForUserException.class)
    public void testGroupModeWithNoPOAndCurrentUserDoesNotBelongToAPoGroup() {
        setPrimaryOwnerMode("GROUP");
        setCurrentUserInNoPOGroup();

        JsonNode definition = noPODefinition();

        apiService.findPrimaryOwner(definition, CURRENT_USER);
    }

    // USER + import with PO GROUP
    @Test
    public void testUserModeWithPOGroup() {
        setPrimaryOwnerMode("USER");
        defineUser(CURRENT_USER);

        JsonNode definition = poGroupDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER, primaryOwner.getId());
        assertEquals("USER", primaryOwner.getType());
    }

    // USER + import with PO User
    @Test
    public void testUserModeWithExistingPOUser() {
        setPrimaryOwnerMode("USER");
        defineUser(PO_USER_ID);

        JsonNode definition = poUserDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(PO_USER_ID, primaryOwner.getId());
        assertEquals("USER", primaryOwner.getType());
    }

    @Test
    public void testUserModeWithNonExistingPOUser() {
        setPrimaryOwnerMode("USER");
        setPoUserNonExisting();
        defineUser(CURRENT_USER);

        JsonNode definition = poUserDefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER, primaryOwner.getId());
        assertEquals("USER", primaryOwner.getType());
    }

    // USER + import with no PO
    @Test
    public void testUserModeWithNoPO() {
        setPrimaryOwnerMode("USER");
        defineUser(CURRENT_USER);

        JsonNode definition = noPODefinition();

        final PrimaryOwnerEntity primaryOwner = apiService.findPrimaryOwner(definition, CURRENT_USER);
        assertEquals(CURRENT_USER, primaryOwner.getId());
        assertEquals("USER", primaryOwner.getType());
    }

    // Helpers
    private JsonNode noPODefinition() {
        return JsonNodeFactory.instance.objectNode();
    }

    private JsonNode poGroupDefinition() {
        return JsonNodeFactory.instance
            .objectNode()
            .set("primaryOwner", JsonNodeFactory.instance.objectNode().put("id", PO_GROUP_ID).put("type", "GROUP"));
    }

    private JsonNode poUserDefinition() {
        return JsonNodeFactory.instance
            .objectNode()
            .set("primaryOwner", JsonNodeFactory.instance.objectNode().put("id", PO_USER_ID).put("type", "USER"));
    }

    private void setPrimaryOwnerMode(String mode) {
        when(parameterService.find(Key.API_PRIMARY_OWNER_MODE, ParameterReferenceType.ENVIRONMENT)).thenReturn(mode);
    }

    private void addUserInPOGroup(String username, String poGroup) {
        GroupEntity userPoGroup = new GroupEntity();
        userPoGroup.setId(poGroup);
        userPoGroup.setApiPrimaryOwner("PO-of-current_user-po-group");
        GroupEntity aNonPoGroup = new GroupEntity();
        aNonPoGroup.setId("aNonPoGroup");
        aNonPoGroup.setApiPrimaryOwner(null);
        when(groupService.findByUser(username)).thenReturn(new HashSet<>(Arrays.asList(userPoGroup, aNonPoGroup)));
    }

    private void setCurrentUserInNoPOGroup() {
        GroupEntity aNonPoGroup = new GroupEntity();
        aNonPoGroup.setId("aNonPoGroup");
        aNonPoGroup.setApiPrimaryOwner(null);
        GroupEntity anotherNonPoGroup = new GroupEntity();
        anotherNonPoGroup.setId("anotherNonPoGroup");
        anotherNonPoGroup.setApiPrimaryOwner(null);
        when(groupService.findByUser(CURRENT_USER)).thenReturn(new HashSet<>(Arrays.asList(aNonPoGroup, anotherNonPoGroup)));
    }

    private void setPoUserInNoPOGroup() {
        GroupEntity aNonPoGroup = new GroupEntity();
        aNonPoGroup.setId("aNonPoGroup");
        aNonPoGroup.setApiPrimaryOwner(null);
        GroupEntity anotherNonPoGroup = new GroupEntity();
        anotherNonPoGroup.setId("anotherNonPoGroup");
        anotherNonPoGroup.setApiPrimaryOwner(null);
        when(groupService.findByUser(PO_USER_ID)).thenReturn(new HashSet<>(Arrays.asList(aNonPoGroup, anotherNonPoGroup)));
    }

    private void defineUser(String username) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(username);
        when(userService.findById(username)).thenReturn(userEntity);
    }

    private void defineGroup(String groupId) {
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(groupId);
        when(groupService.findById(GraviteeContext.getCurrentEnvironment(), groupId)).thenReturn(groupEntity);
    }

    private void setPoUserNonExisting() {
        when(userService.findById(PO_USER_ID)).thenThrow(new UserNotFoundException(PO_USER_ID));
    }

    private void setPoGroupNonExisting() {
        when(groupService.findById(GraviteeContext.getCurrentEnvironment(), PO_GROUP_ID))
            .thenThrow(new GroupNotFoundException(PO_GROUP_ID));
    }
}
