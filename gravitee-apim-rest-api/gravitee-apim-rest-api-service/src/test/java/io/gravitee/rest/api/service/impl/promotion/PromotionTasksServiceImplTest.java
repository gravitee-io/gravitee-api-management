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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fixtures.core.model.GraviteeDefinitionFixtures;
import io.gravitee.apim.core.api.model.import_definition.ApiDescriptor;
import io.gravitee.apim.core.json.JsonProcessingException;
import io.gravitee.apim.infra.json.jackson.GraviteeDefinitionJacksonJsonSerializer;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.v4.ApiType;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class PromotionTasksServiceImplTest {

    @Mock
    private PromotionService promotionService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private ApiSearchService apiSearchService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PromotionTasksService cut;

    @BeforeEach
    void setUp() {
        cut = new PromotionTasksServiceImpl(promotionService, permissionService, environmentService, apiSearchService, objectMapper);
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
    public void shouldGetPromotionTasks_withApiUpdate() {
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
    public void shouldGetPromotionTasks_withApiCreation() {
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
    public void shouldGetPromotionTasks_withApiCreationBecauseItHasBeenDeleted() {
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
    public void shouldGetPromotionTasks_withApiUpdate_andApiCreation() {
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

    @Test
    public void shouldGetPromotionTasks_withV4ApiDefinition() {
        PromotionEntity aPromotionEntity = getAV4PromotionEntity();
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
        // V4 API name is extracted from api.name in the JSON
        assertThat(taskData.get("apiName")).isEqualTo("V4 API Name");
        assertThat(taskData.get("sourceEnvironmentName")).isEqualTo("Source Env");
        assertThat(taskData.get("targetEnvironmentName")).isEqualTo("Target Env");
        assertThat(taskData.get("apiId")).isEqualTo("api id v4");
        assertThat(taskData.get("isApiUpdate")).isEqualTo(false);
    }

    // Built with the serializer promotions are stored with, so the test reads what production reads.
    @ParameterizedTest
    @EnumSource(ApiType.class)
    public void shouldExposeTheLabelOfTheTypeThePromotedDefinitionDeclares(ApiType type) throws JsonProcessingException {
        PromotionEntity promotion = getAV4PromotionEntity();
        promotion.setApiDefinition(
            new GraviteeDefinitionJacksonJsonSerializer().serialize(
                GraviteeDefinitionFixtures.BASE.get()
                    .api(ApiDescriptor.ApiDescriptorV4.builder().name("Promoted API").type(type).build())
                    .build()
            )
        );
        givenPromotionsAwaitingCreation(promotion, getAnEnvironmentEntity());

        Map<String, Object> taskData = taskDataOfTheOnlyTask();

        assertThat(taskData.get("apiType")).isEqualTo(type.getLabel());
        assertThat(taskData.get("apiName")).isEqualTo("Promoted API");
    }

    @Test
    public void shouldOmitTheApiTypeWhenTheDefinitionDoesNotDeclareOne() {
        givenPromotionsAwaitingCreation(getAPromotionEntity(), getAnEnvironmentEntity());

        assertThat(taskDataOfTheOnlyTask()).doesNotContainKey("apiType");
    }

    // A newer source installation may promote a type this one doesn't know yet; a label is not what is stored.
    @ParameterizedTest
    @ValueSource(strings = { "TYPE_FROM_A_NEWER_VERSION", "llm-proxy" })
    public void shouldOmitATypeThisInstallationDoesNotKnowAndStillListThePromotion(String storedType) {
        PromotionEntity promotion = getAV4PromotionEntity();
        promotion.setApiDefinition("{\"api\":{\"name\":\"Promoted API\",\"type\":\"" + storedType + "\"}}");
        givenPromotionsAwaitingCreation(promotion, getAnEnvironmentEntity());

        Map<String, Object> taskData = taskDataOfTheOnlyTask();

        assertThat(taskData).doesNotContainKey("apiType");
        assertThat(taskData.get("apiName")).isEqualTo("Promoted API");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   ", "not json" })
    public void shouldStillListAPromotionWhoseDefinitionCannotBeRead(String definition) {
        PromotionEntity promotion = getAV4PromotionEntity();
        promotion.setApiDefinition(definition);
        givenPromotionsAwaitingCreation(promotion, getAnEnvironmentEntity());

        Map<String, Object> taskData = taskDataOfTheOnlyTask();

        assertThat(taskData.get("apiName")).isEqualTo("Unknown API name");
        assertThat(taskData).doesNotContainKey("apiType");
    }

    @Test
    public void shouldExposeTheEnvironmentAPromotionTargets() {
        givenPromotionsAwaitingCreation(getAV4PromotionEntity(), getAnEnvironmentEntity());

        assertThat(taskDataOfTheOnlyTask().get("targetEnvironmentId")).isEqualTo("env#1");
    }

    @Test
    public void shouldOmitTheTargetEnvironmentWhenNoEnvironmentHasItsCockpitId() {
        PromotionEntity promotion = getAV4PromotionEntity();
        promotion.setTargetEnvCockpitId("unknown-cockpit-id");
        givenPromotionsAwaitingCreation(promotion, getAnEnvironmentEntity());

        assertThat(taskDataOfTheOnlyTask()).doesNotContainKey("targetEnvironmentId");
    }

    // APIM does not keep cockpit ids unique, and nothing says which of the environments the promotion lands in.
    @Test
    public void shouldOmitTheTargetEnvironmentWhenSeveralEnvironmentsShareItsCockpitId() {
        EnvironmentEntity sameCockpitId = getAnEnvironmentEntity();
        sameCockpitId.setId("env#2");
        givenPromotionsAwaitingCreation(getAV4PromotionEntity(), getAnEnvironmentEntity(), sameCockpitId);

        assertThat(taskDataOfTheOnlyTask()).doesNotContainKey("targetEnvironmentId");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> taskDataOfTheOnlyTask() {
        List<TaskEntity> tasks = cut.getPromotionTasks(GraviteeContext.getExecutionContext());
        assertThat(tasks).hasSize(1);
        return (Map<String, Object>) tasks.get(0).getData();
    }

    private void givenPromotionsAwaitingCreation(PromotionEntity promotion, EnvironmentEntity... environments) {
        when(environmentService.findByOrganization(any())).thenReturn(List.of(environments));
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.TO_BE_VALIDATED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(singletonList(promotion), 0, 0, 0));
        when(
            promotionService.search(
                argThat(query -> query != null && query.getStatuses().get(0) == PromotionEntityStatus.ACCEPTED),
                any(),
                any()
            )
        ).thenReturn(new Page<>(emptyList(), 0, 0, 0));
        for (EnvironmentEntity environment : environments) {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_API),
                    eq(environment.getId()),
                    eq(CREATE)
                )
            ).thenReturn(true);
        }
    }

    private PromotionEntity getAPromotionEntity() {
        final PromotionEntity promotion = new PromotionEntity();
        promotion.setApiDefinition(
            "{\"id\" : \"api#1\",\"name\" : \"API Name\",\"version\" : \"1\",\"proxy\" : {  \"context_path\" : \"/product\",  \"endpoint\" : \"http://toto.com\",  \"endpoints\" : [ {    \"target\" : \"http://toto.com\",    \"weight\" : 1,    \"name\" : \"endpointName\"  } ],  \"strip_context_path\" : false,  \"http\" : {    \"configuration\" : {      \"connectTimeout\" : 5000,      \"idleTimeout\" : 60000,      \"keepAliveTimeout\" : 30000,      \"keepAlive\" : true,      \"dumpRequest\" : false    }  }},\"paths\" : {  \"/\" : [ {    \"methods\" : [ ],    \"api-key\" : {}  } ]},\"tags\" : [ ]\n}"
        );
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

    private PromotionEntity getAV4PromotionEntity() {
        final PromotionEntity promotion = new PromotionEntity();
        // V4 export format: name is inside api object
        promotion.setApiDefinition(
            "{\"export\":{\"date\":\"2026-02-03T23:42:09.626Z\",\"apimVersion\":\"4.10.3\"},\"api\":{\"crossId\":\"cc648cc8-a4d3-4d09-a48c-c8a4d3bd09e7\",\"name\":\"V4 API Name\",\"apiVersion\":\"1.0\",\"type\":\"PROXY\",\"definitionVersion\":\"V4\"},\"metadata\":[],\"pages\":[],\"plans\":[]}"
        );
        promotion.setTargetEnvCockpitId("env#1-cockpit-id");
        promotion.setTargetEnvName("Target Env");
        promotion.setSourceEnvCockpitId("env#2-cockpit-id");
        promotion.setSourceEnvName("Source Env");
        promotion.setApiId("api id v4");
        promotion.setTargetApiId("target api id v4");

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
}
