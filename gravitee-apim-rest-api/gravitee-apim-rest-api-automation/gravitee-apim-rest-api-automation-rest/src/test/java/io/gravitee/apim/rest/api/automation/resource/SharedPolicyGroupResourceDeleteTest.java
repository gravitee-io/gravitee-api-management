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

import static io.gravitee.apim.rest.api.automation.resource.SharedPolicyGroupResourceGetTest.HRID;
import static org.assertj.core.api.Assertions.assertThat;

import inmemory.SharedPolicyGroupCrudServiceInMemory;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.use_case.DeleteSharedPolicyGroupUseCase;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class SharedPolicyGroupResourceDeleteTest extends AbstractResourceTest {

    @Inject
    SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService;

    @Inject
    DeleteSharedPolicyGroupUseCase deleteSharedPolicyGroupUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/shared-policy-groups";
    }

    @BeforeEach
    void setUp() {
        IdBuilder builder = IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), HRID);
        sharedPolicyGroupCrudService.initWith(
            List.of(
                SharedPolicyGroup.builder()
                    .id(builder.buildId())
                    .crossId(builder.buildCrossId())
                    .hrid(HRID)
                    .environmentId(ENVIRONMENT)
                    .organizationId(ORGANIZATION)
                    .build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        sharedPolicyGroupCrudService.reset();
    }

    @Nested
    class Run {

        @Test
        void should_delete_shared_policy_group_and_return_no_content() {
            assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
            expectNoContent(HRID);
            assertThat(sharedPolicyGroupCrudService.storage()).isEmpty();
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            expectNotFound("unknown");
            assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_not_delete_shared_policy_group_and_return_no_content() {
            assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
            expectNoContent(HRID, dryRun);
            assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            expectNotFound("unknown", dryRun);
            assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
        }
    }

    private void expectNotFound(String hrid) {
        expectNotFound(hrid, false);
    }

    private void expectNotFound(String hrid, boolean dryRun) {
        try (var response = rootTarget().path(hrid).queryParam("dryRun", dryRun).request().delete()) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    private void expectNoContent(String hrid) {
        expectNoContent(hrid, false);
    }

    private void expectNoContent(String hrid, boolean dryRun) {
        try (var response = rootTarget().path(hrid).queryParam("dryRun", dryRun).request().delete()) {
            assertThat(response.getStatus()).isEqualTo(204);
        }
    }
}
