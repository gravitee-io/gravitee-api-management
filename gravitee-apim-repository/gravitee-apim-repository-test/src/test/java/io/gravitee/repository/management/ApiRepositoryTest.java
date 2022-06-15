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
package io.gravitee.repository.management;

import static io.gravitee.repository.management.model.ApiLifecycleState.PUBLISHED;
import static io.gravitee.repository.management.model.LifecycleState.STOPPED;
import static io.gravitee.repository.management.model.Visibility.PUBLIC;
import static io.gravitee.repository.utils.DateUtils.compareDate;
import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.config.AbstractManagementRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiFieldInclusionFilter;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
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
        assertEquals("Invalid saved api version.", api.getVersion(), apiSaved.getVersion());
        assertEquals("Invalid deployment lifecycle.", api.getLifecycleState(), apiSaved.getLifecycleState());
        assertEquals("Invalid api private api status.", api.getVisibility(), apiSaved.getVisibility());
        assertEquals("Invalid api definition.", api.getDefinition(), apiSaved.getDefinition());
        assertTrue("Invalid api createdAt.", compareDate(api.getCreatedAt(), apiSaved.getCreatedAt()));
        assertTrue("Invalid api updateAt.", compareDate(api.getUpdatedAt(), apiSaved.getUpdatedAt()));
        assertEquals("Invalid api lifecycle.", api.getApiLifecycleState(), apiSaved.getApiLifecycleState());
        assertTrue("Invalid api disable membership notifications", apiSaved.isDisableMembershipNotifications());

        // test delete
        int nbApplicationBefore = apiRepository.search(null).size();
        apiRepository.delete(apiId);
        int nbApplicationAfter = apiRepository.search(null).size();
        assertFalse("api was deleted", apiRepository.findById(apiId).isPresent());
        assertEquals("Invalid number of apis after deletion", nbApplicationBefore - 1, nbApplicationAfter);
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

        int nbAPIsBeforeUpdate = apiRepository.search(null).size();
        apiRepository.update(api);
        int nbAPIsAfterUpdate = apiRepository.search(null).size();

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
        List<Api> apis = apiRepository.search(null);

        assertNotNull(apis);
        assertFalse("Api list is empty", apis.isEmpty());
        assertNotNull("Api is null", apis.iterator().next());
    }

    @Test
    public void findAllTestCriteriaEmpty() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().build());

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
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().ids("api-to-delete", "api-to-update", "unknown").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(2, apis.size());
        assertTrue(apis.stream().map(Api::getId).collect(toList()).containsAll(asList("api-to-delete", "api-to-update")));
    }

    @Test
    public void shouldFindByEnvironmentsDevs() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().environments(Arrays.asList("DEV", "DEVS")).build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(3, apis.size());
        assertTrue(apis.stream().map(Api::getId).collect(toList()).containsAll(asList("api-to-delete", "api-to-update", "big-name")));
    }

    @Test
    public void shouldFindIdsWithMultipleApiCriteria() {
        List<String> apis = apiRepository.searchIds(
            new ApiCriteria.Builder().ids("api-to-delete", "api-to-update", "unknown").build(),
            new ApiCriteria.Builder().environments(Arrays.asList("DEV", "DEVS")).build(),
            new ApiCriteria.Builder().groups("api-group", "unknown").build()
        );
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(4, apis.size());
        assertTrue(apis.containsAll(asList("api-to-delete", "api-to-update", "big-name", "grouped-api")));
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
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().groups("api-group", "unknown").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("grouped-api", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByName() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().name("api-to-findById name").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByLabel() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().label("label 1").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByState() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().state(STOPPED).build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(4, apis.size());
        assertTrue(
            apis
                .stream()
                .map(Api::getId)
                .collect(toList())
                .containsAll(asList("api-to-delete", "api-to-update", "api-to-findById", "grouped-api"))
        );
    }

    @Test
    public void shouldFindByEnvironment() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().environmentId("DEFAULT").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(2, apis.size());
        assertTrue(apis.stream().map(Api::getId).collect(toList()).containsAll(asList("grouped-api", "api-to-findById")));
    }

    @Test
    public void shouldFindByVersion() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().version("1").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(4, apis.size());
        assertTrue(
            apis
                .stream()
                .map(Api::getId)
                .collect(toList())
                .containsAll(asList("api-to-delete", "api-to-update", "api-to-findById", "grouped-api"))
        );
    }

    @Test
    public void shouldFindByView() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().category("my-category").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByVisibility() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().visibility(PUBLIC).build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(2, apis.size());
        assertTrue(apis.stream().map(Api::getId).collect(toList()).containsAll(asList("api-to-findById", "grouped-api")));
    }

    @Test
    public void shouldFindByNameAndVersion() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().name("api-to-findById name").version("1").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
    }

    @Test
    public void shouldFindByNameAndVersionWithoutDefinition() {
        List<Api> apis = apiRepository.search(
            new ApiCriteria.Builder().name("api-to-findById name").version("1").build(),
            new ApiFieldExclusionFilter.Builder().excludeDefinition().build()
        );
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.iterator().next().getId());
        assertNull(apis.iterator().next().getDefinition());
    }

    @Test
    public void shouldFindByCrossId() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().crossId("searched-crossId").build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(1, apis.size());
        assertEquals("api-to-findById", apis.get(0).getId());
    }

    @Test
    public void shouldFindByCrossId_andReturnEmptyListIfNotFound() {
        List<Api> apis = apiRepository.search(new ApiCriteria.Builder().crossId("api-cross-id-not-existing").build());
        assertNotNull(apis);
        assertTrue(apis.isEmpty());
    }

    @Test
    public void searchByPageable() {
        Page<Api> apiPage = apiRepository.search(
            new ApiCriteria.Builder().version("1").build(),
            new PageableBuilder().pageNumber(0).pageSize(2).build()
        );

        assertEquals(4, apiPage.getTotalElements());
        assertEquals(2, apiPage.getPageElements());
        Iterator<Api> apiIterator = apiPage.getContent().iterator();
        assertEquals("api-to-delete", apiIterator.next().getId());
        assertEquals("api-to-findById", apiIterator.next().getId());

        apiPage =
            apiRepository.search(new ApiCriteria.Builder().version("1").build(), new PageableBuilder().pageNumber(1).pageSize(2).build());

        assertEquals(4, apiPage.getTotalElements());
        assertEquals(2, apiPage.getPageElements());
        apiIterator = apiPage.getContent().iterator();
        assertEquals("api-to-update", apiIterator.next().getId());
        assertEquals("grouped-api", apiIterator.next().getId());
    }

    @Test
    public void shouldFindByLifecycleStates() {
        final List<Api> apis = apiRepository.search(new ApiCriteria.Builder().lifecycleStates(singletonList(PUBLISHED)).build());
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(3, apis.size());
        assertTrue(apis.stream().map(Api::getId).collect(toList()).containsAll(asList("api-to-update", "grouped-api")));
        assertEquals(PUBLISHED, apis.get(0).getApiLifecycleState());
        assertEquals(PUBLISHED, apis.get(1).getApiLifecycleState());
        assertEquals(PUBLISHED, apis.get(2).getApiLifecycleState());
    }

    @Test
    public void shouldOnlyIncludeRequiredAndDefaultFields() {
        ApiCriteria criteria = new ApiCriteria.Builder().lifecycleStates(singletonList(PUBLISHED)).build();
        ApiFieldInclusionFilter filter = ApiFieldInclusionFilter.builder().includeCategories().build();

        Set<Api> apis = apiRepository.search(criteria, filter);
        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(3, apis.size());

        assertTrue(apis.stream().map(Api::getId).collect(toList()).containsAll(asList("api-to-update", "grouped-api", "big-name")));
        assertTrue(apis.stream().map(Api::getName).anyMatch(Objects::isNull));
        assertTrue(apis.stream().map(Api::getDefinition).anyMatch(Objects::isNull));
        assertTrue(apis.stream().map(Api::getPicture).anyMatch(Objects::isNull));
        assertTrue(apis.stream().map(Api::getBackground).anyMatch(Objects::isNull));

        List<String> categories = apis.stream().map(Api::getCategories).filter(Objects::nonNull).flatMap(Set::stream).collect(toList());
        assertTrue(categories.contains("category-1"));
    }

    @Test
    public void shouldListCategories() throws TechnicalException {
        final Set<String> categories = apiRepository.listCategories(new ApiCriteria.Builder().build());
        assertNotNull(categories);
        Set<String> expectedCategories = new LinkedHashSet<>();
        expectedCategories.add("category-1");
        expectedCategories.add("cycling");
        expectedCategories.add("hiking");
        expectedCategories.add("my-category");
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
}
