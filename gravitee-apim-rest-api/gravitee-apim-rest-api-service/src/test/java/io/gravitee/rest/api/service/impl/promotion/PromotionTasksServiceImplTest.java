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
package io.gravitee.rest.api.service.impl.promotion;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.TaskEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.promotion.PromotionEntity;
import io.gravitee.rest.api.model.promotion.PromotionEntityAuthor;
import io.gravitee.rest.api.model.promotion.PromotionEntityStatus;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.promotion.PromotionService;
import io.gravitee.rest.api.service.promotion.PromotionTasksService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
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
    private ApiSearchService apiSearchService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private PromotionTasksService cut;

    @Before
    public void setUp() {
        cut = new PromotionTasksServiceImpl(promotionService, permissionService, environmentService, objectMapper, apiSearchService);
    }

    @Test
    public void shouldNotGetPromotionTasksWhenNothingFromDb() {
        when(promotionService.search(any(), any(), any())).thenReturn(new Page<>(emptyList(), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(CREATE)
            )
        ).thenReturn(true);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(UPDATE)
            )
        ).thenReturn(true);
        final List<TaskEntity> result = cut.getPromotionTasks(GraviteeContext.getExecutionContext());

        assertThat(result).isEmpty();
    }

    @Test
    public void shouldNotGetPromotionTasksIfDoesNotHavePermissions() {
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(CREATE)
            )
        ).thenReturn(false);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(UPDATE)
            )
        ).thenReturn(false);

        final List<TaskEntity> result = cut.getPromotionTasks(GraviteeContext.getExecutionContext());
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldNotGetPromotionTasksIfMalformed() throws JsonProcessingException {
        PromotionEntity aPromotionEntity = getAMalformedPromotionEntity();
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.TO_BE_VALIDATED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(aPromotionEntity), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));

        PromotionEntity previousPromotionEntity = getAMalformedPromotionEntity();
        previousPromotionEntity.setTargetApiId("api#target");
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.ACCEPTED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(previousPromotionEntity), 0, 0, 0));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(UPDATE)
            )
        ).thenReturn(true);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(CREATE)
            )
        ).thenReturn(false);

        final List<TaskEntity> result = cut.getPromotionTasks(GraviteeContext.getExecutionContext());
        assertThat(result).isEmpty();
    }

    @Test
    /*
     * Promotion of V4 APIs coming from more recent version of APIM should be ignored
     */
    public void shouldNotGetPromotionTasksIfTooRecent() throws JsonProcessingException {
        PromotionEntity aPromotionEntity = getAV4ApiPromotionEntity();
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.TO_BE_VALIDATED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(aPromotionEntity), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));

        PromotionEntity previousPromotionEntity = getAV4ApiPromotionEntity();
        previousPromotionEntity.setTargetApiId("api#target");
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.ACCEPTED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(previousPromotionEntity), 0, 0, 0));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(UPDATE)
            )
        ).thenReturn(true);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(CREATE)
            )
        ).thenReturn(false);

        when(apiSearchService.exists("api#target")).thenReturn(true);

        final List<TaskEntity> result = cut.getPromotionTasks(GraviteeContext.getExecutionContext());
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldGetPromotionTasks_withApiUpdate() throws JsonProcessingException {
        PromotionEntity aPromotionEntity = getAPromotionEntity();
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.TO_BE_VALIDATED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(aPromotionEntity), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));

        PromotionEntity previousPromotionEntity = getAPromotionEntity();
        previousPromotionEntity.setTargetApiId("api#target");
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.ACCEPTED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(previousPromotionEntity), 0, 0, 0));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(UPDATE)
            )
        ).thenReturn(true);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(CREATE)
            )
        ).thenReturn(false);

        when(apiSearchService.exists("api#target")).thenReturn(true);

        final List<TaskEntity> result = cut.getPromotionTasks(GraviteeContext.getExecutionContext());
        assertThat(result).hasSize(1);
        Map<String, Object> taskData = (Map<String, Object>) result.get(0).getData();
        assertThat(taskData.get("apiName")).isEqualTo("API Name");
        assertThat(taskData.get("sourceEnvironmentName")).isEqualTo("Source Env");
        assertThat(taskData.get("targetEnvironmentName")).isEqualTo("Target Env");
        assertThat(taskData.get("authorDisplayName")).isEqualTo("Author");
        assertThat(taskData.get("authorEmail")).isEqualTo("author@gv.io");
        assertThat(taskData.get("apiId")).isEqualTo("api id");
        assertThat(taskData.get("isApiUpdate")).isEqualTo(true);
        assertThat(taskData.get("targetApiId")).isEqualTo("api#target");
    }

    @Test
    public void shouldGetPromotionTasks_withApiCreation() throws JsonProcessingException {
        PromotionEntity aPromotionEntity = getAPromotionEntity();
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.TO_BE_VALIDATED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(aPromotionEntity), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));

        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.ACCEPTED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(emptyList(), 0, 0, 0));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(CREATE)
            )
        ).thenReturn(true);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(UPDATE)
            )
        ).thenReturn(false);

        final List<TaskEntity> result = cut.getPromotionTasks(GraviteeContext.getExecutionContext());
        assertThat(result).hasSize(1);
        Map<String, Object> taskData = (Map<String, Object>) result.get(0).getData();
        assertThat(taskData.get("apiName")).isEqualTo("API Name");
        assertThat(taskData.get("sourceEnvironmentName")).isEqualTo("Source Env");
        assertThat(taskData.get("targetEnvironmentName")).isEqualTo("Target Env");
        assertThat(taskData.get("authorDisplayName")).isEqualTo("Author");
        assertThat(taskData.get("authorEmail")).isEqualTo("author@gv.io");
        assertThat(taskData.get("apiId")).isEqualTo("api id");
        assertThat(taskData.get("isApiUpdate")).isEqualTo(false);
        assertThat(taskData.get("targetApiId")).isNull();
    }

    @Test
    public void shouldGetPromotionTasks_withApiCreationBecauseItHasBeenDeleted() throws JsonProcessingException {
        PromotionEntity aPromotionEntity = getAPromotionEntity();
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.TO_BE_VALIDATED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(aPromotionEntity), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));

        PromotionEntity previousPromotionEntity = getAPromotionEntity();
        previousPromotionEntity.setTargetApiId("api#target");
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.ACCEPTED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(previousPromotionEntity), 0, 0, 0));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(CREATE)
            )
        ).thenReturn(true);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(UPDATE)
            )
        ).thenReturn(false);

        when(apiSearchService.exists("api#target")).thenReturn(false);

        final List<TaskEntity> result = cut.getPromotionTasks(GraviteeContext.getExecutionContext());
        assertThat(result).hasSize(1);
        Map<String, Object> taskData = (Map<String, Object>) result.get(0).getData();
        assertThat(taskData.get("apiName")).isEqualTo("API Name");
        assertThat(taskData.get("sourceEnvironmentName")).isEqualTo("Source Env");
        assertThat(taskData.get("targetEnvironmentName")).isEqualTo("Target Env");
        assertThat(taskData.get("authorDisplayName")).isEqualTo("Author");
        assertThat(taskData.get("authorEmail")).isEqualTo("author@gv.io");
        assertThat(taskData.get("apiId")).isEqualTo("api id");
        assertThat(taskData.get("isApiUpdate")).isEqualTo(false);
    }

    @Test
    public void shouldGetPromotionTasks_withApiUpdate_andApiCreation() throws JsonProcessingException {
        PromotionEntity promotionEntity1 = getAPromotionEntity();
        PromotionEntity promotionEntity2 = getAPromotionEntity();
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.TO_BE_VALIDATED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(List.of(promotionEntity1, promotionEntity2), 0, 0, 0));
        when(environmentService.findByOrganization(any())).thenReturn(singletonList(getAnEnvironmentEntity()));

        PromotionEntity previousPromotionEntity = getAPromotionEntity();
        previousPromotionEntity.setTargetApiId("api#target");
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.ACCEPTED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(previousPromotionEntity), 0, 0, 0));
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(UPDATE)
            )
        ).thenReturn(true);
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_API),
                eq("env#1"),
                eq(CREATE)
            )
        ).thenReturn(false);

        when(apiSearchService.exists("api#target")).thenReturn(true);

        final List<TaskEntity> result = cut.getPromotionTasks(GraviteeContext.getExecutionContext());
        assertThat(result).hasSize(2);
    }

    private PromotionEntity getAPromotionEntity() {
        final PromotionEntity promotion = new PromotionEntity();
        promotion.setApiDefinition(
            """
                {
                    "id": "api#1",
                    "name": "API Name",
                    "version": "1",
                    "proxy": {
                        "virtual_hosts" : [ {
                          "path" : "/product"
                        } ],
                        "groups" : [ {
                          "name" : "default",
                          "endpoints" : [ {
                            "name" : "Default",
                            "target" : "https://api.gravitee.io/echo",
                            "tenants" : [ ],
                            "weight" : 1,
                            "backup" : false,
                            "type" : "http"
                          } ],
                          "load_balancing" : {
                            "type" : "ROUND_ROBIN"
                          },
                          "http" : {
                            "connectTimeout" : 5000,
                            "idleTimeout" : 60000,
                            "keepAliveTimeout" : 30000,
                            "keepAlive" : true,
                            "readTimeout" : 10000,
                            "pipelining" : false,
                            "maxConcurrentConnections" : 100,
                            "useCompression" : true,
                            "followRedirects" : false,
                            "maxHeaderSize" : 8192,
                            "maxChunkSize" : 8192
                          }
                        } ],
                        "strip_context_path": false
                    },
                    "tags": []
                }
            """
        );
        promotion.setId("promotion#1");
        promotion.setTargetEnvCockpitId("env#1-cockpit-id");
        promotion.setTargetEnvName("Target Env");
        promotion.setSourceEnvCockpitId("env#2-cockpit-id");
        promotion.setSourceEnvName("Source Env");
        promotion.setApiId("api id");
        promotion.setTargetApiId("target api id");

        PromotionEntityAuthor author = new PromotionEntityAuthor();
        author.setDisplayName("Author");
        author.setEmail("author@gv.io");
        promotion.setAuthor(author);
        return promotion;
    }

    private EnvironmentEntity getAnEnvironmentEntity() {
        final EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId("env#1");
        environmentEntity.setCockpitId("env#1-cockpit-id");
        environmentEntity.setName("Env 1");

        return environmentEntity;
    }

    private PromotionEntity getAMalformedPromotionEntity() {
        final PromotionEntity promotion = new PromotionEntity();
        promotion.setApiDefinition("malformed api definition");
        promotion.setId("promotion#1");
        promotion.setTargetEnvCockpitId("env#1-cockpit-id");
        promotion.setTargetEnvName("Target Env");
        promotion.setSourceEnvCockpitId("env#2-cockpit-id");
        promotion.setSourceEnvName("Source Env");
        promotion.setApiId("api id");
        promotion.setTargetApiId("target api id");

        PromotionEntityAuthor author = new PromotionEntityAuthor();
        author.setDisplayName("Author");
        author.setEmail("author@gv.io");
        promotion.setAuthor(author);
        return promotion;
    }

    private PromotionEntity getAV4ApiPromotionEntity() {
        final PromotionEntity promotion = new PromotionEntity();
        promotion.setApiDefinition(
            """
              "export": {
                "date": "2026-02-26T15:40:00.714152891Z",
                "apimVersion": "4.10.7-SNAPSHOT"
              },
              "api": {
                    "id": "api#1",
                    "name": "API Name",
                    "version": "1",
                    "proxy": {
                        "virtual_hosts" : [ {
                          "path" : "/product"
                        } ],
                        "groups" : [ {
                          "name" : "default",
                          "endpoints" : [ {
                            "name" : "Default",
                            "target" : "https://api.gravitee.io/echo",
                            "tenants" : [ ],
                            "weight" : 1,
                            "backup" : false,
                            "type" : "http"
                          } ],
                          "load_balancing" : {
                            "type" : "ROUND_ROBIN"
                          },
                          "http" : {
                            "connectTimeout" : 5000,
                            "idleTimeout" : 60000,
                            "keepAliveTimeout" : 30000,
                            "keepAlive" : true,
                            "readTimeout" : 10000,
                            "pipelining" : false,
                            "maxConcurrentConnections" : 100,
                            "useCompression" : true,
                            "followRedirects" : false,
                            "maxHeaderSize" : 8192,
                            "maxChunkSize" : 8192
                          }
                        } ],
                        "strip_context_path": false
                    },
                    "tags": []
                }
            """
        );
        promotion.setId("promotion#1");
        promotion.setTargetEnvCockpitId("env#1-cockpit-id");
        promotion.setTargetEnvName("Target Env");
        promotion.setSourceEnvCockpitId("env#2-cockpit-id");
        promotion.setSourceEnvName("Source Env");
        promotion.setApiId("api id");
        promotion.setTargetApiId("target api id");

        PromotionEntityAuthor author = new PromotionEntityAuthor();
        author.setDisplayName("Author");
        author.setEmail("author@gv.io");
        promotion.setAuthor(author);
        return promotion;
    }
}
