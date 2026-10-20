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
package io.gravitee.apim.core.portal_page.domain_service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ReferencedApiExistsRuleTest {

    private static final String ENV_ID = "env-1";
    private static final String API_ID = "api-1";

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ReferencedApiExistsRule rule = new ReferencedApiExistsRule(apiCrudService);

    @BeforeEach
    void setUp() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().id(API_ID).environmentId(ENV_ID).build()));
    }

    @ParameterizedTest
    @EnumSource(value = PortalNavigationItemType.class, names = { "PAGE", "FOLDER", "LINK" })
    void applies_to_an_item_owned_by_an_api(PortalNavigationItemType type) {
        assertThat(rule.appliesTo(anItem(type, ownedBy(API_ID)))).isTrue();
    }

    @Test
    void does_not_apply_to_an_item_owned_by_the_portal() {
        assertThat(rule.appliesTo(anItem(PortalNavigationItemType.PAGE, NavigationItemReference.defaultReference()))).isFalse();
    }

    @Test
    void does_not_apply_to_an_item_managed_by_automation() {
        var managed = anItem(PortalNavigationItemType.PAGE, ownedBy("api-not-created-yet"))
            .toBuilder()
            .automationMetadata(
                new AutomationMetadata(
                    AutomationMetadata.ReferenceType.API,
                    "api-not-created-yet",
                    "doc",
                    Optional.empty(),
                    Optional.empty()
                )
            )
            .build();

        assertThat(rule.appliesTo(managed)).isFalse();
    }

    @Test
    void accepts_an_api_of_the_environment() {
        assertThatCode(() ->
            rule.validate(anItem(PortalNavigationItemType.PAGE, ownedBy(API_ID)), ENV_ID, null)
        ).doesNotThrowAnyException();
    }

    @Test
    void rejects_an_api_that_does_not_exist() {
        assertThatThrownBy(() -> rule.validate(anItem(PortalNavigationItemType.PAGE, ownedBy("unknown-api")), ENV_ID, null)).isInstanceOf(
            ApiNotFoundException.class
        );
    }

    @Test
    void rejects_an_api_of_another_environment() {
        assertThatThrownBy(() -> rule.validate(anItem(PortalNavigationItemType.PAGE, ownedBy(API_ID)), "other-env", null)).isInstanceOf(
            ApiNotFoundException.class
        );
    }

    private static CreatePortalNavigationItem anItem(PortalNavigationItemType type, NavigationItemReference reference) {
        return CreatePortalNavigationItem.builder()
            .type(type)
            .title("Doc")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
            .reference(reference)
            .build();
    }

    private static NavigationItemReference ownedBy(String apiId) {
        return new NavigationItemReference.ApiReference(apiId);
    }
}
