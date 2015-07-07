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
package io.gravitee.gateway.registry.mongodb;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.gateway.api.Repository;
import io.gravitee.model.Api;

/**
 * Allows to test {@code Api} operations on {@code MongoDBRegistry}.
 *
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class MongoDBRepositoryTest extends AbstractMongoDBTest {

    private static final String APIS_DATA = "/data/apis.json";

    private Repository repository;

    @Override
    protected String getJsonDataSetResourceName() {
        return APIS_DATA;
    }

    @Before
    public void init() {
        repository = new MongoDBRepository(this.getClass().getResource(APIS_DATA).getPath());
    }

    @Test
    public void shouldListAll() {
        assertEquals(1, repository.listAll().size());
    }

    @Test
    public void shouldCreateApi() {
        final String name = "api-users";
        final Api api = new Api();
        api.setName(name);
        api.setPublicURI(URI.create("http://localhost:8082/users"));
        api.setTargetURI(URI.create("http://localhost:8083/app/users"));
        assertEquals(1, repository.listAll().size());
        assertEquals(2, repository.fetchAll().size());
        assertTrue(repository.create(api));
        assertEquals(1, repository.listAll().size());
        assertEquals(3, repository.fetchAll().size());
    }

    @Test
    public void shouldUpdateApi() {
        final String name = "api-test";
        final URI oldTargetURI = repository.get(name).getTargetURI();
        final Api api = new Api();
        api.setName(name);
        api.setPublicURI(URI.create("http://localhost:8082/users"));
        api.setTargetURI(URI.create("http://localhost:8083/newapp/users"));
        assertEquals(1, repository.listAll().size());
        assertEquals(2, repository.fetchAll().size());
        assertTrue(repository.update(api));
        assertEquals(1, repository.listAll().size());
        assertEquals(2, repository.fetchAll().size());
        assertEquals(oldTargetURI, repository.get(name).getTargetURI());
        assertEquals(URI.create("/newapp/users"), repository.fetch(name).getTargetURI());
    }
}
