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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.group.model.crd.GroupCRDStatus;
import io.gravitee.apim.core.group.use_case.ImportGroupCRDUseCase;
import io.gravitee.apim.core.group.use_case.ValidateGroupCRDUseCase;
import io.gravitee.apim.rest.api.automation.model.GroupState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GroupsResourceTest extends AbstractResourceTest {

    @Inject
    private ImportGroupCRDUseCase importGroupCRDUseCase;

    @Inject
    private ValidateGroupCRDUseCase validateGroupCRDUseCase;

    @AfterEach
    void tearDown() {
        reset(importGroupCRDUseCase);
        reset(validateGroupCRDUseCase);
    }

    @Nested
    class Run {

        @BeforeEach
        void setUp() {
            when(importGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id("group-id").members(0).build())
            );
        }

        @Test
        void should_return_state_from_name() {
            var state = expectEntity("group-with-name.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getStatus().getId()).isEqualTo("group-id");
                soft.assertThat(state.getSpec().getName()).isEqualTo("my-group");
                soft.assertThat(state.getSpec().getNotifyMembers()).isTrue();
            });
        }

        @Test
        void should_return_state_with_members() {
            when(importGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id("group-id").members(1).build())
            );

            var state = expectEntity("group-with-members.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getStatus().getId()).isEqualTo("group-id");
                soft.assertThat(state.getStatus().getMembers()).isEqualTo(1L);
                soft.assertThat(state.getSpec().getMembers()).hasSize(1);
            });
        }

        @Test
        void should_use_name_as_id_when_hrid_contains_uuid() {
            when(importGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenAnswer(call -> {
                ImportGroupCRDUseCase.Input input = call.getArgument(0, ImportGroupCRDUseCase.Input.class);
                return new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id(input.spec().getId()).members(0).build());
            });

            var state = expectEntity("group-with-name.json", false, true);
            assertThat(state.getStatus().getId()).isEqualTo("my-group");
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_return_state_from_validation() {
            when(validateGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id("generated-id").members(0).build())
            );

            var state = expectEntity("group-with-name.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getStatus().getId()).isEqualTo("generated-id");
                soft.assertThat(state.getSpec().getName()).isEqualTo("my-group");
            });
        }

        @Test
        void should_return_state_with_errors() {
            when(validateGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(
                    GroupCRDStatus.builder()
                        .id("generated-id")
                        .members(0)
                        .errors(new GroupCRDStatus.Errors(java.util.List.of(), java.util.List.of("unknown role")))
                        .build()
                )
            );

            var state = expectEntity("group-with-name.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getStatus().getId()).isEqualTo("generated-id");
                soft.assertThat(state.getStatus().getErrors()).isNotNull();
                soft.assertThat(state.getStatus().getErrors().getWarning()).contains("unknown role");
            });
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/groups";
    }

    private GroupState expectEntity(String spec) {
        return expectEntity(spec, false, false);
    }

    private GroupState expectEntity(String spec, boolean dryRun) {
        return expectEntity(spec, dryRun, false);
    }

    private GroupState expectEntity(String spec, boolean dryRun, boolean hridContainsUUID) {
        try (
            var response = rootTarget()
                .queryParam("dryRun", dryRun)
                .queryParam("hridContainsUUID", hridContainsUUID)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(GroupState.class);
        }
    }
}
