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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fixtures.core.model.ApiCRDFixtures;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiResource_ExportCRDTest extends ApiResourceTest {

    private static final YAMLMapper YAML = new YAMLMapper();

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/_export/crd";
    }

    @Test
    public void should_not_export_when_no_definition_permission() {
        when(
            permissionService.hasPermission(
                GraviteeContext.getExecutionContext(),
                RolePermission.API_DEFINITION,
                API,
                RolePermissionAction.READ
            )
        )
            .thenReturn(false);
        Response response = rootTarget().request().get();
        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
    }

    @Test
    public void should_export() throws JsonProcessingException {
        Response response = rootTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);

        var yamlNode = YAML.readTree(response.readEntity(String.class));

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(yamlNode.get("metadata")).isNotNull();
            soft.assertThat(yamlNode.get("metadata").get("name").asText()).isEqualTo("api-name");
            soft.assertThat(yamlNode.get("spec")).isNotNull();
            soft.assertThat(yamlNode.get("spec").get("id").asText()).isEqualTo(ApiCRDFixtures.API_ID);
            soft.assertThat(yamlNode.get("spec").get("crossId").asText()).isEqualTo(ApiCRDFixtures.API_CROSS_ID);
            soft.assertThat(yamlNode.get("spec").get("name").asText()).isEqualTo(ApiCRDFixtures.API_NAME);
            soft.assertThat(yamlNode.get("spec").get("plans")).isNotEmpty();
            soft.assertThat(yamlNode.get("spec").get("plans").get(ApiCRDFixtures.PLAN_NAME)).isNotNull();
            soft.assertThat(yamlNode.get("spec").get("listeners")).isNotEmpty();
            soft.assertThat(yamlNode.get("spec").get("endpointGroups")).isNotEmpty();
        });
    }
}
