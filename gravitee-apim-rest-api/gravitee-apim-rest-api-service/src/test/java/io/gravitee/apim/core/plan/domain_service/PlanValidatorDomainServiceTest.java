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
package io.gravitee.apim.core.plan.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import inmemory.PageCrudServiceInMemory;
import inmemory.ParametersQueryServiceInMemory;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.policy.domain_service.PolicyValidationDomainService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanValidatorDomainServiceTest {

    private PlanValidatorDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new PlanValidatorDomainService(
            new ParametersQueryServiceInMemory(),
            mock(PolicyValidationDomainService.class),
            new PageCrudServiceInMemory()
        );
    }

    @Test
    void should_accept_empty_plan_tags_for_tagless_api_product() {
        assertThatCode(() -> cut.validatePlanTagsAgainstApiProductTags(Set.of(), null)).doesNotThrowAnyException();
        assertThatCode(() -> cut.validatePlanTagsAgainstApiProductTags(Set.of(), Set.of())).doesNotThrowAnyException();
    }

    @Test
    void should_accept_plan_tags_that_are_subset_of_api_product_tags() {
        assertThatCode(() ->
            cut.validatePlanTagsAgainstApiProductTags(Set.of("internal"), Set.of("internal", "external"))
        ).doesNotThrowAnyException();
    }

    @Test
    void should_reject_plan_tags_when_api_product_has_no_tags() {
        assertThatThrownBy(() -> cut.validatePlanTagsAgainstApiProductTags(Set.of("internal"), null))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Plan tags mismatch the tags defined by the API Product");

        assertThatThrownBy(() -> cut.validatePlanTagsAgainstApiProductTags(Set.of("internal"), Set.of()))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Plan tags mismatch the tags defined by the API Product");
    }

    @Test
    void should_reject_plan_tags_not_defined_on_api_product() {
        assertThatThrownBy(() -> cut.validatePlanTagsAgainstApiProductTags(Set.of("internal"), Set.of("external")))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Plan tags mismatch the tags defined by the API Product");
    }
}
