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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import inmemory.SharedPolicyGroupCrudServiceInMemory;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.use_case.CreateSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.DeleteSharedPolicyGroupUseCase;
import io.gravitee.apim.rest.api.automation.model.SharedPolicyGroupState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class SharedPolicyGroupResourceTest extends AbstractResourceTest {

    static final String HRID = "spg-foo";
    static final String LEGACY_ID = "raw-legacy-uuid";

    @Inject
    SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService;

    @Inject
    CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase;

    @Inject
    DeleteSharedPolicyGroupUseCase deleteSharedPolicyGroupUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/shared-policy-groups";
    }

    @AfterEach
    void tearDown() {
        sharedPolicyGroupCrudService.reset();
        reset(createSharedPolicyGroupUseCase);
    }

    private void givenExistingSharedPolicyGroup() {
        givenExistingSharedPolicyGroup(List.of(Step.builder().name("step-name").build()));
    }

    private void givenExistingSharedPolicyGroup(List<Step> steps) {
        var spg = HRIDToUUID.sharedPolicyGroup().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid(HRID);
        sharedPolicyGroupCrudService.initWith(
            List.of(
                SharedPolicyGroup.builder()
                    .id(spg.id())
                    .crossId(spg.crossId())
                    .hrid(HRID)
                    .environmentId(ENVIRONMENT)
                    .organizationId(ORGANIZATION)
                    .steps(steps)
                    .build()
            )
        );
    }

    private void givenExistingSharedPolicyGroupWithLegacyId() {
        sharedPolicyGroupCrudService.initWith(
            List.of(
                SharedPolicyGroup.builder()
                    .id(LEGACY_ID)
                    .crossId("legacy-cross-id")
                    .hrid(LEGACY_ID)
                    .environmentId(ENVIRONMENT)
                    .organizationId(ORGANIZATION)
                    .build()
            )
        );
    }

    @Nested
    class Get {

        @BeforeEach
        void setUp() {
            givenExistingSharedPolicyGroup();
        }

        @Test
        void should_get_shared_policy_group_from_known_hrid() {
            try (var response = rootTarget().path(HRID).request().get()) {
                var state = response.readEntity(SharedPolicyGroupState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isNotBlank();
                    soft.assertThat(state.getCrossId()).isNotBlank();
                    soft.assertThat(state.getHrid()).isEqualTo(HRID);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getSteps()).isNotEmpty();
                });
            }
        }

        @Test
        void should_get_shared_policy_group_from_known_legacy_id() {
            sharedPolicyGroupCrudService.reset();
            givenExistingSharedPolicyGroupWithLegacyId();

            try (var response = rootTarget().path(LEGACY_ID).queryParam("legacyID", true).request().get()) {
                var state = response.readEntity(SharedPolicyGroupState.class);
                assertThat(state.getId()).isEqualTo(LEGACY_ID);
            }
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            try (var response = rootTarget().path("unknown").request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
            assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
        }
    }

    @Nested
    class Delete {

        @BeforeEach
        void setUp() {
            givenExistingSharedPolicyGroup(List.of());
        }

        @Nested
        class Run {

            @Test
            void should_delete_shared_policy_group_and_return_no_content() {
                assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
                try (var response = rootTarget().path(HRID).request().delete()) {
                    assertThat(response.getStatus()).isEqualTo(204);
                }
                assertThat(sharedPolicyGroupCrudService.storage()).isEmpty();
            }

            @Test
            void should_delete_shared_policy_group_with_legacy_id() {
                sharedPolicyGroupCrudService.reset();
                givenExistingSharedPolicyGroupWithLegacyId();

                assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
                try (var response = rootTarget().path(LEGACY_ID).queryParam("legacyID", true).request().delete()) {
                    assertThat(response.getStatus()).isEqualTo(204);
                }
                assertThat(sharedPolicyGroupCrudService.storage()).isEmpty();
            }

            @Test
            void should_return_a_404_status_code_with_unknown_hrid() {
                try (var response = rootTarget().path("unknown").request().delete()) {
                    assertThat(response.getStatus()).isEqualTo(404);
                }
                assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
            }
        }

        @Nested
        class DryRun {

            @Test
            void should_not_delete_shared_policy_group_and_return_no_content() {
                assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
                try (var response = rootTarget().path(HRID).queryParam("dryRun", true).request().delete()) {
                    assertThat(response.getStatus()).isEqualTo(204);
                }
                assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
            }

            @Test
            void should_return_a_404_status_code_with_unknown_hrid() {
                try (var response = rootTarget().path("unknown").queryParam("dryRun", true).request().delete()) {
                    assertThat(response.getStatus()).isEqualTo(404);
                }
                assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
            }
        }
    }

    @Nested
    class Put {

        @Nested
        class Run {

            @BeforeEach
            void setUp() {
                when(createSharedPolicyGroupUseCase.execute(any(CreateSharedPolicyGroupUseCase.Input.class))).thenAnswer(invocation -> {
                    CreateSharedPolicyGroupUseCase.Input argument = invocation.getArgument(0);
                    var spgToCreate = argument.sharedPolicyGroupToCreate();
                    return new CreateSharedPolicyGroupUseCase.Output(
                        SharedPolicyGroup.builder()
                            .id(spgToCreate.getId())
                            .crossId(spgToCreate.getCrossId())
                            .hrid(spgToCreate.getHrid())
                            .environmentId(ENVIRONMENT)
                            .build()
                    );
                });
            }

            @Test
            void should_return_state_from_hrid() {
                var state = putAndReadEntity("shared-policy-group.json", false);
                var spg = HRIDToUUID.sharedPolicyGroup().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid(HRID);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getCrossId()).isEqualTo(spg.crossId());
                    soft.assertThat(state.getId()).isEqualTo(spg.id());
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getHrid()).isEqualTo("spg-foo");
                });
            }

            @Test
            void should_return_state_from_cross_id() {
                var state = putAndReadEntity("shared-policy-group-with-cross-id-and-hrid.json", false);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo("spg-foo");
                    soft.assertThat(state.getCrossId()).isEqualTo("spg-cross-id");
                    soft.assertThat(state.getId()).isNotBlank();
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }

            @Test
            void should_reject_with_cross_id_and_no_hrid() {
                try (
                    var response = rootTarget()
                        .queryParam("dryRun", false)
                        .request()
                        .put(Entity.json(readJSON("shared-policy-group-with-cross-id-and-no-hrid.json")))
                ) {
                    assertThat(response.getStatus()).isEqualTo(400);
                }
            }
        }

        @Nested
        class DryRun {

            @Test
            void should_return_state_from_hrid() {
                var state = putAndReadEntity("shared-policy-group.json", true);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getCrossId()).isNotBlank();
                    soft.assertThat(state.getId()).isNotBlank();
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getHrid()).isEqualTo("spg-foo");
                });
            }

            @Test
            void should_return_state_from_cross_id_no_hrid() {
                try (
                    var response = rootTarget()
                        .queryParam("dryRun", true)
                        .request()
                        .put(Entity.json(readJSON("shared-policy-group-with-cross-id-and-no-hrid.json")))
                ) {
                    assertThat(response.getStatus()).isEqualTo(400);
                }
            }
        }

        private SharedPolicyGroupState putAndReadEntity(String spec, boolean dryRun) {
            try (var response = rootTarget().queryParam("dryRun", dryRun).request().put(Entity.json(readJSON(spec)))) {
                return response.readEntity(SharedPolicyGroupState.class);
            }
        }
    }
}
