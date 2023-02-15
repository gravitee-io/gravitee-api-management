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
package io.gravitee.rest.api.management.v4.rest.resource.installation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.rest.api.management.v4.rest.model.Organization;
import io.gravitee.rest.api.management.v4.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

public class OrganizationResourceTest extends AbstractResourceTest {

    public static final String ORG = "my-org";

    @Inject
    private OrganizationService organizationService;

    @Override
    protected String contextPath() {
        return "/organizations";
    }

    @Before
    public void init() {
        GraviteeContext.setCurrentOrganization(ORG);
        GraviteeContext.setCurrentEnvironment(null);
    }

    @Test
    public void shouldGetOrganizationById() {
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(ORG);
        organizationEntity.setName("org-name");
        organizationEntity.setDescription("A nice description");
        organizationEntity.setCockpitId("cockpit-id");
        organizationEntity.setDomainRestrictions(List.of("restriction-1"));
        organizationEntity.setFlowMode(FlowMode.BEST_MATCH);
        organizationEntity.setHrids(List.of("one-hrid"));

        doReturn(organizationEntity).when(organizationService).findById(eq(ORG));

        final Response response = rootTarget(ORG).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        var body = response.readEntity(Organization.class);
        assertNotNull(body);

        assertEquals(ORG, body.getId());
        assertEquals("org-name", body.getName());
        assertEquals("A nice description", body.getDescription());
    }
}
