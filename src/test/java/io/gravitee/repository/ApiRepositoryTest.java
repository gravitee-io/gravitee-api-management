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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.utils.DateUtils.parse;
import static org.junit.Assert.*;

public class ApiRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/api-tests/";
    }

    @Test
    public void createApiTest() throws Exception {
        String apiName = "sample-new";

        Api api = new Api();
        api.setId(apiName);
        api.setName(apiName);
        api.setVersion("1");
        api.setLifecycleState(LifecycleState.STOPPED);
        api.setVisibility(Visibility.PRIVATE);
        api.setDefinition("{}");
        api.setCreatedAt(parse("11/02/2016"));
        api.setUpdatedAt(parse("12/02/2016"));

        apiRepository.create(api);

        Optional<Api> optional = apiRepository.findById(apiName);
        assertTrue("Api saved not found", optional.isPresent());

        Api apiSaved = optional.get();
        assertEquals("Invalid saved api version.", api.getVersion(), apiSaved.getVersion());
        assertEquals("Invalid api lifecycle.", api.getLifecycleState(), apiSaved.getLifecycleState());
        assertEquals("Invalid api private api status.", api.getVisibility(), apiSaved.getVisibility());
        assertEquals("Invalid api definition.", api.getDefinition(), apiSaved.getDefinition());
        assertEquals("Invalid api createdAt.", api.getCreatedAt(), apiSaved.getCreatedAt());
        assertEquals("Invalid api updateAt.", api.getUpdatedAt(), apiSaved.getUpdatedAt());
    }

    @Test
    public void shouldUpdate() throws Exception {
        Optional<Api> optional = apiRepository.findById("api-to-update");
        assertTrue("API to update not found", optional.isPresent());
        assertEquals("Invalid saved api name.", "api-to-update", optional.get().getName());

        final Api api = optional.get();
        api.setName("New API name");
        api.setDescription("New description");
        api.setViews(new HashSet<>(Arrays.asList("view1", "view2")));
        api.setDefinition("New definition");
        api.setDeployedAt(parse("11/02/2016"));
        api.setGroups(Collections.singleton("New group"));
        api.setLifecycleState(LifecycleState.STARTED);
        api.setPicture("New picture");
        api.setCreatedAt(parse("11/02/2016"));
        api.setUpdatedAt(parse("13/11/2016"));
        api.setVersion("New version");
        api.setVisibility(Visibility.PRIVATE);

        int nbAPIsBeforeUpdate = apiRepository.findAll().size();
        apiRepository.update(api);
        int nbAPIsAfterUpdate = apiRepository.findAll().size();

        assertEquals(nbAPIsBeforeUpdate, nbAPIsAfterUpdate);

        Optional<Api> optionalUpdated = apiRepository.findById("api-to-update");
        assertTrue("API to update not found", optionalUpdated.isPresent());

        final Api apiUpdated = optionalUpdated.get();
        assertEquals("Invalid saved API name.", "New API name", apiUpdated.getName());
        assertEquals("Invalid API description.", "New description", apiUpdated.getDescription());
        assertEquals("Invalid API views.", new HashSet<>(Arrays.asList("view1", "view2")), apiUpdated.getViews());
        assertEquals("Invalid API definition.", "New definition", apiUpdated.getDefinition());
        assertEquals("Invalid API deployment date.", parse("11/02/2016"), apiUpdated.getDeployedAt());
        assertEquals("Invalid API group.", Collections.singleton("New group"), apiUpdated.getGroups());
        assertEquals("Invalid API lifecycle state.", LifecycleState.STARTED, apiUpdated.getLifecycleState());
        assertEquals("Invalid API picture.", "New picture", apiUpdated.getPicture());
        assertEquals("Invalid API create date.", parse("11/02/2016"), apiUpdated.getCreatedAt());
        assertEquals("Invalid API update date.", parse("13/11/2016"), apiUpdated.getUpdatedAt());
        assertEquals("Invalid API version.", "New version", apiUpdated.getVersion());
        assertEquals("Invalid API visibility.", Visibility.PRIVATE, apiUpdated.getVisibility());
    }

    @Test
    public void findByIdTest() throws Exception {
        Optional<Api> optional = apiRepository.findById("api-to-findById");
        assertTrue("Find api by name return no result ", optional.isPresent());

        Api api = optional.get();
        assertEquals("Invalid api name", "api-to-findById", api.getName());
        assertEquals("Invalid api version", "1", api.getVersion());
        assertEquals("Invalid api visibility", Visibility.PUBLIC, api.getVisibility());
        assertEquals("Invalid api lifecycle state", LifecycleState.STOPPED, api.getLifecycleState());
        assertEquals("Invalid api labels", 2, api.getLabels().size());
        assertEquals("Invalid api label at position 0", "label 1", api.getLabels().iterator().next());
    }

    @Test
    public void findByIdMissingTest() throws Exception {
        Optional<Api> optional = apiRepository.findById("findByNameMissing");
        assertFalse("Find api by name on missing api return a result", optional.isPresent());
    }

    @Test
    public void findAllTest() throws Exception {
        Set<Api> apis = apiRepository.findAll();

        assertNotNull(apis);
        assertFalse("Fail to resolve api in findAll", apis.isEmpty());
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
    public void shouldFindByIds() throws Exception {
        Set<Api> apis = apiRepository.findByIds(Arrays.asList("api-to-delete", "api-to-update", "unknown"));

        assertNotNull(apis);
        assertFalse(apis.isEmpty());
        assertEquals(2, apis.size());
        assertTrue(apis.
                stream().
                map(Api::getId).
                collect(Collectors.toList()).
                containsAll(Arrays.asList("api-to-delete", "api-to-update")));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownApi() throws Exception {
        Api unknownApi = new Api();
        unknownApi.setId("unknown");
        apiRepository.update(unknownApi);
        fail("An unknown api should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        apiRepository.update(null);
        fail("A null api should not be updated");
    }
}
