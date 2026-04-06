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
package io.gravitee.rest.api.service.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.audit.model.AuditInfo;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HRIDToUUIDTest {

    private static final AuditInfo AUDIT = AuditInfo.builder().organizationId("org").environmentId("env").build();
    private static final ExecutionContext EXEC_CTX = new ExecutionContext("org", "env");

    @Nested
    class Api {

        @Test
        void should_generate_same_id_for_same_hrid() {
            assertThat(HRIDToUUID.api().context(AUDIT).hrid("foo").id()).isEqualTo(HRIDToUUID.api().context(AUDIT).hrid("foo").id());
        }

        @Test
        void should_generate_same_cross_id_for_same_hrid() {
            assertThat(HRIDToUUID.api().context(AUDIT).hrid("foo").crossId()).isEqualTo(
                HRIDToUUID.api().context(AUDIT).hrid("foo").crossId()
            );
        }

        @Test
        void should_generate_different_id_for_different_hrid() {
            assertThat(HRIDToUUID.api().context(AUDIT).hrid("foo").id()).isNotEqualTo(HRIDToUUID.api().context(AUDIT).hrid("bar").id());
        }

        @Test
        void should_produce_same_result_from_audit_info_and_execution_context() {
            assertThat(HRIDToUUID.api().context(AUDIT).hrid("foo").id()).isEqualTo(HRIDToUUID.api().context(EXEC_CTX).hrid("foo").id());
            assertThat(HRIDToUUID.api().context(AUDIT).hrid("foo").crossId()).isEqualTo(
                HRIDToUUID.api().context(EXEC_CTX).hrid("foo").crossId()
            );
        }
    }

    @Nested
    class Application {

        @Test
        void should_generate_same_id_for_same_hrid() {
            assertThat(HRIDToUUID.application().context(AUDIT).hrid("app").id()).isEqualTo(
                HRIDToUUID.application().context(AUDIT).hrid("app").id()
            );
        }

        @Test
        void should_produce_same_formula_as_api_for_same_hrid() {
            assertThat(HRIDToUUID.application().context(AUDIT).hrid("x").id()).isEqualTo(HRIDToUUID.api().context(AUDIT).hrid("x").id());
        }
    }

    @Nested
    class SharedPolicyGroup {

        @Test
        void should_generate_same_id_and_cross_id_for_same_hrid() {
            assertThat(HRIDToUUID.sharedPolicyGroup().context(AUDIT).hrid("spg").id()).isEqualTo(
                HRIDToUUID.sharedPolicyGroup().context(AUDIT).hrid("spg").id()
            );
            assertThat(HRIDToUUID.sharedPolicyGroup().context(AUDIT).hrid("spg").crossId()).isEqualTo(
                HRIDToUUID.sharedPolicyGroup().context(AUDIT).hrid("spg").crossId()
            );
        }
    }

    @Nested
    class Plan {

        @Test
        void should_generate_same_id_for_same_inputs() {
            assertThat(HRIDToUUID.plan().context(AUDIT).api("my-api").plan("my-plan").id()).isEqualTo(
                HRIDToUUID.plan().context(AUDIT).api("my-api").plan("my-plan").id()
            );
        }

        @Test
        void should_generate_different_id_for_different_plan_hrid() {
            assertThat(HRIDToUUID.plan().context(AUDIT).api("my-api").plan("plan-a").id()).isNotEqualTo(
                HRIDToUUID.plan().context(AUDIT).api("my-api").plan("plan-b").id()
            );
        }

        @Test
        void should_generate_different_id_for_different_api_hrid() {
            assertThat(HRIDToUUID.plan().context(AUDIT).api("api-a").plan("plan").id()).isNotEqualTo(
                HRIDToUUID.plan().context(AUDIT).api("api-b").plan("plan").id()
            );
        }
    }

    @Nested
    class Page {

        @Test
        void should_generate_same_id_for_same_inputs() {
            assertThat(HRIDToUUID.page().context(AUDIT).api("my-api").page("my-page").id()).isEqualTo(
                HRIDToUUID.page().context(AUDIT).api("my-api").page("my-page").id()
            );
        }

        @Test
        void should_generate_different_id_for_different_page_hrid() {
            assertThat(HRIDToUUID.page().context(AUDIT).api("my-api").page("page-a").id()).isNotEqualTo(
                HRIDToUUID.page().context(AUDIT).api("my-api").page("page-b").id()
            );
        }
    }

    @Nested
    class Subscription {

        @Test
        void should_generate_same_id_for_same_inputs() {
            assertThat(HRIDToUUID.subscription().context(AUDIT).api("my-api").subscription("my-sub").id()).isEqualTo(
                HRIDToUUID.subscription().context(AUDIT).api("my-api").subscription("my-sub").id()
            );
        }

        @Test
        void should_generate_different_id_for_different_subscription_hrid() {
            assertThat(HRIDToUUID.subscription().context(AUDIT).api("my-api").subscription("sub-a").id()).isNotEqualTo(
                HRIDToUUID.subscription().context(AUDIT).api("my-api").subscription("sub-b").id()
            );
        }

        @Test
        void should_produce_same_result_from_audit_info_and_execution_context() {
            assertThat(HRIDToUUID.subscription().context(AUDIT).api("api").subscription("sub").id()).isEqualTo(
                HRIDToUUID.subscription().context(EXEC_CTX).api("api").subscription("sub").id()
            );
        }
    }

    @Nested
    class CrossResourceConsistency {

        @Test
        void sub_resource_id_should_differ_from_top_level_id() {
            String apiId = HRIDToUUID.api().context(AUDIT).hrid("my-api").id();
            String planId = HRIDToUUID.plan().context(AUDIT).api("my-api").plan("my-plan").id();
            String pageId = HRIDToUUID.page().context(AUDIT).api("my-api").page("my-plan").id();
            assertThat(apiId).isNotEqualTo(planId).isNotEqualTo(pageId);
        }

        @Test
        void different_sub_resource_types_with_same_hrid_should_produce_same_id() {
            // plan, page, subscription share the same formula
            // this is not a design constraint, just a reminder
            // that the formula is the same for all sub-resources
            // if this is not passing anymore, it raises questions:
            // Is this what we want? or is it a bug?
            String planId = HRIDToUUID.plan().context(AUDIT).api("api").plan("x").id();
            String pageId = HRIDToUUID.page().context(AUDIT).api("api").page("x").id();
            String subId = HRIDToUUID.subscription().context(AUDIT).api("api").subscription("x").id();
            assertThat(planId).isEqualTo(pageId).isEqualTo(subId);
        }
    }
}
