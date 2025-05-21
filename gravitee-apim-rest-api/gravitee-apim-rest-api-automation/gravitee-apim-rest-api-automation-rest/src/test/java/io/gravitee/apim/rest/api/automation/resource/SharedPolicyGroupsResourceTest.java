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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.shared_policy_group.domain_service.ValidateSharedPolicyGroupCRDDomainService;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroupCRDStatus;
import io.gravitee.apim.core.shared_policy_group.use_case.CreateSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.DeploySharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.ImportSharedPolicyGroupCRDCRDUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.UpdateSharedPolicyGroupUseCase;
import io.gravitee.apim.rest.api.automation.model.SharedPolicyGroupState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SharedPolicyGroupsResourceTest extends AbstractResourceTest {

    @Inject
    private CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase;

    @Inject
    private ImportSharedPolicyGroupCRDCRDUseCase importSharedPolicyGroupCRDCRDUseCase;

    @Inject
    private ValidateSharedPolicyGroupCRDDomainService validateSharedPolicyGroupCRDDomainService;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/shared-policy-groups";
    }

    @AfterEach
    void tearDown() {
        reset(createSharedPolicyGroupUseCase);
    }

    @Nested
    class Run {

        @BeforeEach
        void setUp() {
            when(createSharedPolicyGroupUseCase.execute(any(CreateSharedPolicyGroupUseCase.Input.class)))
                .thenReturn(
                    new CreateSharedPolicyGroupUseCase.Output(
                        SharedPolicyGroup.builder().id("spg-id").crossId("spg-cross-id").environmentId(ENVIRONMENT).build()
                    )
                );
        }

        @Test
        void should_return_state_from_hrid() {
            var state = expectEntity("shared-policy-group.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("spg-cross-id");
                soft.assertThat(state.getId()).isEqualTo("spg-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("spg-foo");
            });
        }

        @Test
        void should_return_state_from_cross_id() {
            var state = expectEntity("shared-policy-group-with-cross-id-and-no-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("spg-cross-id");
                soft.assertThat(state.getId()).isEqualTo("spg-id");
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isNull();
            });
        }

        @Test
        void should_reject_with_no_cross_id_nor_hrid() {
            expectBadRequest("shared-policy-group-with-no-cross-id-nor-hrid.json");
        }

        @Test
        void should_reject_with_cross_id_and_hrid() {
            expectBadRequest("shared-policy-group-with-cross-id-and-hrid.json");
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_return_state_from_hrid() {
            var state = expectEntity("shared-policy-group.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isNull();
                soft.assertThat(state.getId()).isNull();
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("spg-foo");
            });
        }

        @Test
        void should_return_state_from_cross_id() {
            var state = expectEntity("shared-policy-group-with-cross-id-and-no-hrid.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo("spg-foo");
                soft.assertThat(state.getId()).isNull();
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_reject_with_no_cross_id_nor_hrid() {
            var state = expectEntity("shared-policy-group-with-no-cross-id-nor-hrid.json", dryRun);
            var errors = state.getErrors();
            assertThat(errors).isNotNull();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(errors.getSevere()).isNotNull();
                soft
                    .assertThat(errors.getSevere())
                    .contains("when no hrid is set in the payload a cross ID should be passed to identify the resource");
            });
        }

        @Test
        void should_reject_with_cross_id_and_hrid() {
            var state = expectEntity("shared-policy-group-with-cross-id-and-hrid.json", dryRun);
            var errors = state.getErrors();
            assertThat(errors).isNotNull();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(errors.getSevere()).isNotNull();
                soft
                    .assertThat(errors.getSevere())
                    .contains("cross ID should only be passed to identify the resource if no hrid has been set");
            });
        }
    }

    private void expectBadRequest(String spec) {
        try (var response = rootTarget().queryParam("dryRun", false).request().put(Entity.json(readJSON(spec)))) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    private SharedPolicyGroupState expectEntity(String spec) {
        return expectEntity(spec, false);
    }

    private SharedPolicyGroupState expectEntity(String spec, boolean dryRun) {
        try (var response = rootTarget().queryParam("dryRun", dryRun).request().put(Entity.json(readJSON(spec)))) {
            return response.readEntity(SharedPolicyGroupState.class);
        }
    }
}
