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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.PolicyEntity;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Nicolas Geraud (nicolas.geraud at gmail.com)
 */
public class PoliciesResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "policies";
    }

    @Test
    public void shouldGetPoliciesemptyList() {
        when(policyService.findAll(null)).thenReturn(Collections.emptySet());

        final Response response = envTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertTrue("empty", response.readEntity(Set.class).isEmpty());
    }

    @Test
    public void shouldGetPoliciesList() {
        HashSet<PolicyEntity> policyEntities = new HashSet<>();
        PolicyEntity policyEntity = new PolicyEntity();
        policyEntities.add(policyEntity);
        policyEntity.setId("my-api");
        policyEntity.setName("My Api");

        when(policyService.findAll(null)).thenReturn(policyEntities);

        final Response response = envTarget().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        Set entity = response.readEntity(Set.class);
        assertFalse("not empty", entity.isEmpty());
        assertEquals("one element", 1, entity.size());
        Object o = entity.iterator().next();
        assertTrue(o instanceof LinkedHashMap);
        LinkedHashMap<String, String> elt = (LinkedHashMap<String, String>) o;
        assertEquals("id", "my-api", elt.get("id"));
        assertEquals("name", "My Api", elt.get("name"));
    }

    @Test
    public void shouldGetPoliciesListWithSchema() {
        HashSet<PolicyEntity> policyEntities = new HashSet<>();
        PolicyEntity policyEntity = new PolicyEntity();
        policyEntities.add(policyEntity);
        policyEntity.setId("my-api");

        when(policyService.findAll(null)).thenReturn(policyEntities);
        when(policyService.getSchema(any())).thenReturn("policy schema");

        final Response response = envTarget().queryParam("expand", "schema").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        Set entity = response.readEntity(Set.class);
        assertFalse("not empty", entity.isEmpty());
        assertEquals("one element", 1, entity.size());
        Object o = entity.iterator().next();
        assertTrue(o instanceof LinkedHashMap);
        LinkedHashMap<String, String> elt = (LinkedHashMap<String, String>) o;
        assertEquals("id", "my-api", elt.get("id"));
        assertEquals("schema", "policy schema", elt.get("schema"));
    }

    @Test
    public void shouldGetPoliciesListWithUnknownExpand() {
        HashSet<PolicyEntity> policyEntities = new HashSet<>();
        PolicyEntity policyEntity = new PolicyEntity();
        policyEntities.add(policyEntity);
        policyEntity.setId("my-api");

        when(policyService.findAll(null)).thenReturn(policyEntities);

        final Response response = envTarget().queryParam("expand", "unknown").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        Set entity = response.readEntity(Set.class);
        assertFalse("not empty", entity.isEmpty());
        assertEquals("one element", 1, entity.size());
        Object o = entity.iterator().next();
        assertTrue(o instanceof LinkedHashMap);
        LinkedHashMap<String, String> elt = (LinkedHashMap<String, String>) o;
        assertEquals("id", "my-api", elt.get("id"));
        assertFalse("unknown expand", elt.containsKey("schema"));
        assertFalse("unknown expand", elt.containsKey("unknown"));
    }
}
