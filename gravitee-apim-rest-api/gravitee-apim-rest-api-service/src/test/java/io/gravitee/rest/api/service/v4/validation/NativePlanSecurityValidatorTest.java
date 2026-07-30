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
package io.gravitee.rest.api.service.v4.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.service.exceptions.NativePlanAuthenticationConflictException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NativePlanSecurityValidatorTest {

    private static Plan plan(String id, ApiType apiType, Plan.PlanSecurityType security, Plan.Status status) {
        return Plan.builder().id(id).apiType(apiType).security(security).status(status).build();
    }

    private static Plan nativePlanToPublish(Plan.PlanSecurityType security) {
        return plan("to-publish", ApiType.NATIVE, security, Plan.Status.STAGING);
    }

    @Test
    void should_skip_validation_for_non_native_api() {
        var proxyKeyless = plan("to-publish", ApiType.PROXY, Plan.PlanSecurityType.KEY_LESS, Plan.Status.STAGING);
        var existingApiKey = plan("existing", ApiType.PROXY, Plan.PlanSecurityType.API_KEY, Plan.Status.PUBLISHED);

        assertThatCode(() ->
            NativePlanSecurityValidator.validateNoConflictingSecurity(proxyKeyless, Set.of(proxyKeyless, existingApiKey))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_not_conflict_with_itself() {
        var keyless = plan("to-publish", ApiType.NATIVE, Plan.PlanSecurityType.KEY_LESS, Plan.Status.PUBLISHED);

        assertThatCode(() ->
            NativePlanSecurityValidator.validateNoConflictingSecurity(keyless, Set.of(keyless))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_reject_keyless_when_authentication_plan_published() {
        var toPublish = nativePlanToPublish(Plan.PlanSecurityType.KEY_LESS);
        var existing = plan("existing", ApiType.NATIVE, Plan.PlanSecurityType.API_KEY, Plan.Status.PUBLISHED);

        assertThatThrownBy(() ->
            NativePlanSecurityValidator.validateNoConflictingSecurity(toPublish, Set.of(toPublish, existing))
        ).isInstanceOf(NativePlanAuthenticationConflictException.class);
    }

    @Test
    void should_reject_keyless_when_authentication_plan_deprecated() {
        var toPublish = nativePlanToPublish(Plan.PlanSecurityType.KEY_LESS);
        var existing = plan("existing", ApiType.NATIVE, Plan.PlanSecurityType.API_KEY, Plan.Status.DEPRECATED);

        assertThatThrownBy(() ->
            NativePlanSecurityValidator.validateNoConflictingSecurity(toPublish, Set.of(toPublish, existing))
        ).isInstanceOf(NativePlanAuthenticationConflictException.class);
    }

    @Test
    void should_reject_mtls_when_authentication_plan_published() {
        var toPublish = nativePlanToPublish(Plan.PlanSecurityType.MTLS);
        var existing = plan("existing", ApiType.NATIVE, Plan.PlanSecurityType.OAUTH2, Plan.Status.PUBLISHED);

        assertThatThrownBy(() ->
            NativePlanSecurityValidator.validateNoConflictingSecurity(toPublish, Set.of(toPublish, existing))
        ).isInstanceOf(NativePlanAuthenticationConflictException.class);
    }

    @Test
    void should_reject_authentication_when_mtls_plan_published() {
        var toPublish = nativePlanToPublish(Plan.PlanSecurityType.JWT);
        var existing = plan("existing", ApiType.NATIVE, Plan.PlanSecurityType.MTLS, Plan.Status.PUBLISHED);

        assertThatThrownBy(() ->
            NativePlanSecurityValidator.validateNoConflictingSecurity(toPublish, Set.of(toPublish, existing))
        ).isInstanceOf(NativePlanAuthenticationConflictException.class);
    }

    @Test
    void should_allow_two_authentication_plans_of_different_types() {
        var toPublish = nativePlanToPublish(Plan.PlanSecurityType.API_KEY);
        var existing = plan("existing", ApiType.NATIVE, Plan.PlanSecurityType.JWT, Plan.Status.PUBLISHED);

        assertThatCode(() ->
            NativePlanSecurityValidator.validateNoConflictingSecurity(toPublish, Set.of(toPublish, existing))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_allow_keyless_when_conflicting_plan_is_closed() {
        var toPublish = nativePlanToPublish(Plan.PlanSecurityType.KEY_LESS);
        var existing = plan("existing", ApiType.NATIVE, Plan.PlanSecurityType.API_KEY, Plan.Status.CLOSED);

        assertThatCode(() ->
            NativePlanSecurityValidator.validateNoConflictingSecurity(toPublish, Set.of(toPublish, existing))
        ).doesNotThrowAnyException();
    }
}
