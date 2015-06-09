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

import io.gravitee.gateway.api.Registry;
import io.gravitee.model.Api;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
public class MongoDBRegistryTest extends AbstractMongoDBTest {

    private static final String APIS_DATA = "/data/apis.json";

    private Registry registry;

    @Override
    protected String getJsonDataSetResourceName() {
        return APIS_DATA;
    }

    @Before
    public void init() {
        registry = new MongoDBRegistry(this.getClass().getResource(APIS_DATA).getPath());
    }

    @Test
    public void shouldListAll() {
        assertEquals(1, registry.listAll().size());
    }

    @Test
    public void shouldCreateApi() {
        final String name = "api-users";
        final Api api = new Api();
        api.setName(name);
        api.setPublicURI(URI.create("http://localhost:8082/users"));
        api.setTargetURI(URI.create("http://localhost:8083/app/users"));
        assertTrue(registry.createApi(api));
        assertEquals(1, registry.listAll().size());
        registry.reloadApi(name);
        assertEquals(2, registry.listAll().size());
    }

    @Test
    public void shouldNotCreateApi() {
        final String name = "api-users";
        final Api api = new Api();
        api.setName(name);
        assertFalse(registry.createApi(api));
        assertEquals(1, registry.listAll().size());
        registry.reloadApi(name);
        assertEquals(1, registry.listAll().size());
    }

    @Test
    public void shouldStartAndStopApi() {
        final String name = "api-test";
        // stop API
        assertTrue(registry.statusApi(name));
        assertTrue(registry.stopApi(name));
        assertTrue(registry.statusApi(name));
        assertTrue(registry.reloadAll());
        assertFalse(registry.statusApi(name));

        // start API
        assertTrue(registry.startApi(name));
        assertFalse(registry.statusApi(name));
        assertTrue(registry.reloadAll());
        assertTrue(registry.statusApi(name));
    }

    @Test
    public void shouldFindMatchingApi() {
        assertNotNull(registry.findMatchingApi("/test"));
        assertNull(registry.findMatchingApi("/myapi"));
    }
}
