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
package io.gravitee.rest.api.management.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.model.platform.plugin.SchemaDisplayFormat;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Nicolas Geraud (nicolas.geraud at gmail.com)
 */
public class PoliciesResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "policies";
    }

    @Autowired
    protected LicenseManager licenseManager;

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
        policyEntity.setId("policy-1");
        policyEntity.setName("Policy 1");
        policyEntity.setDeployed(true);
        policyEntity.setFeature("feature-1");
        PolicyEntity policyEntity2 = new PolicyEntity();
        policyEntity2.setId("policy-2");
        policyEntity2.setName("Policy 2");
        policyEntity2.setDeployed(true);
        policyEntity2.setFeature("feature-2");
        PolicyEntity policyEntity3 = new PolicyEntity();
        policyEntity3.setId("policy-3");
        policyEntity3.setName("Policy 3");
        policyEntity3.setDeployed(false);
        policyEntity3.setFeature("feature-3");

        policyEntities.add(policyEntity);
        policyEntities.add(policyEntity2);
        policyEntities.add(policyEntity3);

        when(policyService.findAll(null)).thenReturn(policyEntities);
        var license = mock(License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("DEFAULT")).thenReturn(license);
        when(license.isFeatureEnabled("feature-1")).thenReturn(false);
        when(license.isFeatureEnabled("feature-2")).thenReturn(true);
        when(license.isFeatureEnabled("feature-3")).thenReturn(true);

        final Response response = envTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        Set<?> entity = response.readEntity(Set.class);
        assertThat(entity)
            .hasSize(3)
            .hasToString(
                "[{id=policy-2, name=Policy 2, onRequest=true, onResponse=false, deployed=true}, " +
                    "{id=policy-3, name=Policy 3, onRequest=true, onResponse=false, deployed=false}, " +
                    "{id=policy-1, name=Policy 1, onRequest=true, onResponse=false, deployed=false}]"
            );
    }

    @Test
    public void shouldGetPoliciesListWithSchema() {
        HashSet<PolicyEntity> policyEntities = new HashSet<>();
        PolicyEntity policyEntity = new PolicyEntity();
        policyEntities.add(policyEntity);
        policyEntity.setId("my-api");

        when(policyService.findAll(null)).thenReturn(policyEntities);
        when(policyService.getSchema(any(), eq(SchemaDisplayFormat.GV_SCHEMA_FORM))).thenReturn("policy schema");

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
