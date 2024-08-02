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
package io.gravitee.repository.management;

import static io.gravitee.repository.management.model.ApiLifecycleState.CREATED;
import static io.gravitee.repository.management.model.ApiLifecycleState.PUBLISHED;
import static io.gravitee.repository.management.model.LifecycleState.STOPPED;
import static io.gravitee.repository.management.model.Visibility.PRIVATE;
import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static io.gravitee.repository.utils.DateUtils.compareDate;
import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import java.util.*;
import org.junit.Test;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRepositoryTest extends AbstractManagementRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/api-tests/";
    }

    @Test
    public void createAndDeleteApiTest() throws Exception {
        String apiId = "newApi-Id";

        Api api = new Api();
        api.setId(apiId);
        api.setEnvironmentId("DEFAULT");
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        api.setMode(Api.MODE_API_DEFINITION_ONLY);
        api.setSyncFrom(Api.ORIGIN_MANAGEMENT.toUpperCase());
        api.setName("sample-new name");
        api.setVersion("1");
        api.setLifecycleState(STOPPED);
        api.setVisibility(Visibility.PRIVATE);
        api.setDefinition("{}");
        api.setCreatedAt(parse("11/02/2016"));
        api.setUpdatedAt(parse("12/02/2016"));
        api.setDisableMembershipNotifications(true);

        apiRepository.create(api);

        Optional<Api> optional = apiRepository.findById(apiId);
        assertTrue("Api saved not found", optional.isPresent());

        Api apiSaved = optional.get();
        assertEquals("Invalid saved environment id.", api.getEnvironmentId(), apiSaved.getEnvironmentId());
        assertEquals("Invalid saved api origin.", api.getOrigin(), apiSaved.getOrigin());
        assertEquals("Invalid saved api mode.", api.getMode(), apiSaved.getMode());
        assertEquals("Invalid saved api syncFrom.", api.getSyncFrom(), apiSaved.getSyncFrom());
        assertEquals("Invalid saved api version.", api.getVersion(), apiSaved.getVersion());
        assertEquals("Invalid deployment lifecycle.", api.getLifecycleState(), apiSaved.getLifecycleState());
        assertEquals("Invalid api private api status.", api.getVisibility(), apiSaved.getVisibility());
        assertEquals("Invalid api definition.", api.getDefinition(), apiSaved.getDefinition());
        assertTrue("Invalid api createdAt.", compareDate(api.getCreatedAt(), apiSaved.getCreatedAt()));
        assertTrue("Invalid api updateAt.", compareDate(api.getUpdatedAt(), apiSaved.getUpdatedAt()));
        assertEquals("Invalid api lifecycle.", api.getApiLifecycleState(), apiSaved.getApiLifecycleState());
        assertTrue("Invalid api disable membership notifications", apiSaved.isDisableMembershipNotifications());

        // test delete
        int nbApplicationBefore = apiRepository.search(null, ApiFieldFilter.allFields()).size();
        apiRepository.delete(apiId);
        int nbApplicationAfter = apiRepository.search(null, ApiFieldFilter.allFields()).size();
        assertFalse("api was deleted", apiRepository.findById(apiId).isPresent());
        assertEquals("Invalid number of apis after deletion", nbApplicationBefore - 1, nbApplicationAfter);
    }

    @Test
    public void createAndDeleteFederatedApiTest() throws Exception {
        String apiId = "newApi-Id";

        var api = Api
            .builder()
            .id(apiId)
            .environmentId("DEFAULT")
            .origin("integration")
            .integrationId("integration-id")
            .name("A Federated API")
            .description("federated-api-description")
            .definition("{}")
            .version("1.0")
            .visibility(PRIVATE)
            .createdAt(parse("11/02/2024"))
            .updatedAt(parse("11/02/2024"))
            .lifecycleState(null)
            .mode(null)
            .labels(List.of())
            .categories(Set.of())
            .groups(Set.of())
            .apiLifecycleState(CREATED)
            .syncFrom("MANAGEMENT")
            .build();
        apiRepository.create(api);

        assertThat(apiRepository.findById(apiId))
            .isPresent()
            .get()
            .satisfies(result -> assertThat(result).usingRecursiveComparison().isEqualTo(api));

        // test delete
        apiRepository.delete(apiId);
        assertThat(apiRepository.findById(apiId)).isEmpty();
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Api> optional = apiRepository.findById("api-to-update");
        assertTrue("API to update not found", optional.isPresent());
        assertEquals("Invalid saved api name.", "api-to-update name", optional.get().getName());

        final Api api = optional.get();
        api.setName("New API name");
        api.setEnvironmentId("new_DEFAULT");
        api.setDescription("New description");
        api.setCategories(new HashSet<>(asList("category1", "category2")));
        api.setDefinition("New definition");
        api.setDeployedAt(parse("11/02/2016"));
        api.setGroups(Collections.singleton("New group"));
        api.setLifecycleState(LifecycleState.STARTED);
        api.setPicture("New picture");
        api.setBackground("New background");
        api.setCreatedAt(parse("11/02/2016"));
        api.setUpdatedAt(parse("13/11/2016"));
        api.setVersion("New version");
        api.setVisibility(Visibility.PRIVATE);
        api.setApiLifecycleState(ApiLifecycleState.UNPUBLISHED);
        api.setDisableMembershipNotifications(false);

        int nbAPIsBeforeUpdate = apiRepository.search(null, ApiFieldFilter.allFields()).size();
        apiRepository.update(api);
        int nbAPIsAfterUpdate = apiRepository.search(null, ApiFieldFilter.allFields()).size();

        assertEquals(nbAPIsBeforeUpdate, nbAPIsAfterUpdate);

        Optional<Api> optionalUpdated = apiRepository.findById("api-to-update");
        assertTrue("API to update not found", optionalUpdated.isPresent());

        final Api apiUpdated = optionalUpdated.get();
        assertEquals("Invalid saved API name.", "New API name", apiUpdated.getName());
        assertEquals("Invalid saved environment id.", "new_DEFAULT", apiUpdated.getEnvironmentId());
        assertEquals("Invalid API description.", "New description", apiUpdated.getDescription());
        assertEquals("Invalid API categories.", new HashSet<>(asList("category1", "category2")), apiUpdated.getCategories());
        assertEquals("Invalid API definition.", "New definition", apiUpdated.getDefinition());
        assertTrue("Invalid API deployment date.", compareDate("11/02/2016", apiUpdated.getDeployedAt()));
        assertEquals("Invalid API group.", Collections.singleton("New group"), apiUpdated.getGroups());
        assertEquals("Invalid deployment lifecycle state.", LifecycleState.STARTED, apiUpdated.getLifecycleState());
        assertEquals("Invalid API picture.", "New picture", apiUpdated.getPicture());
        assertEquals("Invalid API background.", "New background", apiUpdated.getBackground());
        assertTrue("Invalid API create date.", compareDate("11/02/2016", apiUpdated.getCreatedAt()));
        assertTrue("Invalid API update date.", compareDate("13/11/2016", apiUpdated.getUpdatedAt()));
        assertEquals("Invalid API version.", "New version", apiUpdated.getVersion());
        assertEquals("Invalid API visibility.", Visibility.PRIVATE, apiUpdated.getVisibility());
        assertEquals("Invalid API lifecycle state.", ApiLifecycleState.UNPUBLISHED, apiUpdated.getApiLifecycleState());
        assertFalse("Invalid API disable membership notifications", apiUpdated.isDisableMembershipNotifications());
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Api> optional = apiRepository.findById("api-to-findById");
        assertTrue("Find api by name return no result ", optional.isPresent());

        Api api = optional.get();
        assertEquals("Invalid environment id.", "DEFAULT", api.getEnvironmentId());
        assertEquals("Invalid origin.", Api.ORIGIN_KUBERNETES, api.getOrigin());
        assertEquals("Invalid mode.", Api.MODE_API_DEFINITION_ONLY, api.getMode());
        assertEquals("Invalid api name", "api-to-findById name", api.getName());
        assertEquals("Invalid api version", "1", api.getVersion());
        assertEquals("Invalid api visibility", PUBLIC, api.getVisibility());
        assertEquals("Invalid deployment lifecycle state", STOPPED, api.getLifecycleState());
        assertEquals("Invalid api labels", 2, api.getLabels().size());
        assertEquals("Invalid api label at position 0", "label 1", api.getLabels().iterator().next());
        assertEquals("Invalid api lifecycle state", ApiLifecycleState.DEPRECATED, api.getApiLifecycleState());
        assertTrue("Invalid api disable membership notifications", api.isDisableMembershipNotifications());
    }

    @Test
    public void findByIdMissingTest() throws Exception {
        Optional<Api> optional = apiRepository.findById("findByNameMissing");
        assertFalse("Find api by name on missing api return a result", optional.isPresent());
    }

    @Test
    public void findAllTest() {
        List<Api> apis = apiRepository.search(null, ApiFieldFilter.allFields());

        assertNotNull(apis);
        assertFalse("Api list is empty", apis.isEmpty());
        assertNotNull("Api is null", apis.iterator().next());
    }

    @Test
    public void findAllTestCriteriaEmpty() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().build(), ApiFieldFilter.allFields());

        assertNotNull(apis);
        assertFalse("Api list is empty", apis.isEmpty());
        assertNotNull("Api is null", apis.iterator().next());
    }

    @Test
    public void deleteApiTest() throws Exception {
        Optional<Api> api = apiRepository.findById("api-to-delete");
        assertTrue("api exists", api.isPresent());
        apiRepository.delete("api-to-delete");
        api = apiRepository.findById("api-to-delete");
        assertFalse("api was deleted", api.isPresent());
    }

    @Test
    public void shouldFindApiWithGroup() throws Exception {
        Optional<Api> api = apiRepository.findById("grouped-api");
        assertTrue(api.isPresent());
        assertNotNull(api.get().getGroups());
        assertEquals(Collections.singleton("api-group"), api.get().getGroups());
    }

    @Test
    public void shouldFindByIds() {
        List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().ids("api-to-delete", "api-to-update", "unknown").build(),
            ApiFieldFilter.allFields()
        );
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(2, apis.size());
        assertTrue(apis.stream().map(Api::getId).toList().containsAll(asList("api-to-delete", "api-to-update")));
    }

    @Test
    public void shouldFindByEnvironmentsDevs() {
        List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().environments(Arrays.asList("DEV", "DEVS")).build(),
            ApiFieldFilter.allFields()
        );
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(3, apis.size());
        assertTrue(apis.stream().map(Api::getId).toList().containsAll(asList("api-to-delete", "api-to-update", "big-name")));
    }

    @Test
    public void shouldFindByDefinitionVersion() {
        List<Api> v2Apis = apiRepository.search(
            new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V2)).build(),
            ApiFieldFilter.allFields()
        );
        assertThat(v2Apis).hasSize(10);

        List<Api> v4Apis = apiRepository.search(
            new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.V4)).build(),
            ApiFieldFilter.allFields()
        );
        assertThat(v4Apis).hasSize(1);

        List<Api> federatedApis = apiRepository.search(
            new ApiCriteria.Builder().definitionVersion(List.of(DefinitionVersion.FEDERATED)).build(),
            ApiFieldFilter.allFields()
        );
        assertThat(federatedApis).hasSize(1);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownApi() throws TechnicalException {
        Api unknownApi = new Api();
        unknownApi.setId("unknown");
        apiRepository.update(unknownApi);
        fail("An unknown api should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws TechnicalException {
        apiRepository.update(null);
        fail("A null api should not be updated");
    }

    @Test
    public void shouldFindByGroups() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().groups("api-group", "unknown").build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("grouped-api", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByName() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().name("api-to-findById name").build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByLabel() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().label("label 1").build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByState() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().state(STOPPED).build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(4, apis.size());
        assertTrue(
            apis.stream().map(Api::getId).toList().containsAll(asList("api-to-delete", "api-to-update", "api-to-findById", "grouped-api"))
        );
    }

    @Test
    public void shouldFindByEnvironment() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(2, apis.size());
        assertTrue(apis.stream().map(Api::getId).toList().containsAll(asList("grouped-api", "api-to-findById")));
    }

    @Test
    public void shouldFindByVersion() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().version("1").build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(4, apis.size());
        assertTrue(
            apis.stream().map(Api::getId).toList().containsAll(asList("api-to-delete", "api-to-update", "api-to-findById", "grouped-api"))
        );
    }

    @Test
    public void shouldFindByView() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().category("my-category").build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByVisibility() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().visibility(PUBLIC).build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(2, apis.size());
        assertTrue(apis.stream().map(Api::getId).toList().containsAll(asList("api-to-findById", "grouped-api")));
    }

    @Test
    public void shouldFindByNameAndVersion() {
        List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().name("api-to-findById name").version("1").build(),
            ApiFieldFilter.allFields()
        );
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByNameAndVersionWithoutDefinition() {
        List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().name("api-to-findById name").version("1").build(),
            new ApiFieldFilter.Builder().excludeDefinition().build()
        );
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
        assertNull(apis.iterator().next().getDefinition());
    }

    @Test
    public void shouldFindByCrossId() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().crossId("searched-crossId").build(), ApiFieldFilter.allFields());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.get(0).getId());
    }

    @Test
    public void shouldFindByCrossId_andReturnEmptyListIfNotFound() {
        List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().crossId("api-cross-id-not-existing").build(),
            ApiFieldFilter.allFields()
        );
        assertNotNull(apis);
        assertTrue(apis.isEmpty());
    }

    @Test
    public void shouldFindByLifecycleStates() {
        final List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().lifecycleStates(singletonList(PUBLISHED)).build(),
            ApiFieldFilter.allFields()
        );
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(3, apis.size());
        assertTrue(apis.stream().map(Api::getId).toList().containsAll(asList("api-to-update", "grouped-api")));
        assertEquals(PUBLISHED, apis.get(0).getApiLifecycleState());
        assertEquals(PUBLISHED, apis.get(1).getApiLifecycleState());
        assertEquals(PUBLISHED, apis.get(2).getApiLifecycleState());
    }

    @Test
    public void shouldFindByDefinitionVersionV4() {
        final List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().definitionVersion(singletonList(DefinitionVersion.V4)).build(),
            ApiFieldFilter.allFields()
        );
        assertNotNull(apis);
        assertEquals(1, apis.size());
        assertEquals("async-api", apis.get(0).getId());
        assertEquals(DefinitionVersion.V4, apis.get(0).getDefinitionVersion());
        assertEquals(ApiType.MESSAGE, apis.get(0).getType());
        assertEquals("async-searched-crossId", apis.get(0).getCrossId());
    }

    @Test
    public void shouldFindByDefinitionVersionNull() {
        final List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().definitionVersion(singletonList(null)).environmentId("DEV").build(),
            ApiFieldFilter.allFields()
        );
        assertNotNull(apis);
        assertEquals(2, apis.size());
        assertTrue(apis.stream().map(Api::getId).toList().containsAll(asList("api-to-delete", "big-name")));
        assertNull(apis.get(0).getDefinitionVersion());
        assertNull(apis.get(1).getDefinitionVersion());
    }

    @Test
    public void shouldFindByIntegrationId() {
        var integrationId = "integration-id";
        final List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().integrationId(integrationId).build(),
            ApiFieldFilter.allFields()
        );
        assertThat(apis).isNotNull().isNotEmpty().hasSize(1);
    }

    @Test
    public void shouldReturnUniqueApiWhenSearchApisWithCategories() {
        List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().ids(List.of("api-with-many-categories")).build(),
            ApiFieldFilter.allFields()
        );

        assertNotNull(apis);
        assertEquals(1, apis.size());
        assertTrue(apis.stream().map(Api::getId).toList().contains("api-with-many-categories"));
    }

    @Test
    public void shouldListCategories() throws TechnicalException {
        final Set<String> categories = apiRepository.listCategories(new ApiCriteria.Builder().build());
        assertNotNull(categories);
        Set<String> expectedCategories = new LinkedHashSet<>();
        expectedCategories.add("category-1");
        expectedCategories.add("cycling");
        expectedCategories.add("hiking");
        expectedCategories.add("my-async-category");
        expectedCategories.add("my-category");
        expectedCategories.add("my-many-category");
        expectedCategories.add("my-many-category-2");
        assertEquals(expectedCategories, categories);
    }

    @Test
    public void shouldListCategoriesWithCriteria() throws TechnicalException {
        final Set<String> categories = apiRepository.listCategories(new ApiCriteria.Builder().ids("api-to-findById").build());
        assertNotNull(categories);
        Set<String> expectedCategories = new LinkedHashSet<>();
        expectedCategories.add("my-category");
        assertEquals(expectedCategories, categories);
    }

    @Test
    public void shouldFindByEnvironmentIdAndCrossId_returnOptionalPresent() throws TechnicalException {
        Optional<Api> api = apiRepository.findByEnvironmentIdAndCrossId("ENV6", "searched-crossId2");
        assertTrue(api.isPresent());
        assertEquals("crossId-api", api.get().getId());
    }

    @Test
    public void shouldNotFindByEnvironmentIdAndCrossId_returnOptionalEmpty() throws TechnicalException {
        Optional<Api> api = apiRepository.findByEnvironmentIdAndCrossId("unknown-env-id", "unknown-cross-id");
        assertTrue(api.isEmpty());
    }

    @Test(expected = Exception.class)
    public void shouldFindMultipleByEnvironmentIdAndCrossId_throwsException() throws TechnicalException {
        apiRepository.findByEnvironmentIdAndCrossId("ENV6", "duplicated-crossId");
    }

    @Test
    public void searchIdsWithPageable() {
        Page<String> apiIds = apiRepository.searchIds(
            List.of(new ApiCriteria.Builder().version("1").build()),
            new PageableBuilder().pageNumber(0).pageSize(2).build(),
            null
        );

        assertEquals(4, apiIds.getTotalElements());
        assertEquals(2, apiIds.getPageElements());

        assertEquals("api-to-delete", apiIds.getContent().get(0));
        assertEquals("api-to-findById", apiIds.getContent().get(1));

        apiIds =
            apiRepository.searchIds(
                List.of(new ApiCriteria.Builder().version("1").build()),
                new PageableBuilder().pageNumber(1).pageSize(2).build(),
                null
            );

        assertEquals(4, apiIds.getTotalElements());
        assertEquals(2, apiIds.getPageElements());

        assertEquals("api-to-update", apiIds.getContent().get(0));
        assertEquals("grouped-api", apiIds.getContent().get(1));

        apiIds =
            apiRepository.searchIds(
                List.of(new ApiCriteria.Builder().version("1").build()),
                new PageableBuilder().pageNumber(0).pageSize(4).build(),
                new SortableBuilder().field("updated_at").order(Order.DESC).build()
            );

        assertEquals("api-to-update", apiIds.getContent().get(0));
        assertEquals("api-to-delete", apiIds.getContent().get(1));
    }

    @Test
    public void shouldReturnUniqueIdsWhenSearchApisWithCategories() {
        Page<String> apiIds = apiRepository.searchIds(
            List.of(new ApiCriteria.Builder().ids(List.of("api-with-many-categories")).build()),
            new PageableBuilder().pageNumber(0).pageSize(2).build(),
            null
        );

        assertEquals(1, apiIds.getTotalElements());
        assertEquals(1, apiIds.getPageElements());

        assertEquals("api-with-many-categories", apiIds.getContent().get(0));

        apiIds =
            apiRepository.searchIds(
                List.of(new ApiCriteria.Builder().version("1").build()),
                new PageableBuilder().pageNumber(1).pageSize(2).build(),
                null
            );

        assertEquals(4, apiIds.getTotalElements());
        assertEquals(2, apiIds.getPageElements());

        assertEquals("api-to-update", apiIds.getContent().get(0));
        assertEquals("grouped-api", apiIds.getContent().get(1));

        apiIds =
            apiRepository.searchIds(
                List.of(new ApiCriteria.Builder().version("1").build()),
                new PageableBuilder().pageNumber(0).pageSize(4).build(),
                new SortableBuilder().field("updated_at").order(Order.DESC).build()
            );

        assertEquals("api-to-update", apiIds.getContent().get(0));
        assertEquals("api-to-delete", apiIds.getContent().get(1));
    }

    @Test
    public void shouldStreamSearch() {
        List<Api> apis = apiRepository.search(null, null, ApiFieldFilter.allFields(), 2).collect(toList());

        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(12, apis.size());
    }

    @Test
    public void shouldFindIdByEnvironmentIdAndCrossId() throws TechnicalException {
        Optional<String> optApiId = apiRepository.findIdByEnvironmentIdAndCrossId("ENV6", "searched-crossId2");
        assertTrue(optApiId.isPresent());
        assertEquals("crossId-api", optApiId.get());
    }

    @Test
    public void shouldExist() throws TechnicalException {
        boolean exist = apiRepository.existById("api-to-delete");
        assertTrue(exist);
    }

    @Test
    public void shouldNotExist() throws TechnicalException {
        boolean exist = apiRepository.existById("unknown-api");
        assertFalse(exist);
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        final var beforeDeletion = apiRepository
            .search(new ApiCriteria.Builder().environmentId("DEFAULT").build(), ApiFieldFilter.defaultFields())
            .stream()
            .map(Api::getId)
            .toList();

        final var deleted = apiRepository.deleteByEnvironmentId("DEFAULT");
        final var nbAfterDeletion = apiRepository
            .search(new ApiCriteria.Builder().environmentId("DEFAULT").build(), ApiFieldFilter.defaultFields())
            .size();

        assertEquals(beforeDeletion.size(), deleted.size());
        assertTrue(deleted.containsAll(beforeDeletion));
        assertEquals(0, nbAfterDeletion);
    }
}
