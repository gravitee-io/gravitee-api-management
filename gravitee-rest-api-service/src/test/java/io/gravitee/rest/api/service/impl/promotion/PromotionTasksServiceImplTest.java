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
package io.gravitee.rest.api.service.impl.promotion;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.TaskEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.UserRoleEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityAuthor;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.gravitee.rest.api.service.promotion.PromotionTasksService;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PromotionTasksServiceImplTest {

    @Mock
    private PromotionService promotionService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiService apiService;

    @Mock
    private ObjectMapper objectMapper;

    private PromotionTasksService cut;

    @Before
    public void setUp() {
        cut = new PromotionTasksServiceImpl(promotionService, permissionService, environmentService, apiService, objectMapper);
    }

    @Test
    public void shouldNotGetPromotionTasksWhenNothingFromDb() {
        when(promotionService.search(any(), any(), any())).thenReturn(new Page<>(emptyList(), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));
        when(permissionService.hasPermission(eq(RolePermission.ENVIRONMENT_API), any(String.class), eq(CREATE), eq(UPDATE)))
            .thenReturn(true);
        final List<TaskEntity> result = cut.getPromotionTasks("user");

        assertThat(result).isEmpty();
    }

    @Test
    public void shouldNotGetPromotionTasksIfDoesNotHavePermissions() {
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));
        when(promotionService.search(any(), any(), any())).thenReturn(new Page<>(emptyList(), 0, 0, 0));
        when(permissionService.hasPermission(eq(RolePermission.ENVIRONMENT_API), any(String.class), eq(CREATE), eq(UPDATE)))
            .thenReturn(false);

        final List<TaskEntity> result = cut.getPromotionTasks("user");
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldGetPromotionTasks() throws JsonProcessingException {
        PromotionEntity aPromotionEntity = getAPromotionEntity();
        when(promotionService.search(any(), any(), any())).thenReturn(new Page<>(singletonList(aPromotionEntity), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));
        when(permissionService.hasPermission(RolePermission.ENVIRONMENT_API, "env#1", CREATE, UPDATE)).thenReturn(true);
        when(objectMapper.readValue(aPromotionEntity.getApiDefinition(), ApiEntity.class)).thenReturn(getAnApiEntity());

        final List<TaskEntity> result = cut.getPromotionTasks("user");
        assertThat(result).hasSize(1);
        Map<String, Object> taskData = (Map<String, Object>) result.get(0).getData();
        assertThat(taskData.get("apiName")).isEqualTo("API Name");
        assertThat(taskData.get("sourceEnvironmentName")).isEqualTo("Source Env");
        assertThat(taskData.get("targetEnvironmentName")).isEqualTo("Target Env");
        assertThat(taskData.get("authorDisplayName")).isEqualTo("Author");
        assertThat(taskData.get("authorEmail")).isEqualTo("author@gv.io");
        assertThat(taskData.get("authorPicture")).isEqualTo("https://picture.png");
    }

    private PromotionEntity getAPromotionEntity() {
        final PromotionEntity promotion = new PromotionEntity();
        promotion.setApiDefinition(
            "{\"id\" : \"api#1\",\"name\" : \"API Name\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}"
        );
        promotion.setTargetEnvCockpitId("env#1-cockpit-id");
        promotion.setTargetEnvName("Target Env");
        promotion.setSourceEnvCockpitId("env#2-cockpit-id");
        promotion.setSourceEnvName("Source Env");

        PromotionEntityAuthor author = new PromotionEntityAuthor();
        author.setDisplayName("Author");
        author.setEmail("author@gv.io");
        author.setPicture("https://picture.png");
        promotion.setAuthor(author);
        return promotion;
    }

    private ApiEntity getAnApiEntity() {
        final ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("api#1");
        apiEntity.setName("API Name");

        return apiEntity;
    }

    private EnvironmentEntity getAnEnvironmentEntity() {
        final EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId("env#1");
        environmentEntity.setCockpitId("env#1-cockpit-id");
        environmentEntity.setName("Env 1");

        return environmentEntity;
    }
}
