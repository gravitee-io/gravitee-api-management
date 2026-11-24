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
package io.gravitee.gamma.rest.resources.guardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.guardrail.GuardrailAuditFixture;
import io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures;
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.guardrail.GuardrailsResourceTest.GuardrailsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * The deployment history of a Guardrail over HTTP, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/guardrails/{guardrailId}/deployments}: the list, the detail
 * of a revision and the rollback to one. Wired as {@link GuardrailsResourceTest}, whose Spring context it shares.
 */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, GuardrailsTestConfiguration.class })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardrailDeploymentsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final String OTHER_ENVIRONMENT = "other-env";
    private static final String GUARDRAIL_ID = GuardrailFixtures.GUARDRAIL_ID;

    @Inject
    private InMemoryGuardrailRepository guardrailRepository;

    @Inject
    private GuardrailPublicationFixture guardrailPublications;

    @Inject
    private GuardrailAuditFixture withdrawalAudits;

    @Inject
    private LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/guardrails";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
        License license = mock(License.class);
        when(license.isFeatureEnabled("apim-guardians")).thenReturn(true);
        when(licenseManager.getPlatformLicense()).thenReturn(license);
    }

    @AfterEach
    void resetRepository() {
        guardrailRepository.reset();
        guardrailPublications.reset();
        withdrawalAudits.reset();
    }

    private static Guardrail guardrail(String id, String environmentId, String name) {
        return GuardrailFixtures.aGuardrail(d -> d.id(id).environmentId(environmentId).name(name))
            .version(1)
            .build();
    }

    @BeforeEach
    void seed() {
        guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Credential leakage"));
        withdrawalAudits.users.initWith(List.of(BaseUserEntity.builder().id(USER_NAME).firstname("Ada").lastname("Lovelace").build()));
    }

    private void deploy(Map<String, Object> body) {
        assertThat(rootTarget("/" + GUARDRAIL_ID + "/_deploy").request().post(Entity.json(body)).getStatus())
            .as("deployment")
            .isEqualTo(HttpStatusCode.OK_200);
    }

    /** A deployment and its withdrawal answer the Guardrail, so they name its version, which neither changed. */
    @Test
    void should_name_the_unchanged_version_in_the_etag_of_a_deployment_and_of_its_withdrawal() {
        var deployed = rootTarget("/" + GUARDRAIL_ID + "/_deploy").request().post(Entity.json(Map.of()));
        var withdrawn = rootTarget("/" + GUARDRAIL_ID + "/_undeploy").request().post(null);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(deployed.getEntityTag()).as("after the deployment").isEqualTo(new EntityTag("1"));
            softly.assertThat(withdrawn.getStatus()).as("withdrawal").isEqualTo(HttpStatusCode.OK_200);
            softly.assertThat(withdrawn.getEntityTag()).as("after the withdrawal").isEqualTo(new EntityTag("1"));
        });
    }

    @Test
    void should_list_the_deployed_revisions_newest_first() {
        deploy(Map.of("deploymentLabel", "First"));
        deploy(Map.of());

        var response = rootTarget("/" + GUARDRAIL_ID + "/deployments").request().get();

        assertThat(response.getStatus()).as("history response").isEqualTo(HttpStatusCode.OK_200);
        var body = response.readEntity(JsonNode.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(body.at("/data/0/id").asText()).as("newest revision").isEqualTo(guardrailPublications.lastPublicationId());
            softly.assertThat(body.at("/data/0/label").isMissingNode()).as("unlabelled revision").isTrue();
            softly.assertThat(body.at("/data/1/label").asText()).as("deployment label").isEqualTo("First");
            softly.assertThat(body.at("/data/1/id").asText()).as("revision id").isNotBlank();
            softly.assertThat(body.at("/data/0/version").isMissingNode()).as("no version number").isTrue();
            softly
                .assertThat(Instant.parse(body.at("/data/0/deployedAt").asText()))
                .as("ISO 8601 deployment date")
                .isEqualTo(guardrailPublications.published(ENVIRONMENT, GUARDRAIL_ID).at());
            softly.assertThat(body.at("/data/0/deployedBy/id").asText()).as("deployer id").isEqualTo(USER_NAME);
            softly.assertThat(body.at("/data/0/deployedBy/displayName").asText()).as("deployer name").isEqualTo("Ada Lovelace");
            softly.assertThat(body.at("/pagination/totalCount").asLong()).as("total").isEqualTo(2);
        });
    }

    @Test
    void should_paginate_the_revisions() {
        deploy(Map.of("deploymentLabel", "First"));
        deploy(Map.of());

        var body = rootTarget("/" + GUARDRAIL_ID + "/deployments")
            .queryParam("page", 2)
            .queryParam("perPage", 1)
            .request()
            .get()
            .readEntity(JsonNode.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(body.at("/data").size()).as("page content").isEqualTo(1);
            softly.assertThat(body.at("/data/0/label").asText()).as("oldest revision").isEqualTo("First");
            softly.assertThat(body.at("/pagination/page").asInt()).as("page").isEqualTo(2);
            softly.assertThat(body.at("/pagination/perPage").asInt()).as("page size").isEqualTo(1);
            softly.assertThat(body.at("/pagination/totalCount").asLong()).as("total").isEqualTo(2);
        });
    }

    @Test
    void should_return_an_empty_history_for_a_guardrail_never_deployed() {
        var body = rootTarget("/" + GUARDRAIL_ID + "/deployments").request().get().readEntity(JsonNode.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(body.at("/data").isEmpty()).as("no revision").isTrue();
            softly.assertThat(body.at("/pagination/totalCount").asLong()).as("total").isZero();
        });
    }

    @Test
    void should_return_404_for_an_unknown_guardrail() {
        assertThat(rootTarget("/unknown/deployments").request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_return_404_for_a_guardrail_from_another_environment() {
        guardrailRepository.reset();
        guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, OTHER_ENVIRONMENT, "Elsewhere"));

        assertThat(rootTarget("/" + GUARDRAIL_ID + "/deployments").request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_refuse_the_history_without_the_read_permission() {
        when(
            permissionService.hasPermission(
                any(),
                eq(RolePermission.ENVIRONMENT_GUARDRAIL),
                any(),
                eq(new RolePermissionAction[] { RolePermissionAction.READ })
            )
        ).thenReturn(false);

        assertThat(rootTarget("/" + GUARDRAIL_ID + "/deployments").request().get().getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
    }

    @Test
    void should_return_a_revision_with_the_definition_it_deployed() {
        deploy(Map.of("deploymentLabel", "First"));
        var revisionId = latestRevisionId();
        guardrailRepository.reset();
        guardrailRepository.givenGuardrail(guardrail(GUARDRAIL_ID, ENVIRONMENT, "Renamed since"));
        deploy(Map.of());

        var response = rootTarget("/" + GUARDRAIL_ID + "/deployments/" + revisionId).request().get();

        assertThat(response.getStatus()).as("revision response").isEqualTo(HttpStatusCode.OK_200);
        var body = response.readEntity(JsonNode.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(body.at("/id").asText()).as("revision id").isEqualTo(revisionId);
            softly.assertThat(body.at("/label").asText()).as("deployment label").isEqualTo("First");
            softly.assertThat(Instant.parse(body.at("/deployedAt").asText())).as("ISO 8601 deployment date").isNotNull();
            softly.assertThat(body.at("/deployedBy/displayName").asText()).as("deployer name").isEqualTo("Ada Lovelace");
            softly.assertThat(body.at("/name").asText()).as("deployed name").isEqualTo("Credential leakage");
            softly.assertThat(body.at("/phase").asText()).as("deployed phase").isEqualTo("REQUEST");
            softly.assertThat(body.at("/apiTypes/0").asText()).as("deployed API type").isEqualTo("llm-proxy");
            softly.assertThat(body.at("/policies").isArray()).as("deployed guardrail policy sequence").isTrue();
            softly.assertThat(body.has("environmentId")).as("environment left out").isFalse();
        });
    }

    @Test
    void should_return_404_for_an_unknown_revision() {
        deploy(Map.of());

        assertThat(rootTarget("/" + GUARDRAIL_ID + "/deployments/unknown").request().get().getStatus()).isEqualTo(
            HttpStatusCode.NOT_FOUND_404
        );
    }

    @Test
    void should_return_404_for_a_revision_of_an_unknown_guardrail() {
        deploy(Map.of());
        var revisionId = latestRevisionId();

        assertThat(rootTarget("/unknown/deployments/" + revisionId).request().get().getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_refuse_a_revision_without_the_read_permission() {
        deploy(Map.of());
        var revisionId = latestRevisionId();
        when(
            permissionService.hasPermission(
                any(),
                eq(RolePermission.ENVIRONMENT_GUARDRAIL),
                any(),
                eq(new RolePermissionAction[] { RolePermissionAction.READ })
            )
        ).thenReturn(false);

        assertThat(rootTarget("/" + GUARDRAIL_ID + "/deployments/" + revisionId).request().get().getStatus()).isEqualTo(
            HttpStatusCode.FORBIDDEN_403
        );
    }

    @Test
    void should_roll_the_draft_back_to_a_revision_without_deploying_it() {
        deploy(Map.of());
        var revisionId = latestRevisionId();
        var renamed = Map.of(
            "name",
            "Renamed since",
            "apiTypes",
            List.of("llm-proxy"),
            "phase",
            "REQUEST",
            "policies",
            List.of(Map.of("name", "Detect credentials", "policy", "data-logging-masking", "enabled", true))
        );
        assertThat(rootTarget("/" + GUARDRAIL_ID).request().header(HttpHeaders.IF_MATCH, "*").put(Entity.json(renamed)).getStatus())
            .as("update")
            .isEqualTo(HttpStatusCode.OK_200);

        var response = rollback(revisionId);

        assertThat(response.getStatus()).as("rollback response").isEqualTo(HttpStatusCode.OK_200);
        assertThat(response.getEntityTag()).as("created at 1, updated to 2, rolled back to 3").isEqualTo(new EntityTag("3"));
        var body = response.readEntity(JsonNode.class);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(body.at("/name").asText()).as("restored name").isEqualTo("Credential leakage");
            softly.assertThat(body.at("/lifecycleState").asText()).as("draft matches what runs").isEqualTo("DEPLOYED");
            softly.assertThat(body.at("/deployedRevisionId").asText()).as("revision running").isEqualTo(revisionId);
            softly.assertThat(guardrailPublications.publications()).as("nothing published").isEqualTo(1);
        });
    }

    @Test
    void should_return_404_when_rolling_back_to_an_unknown_revision() {
        deploy(Map.of());

        assertThat(rollback("unknown").getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_refuse_a_rollback_without_the_update_permission() {
        deploy(Map.of());
        var revisionId = latestRevisionId();
        when(
            permissionService.hasPermission(
                any(),
                eq(RolePermission.ENVIRONMENT_GUARDRAIL),
                any(),
                eq(new RolePermissionAction[] { RolePermissionAction.UPDATE })
            )
        ).thenReturn(false);

        assertThat(rollback(revisionId).getStatus()).isEqualTo(HttpStatusCode.FORBIDDEN_403);
    }

    private Response rollback(String revisionId) {
        return rootTarget("/" + GUARDRAIL_ID + "/deployments/" + revisionId + "/_rollback").request().post(Entity.json(Map.of()));
    }

    private String latestRevisionId() {
        return rootTarget("/" + GUARDRAIL_ID + "/deployments").request().get().readEntity(JsonNode.class).at("/data/0/id").asText();
    }
}
