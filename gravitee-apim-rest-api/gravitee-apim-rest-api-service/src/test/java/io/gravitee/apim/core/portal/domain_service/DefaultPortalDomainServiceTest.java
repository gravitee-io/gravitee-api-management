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
package io.gravitee.apim.core.portal.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.InstallationAccessQueryServiceInMemory;
import inmemory.PortalCrudServiceInMemory;
import io.gravitee.apim.core.portal.exception.DefaultPortalNotFoundException;
import io.gravitee.apim.core.portal.exception.SeveralPortalsInEnvironmentException;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultPortalDomainServiceTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final String OTHER_ENV_ID = "env-2";
    private static final PortalId DEFAULT_PORTAL_ID = PortalId.of("11111111-1111-1111-1111-111111111111");
    private static final PortalId AUTOMATION_PORTAL_ID = PortalId.of("22222222-2222-2222-2222-222222222222");
    private static final URL STALE_URL = url("https://example.com/to-be-implemented");

    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private DefaultPortalDomainService cut;

    @BeforeEach
    void setUp() {
        portalCrudService.reset();
        installationAccessQueryService.reset();
        cut = new DefaultPortalDomainService(portalCrudService, new PortalUrlDomainService(installationAccessQueryService));
    }

    @SneakyThrows
    private static URL url(String value) {
        return URI.create(value).toURL();
    }

    private static Portal aPortal(PortalId id, String environmentId, String name, boolean defaultPortal, URL url) {
        return Portal.of(id, environmentId, ORG_ID, name, PortalNavigationStructure.empty(), null, defaultPortal, url);
    }

    private static Portal aDefaultPortal(URL url) {
        return aPortal(DEFAULT_PORTAL_ID, ENV_ID, "Developer Portal", true, url);
    }

    private static Portal anAutomationPortal(URL url) {
        return aPortal(AUTOMATION_PORTAL_ID, ENV_ID, "Automation Portal", false, url);
    }

    /** A comparable snapshot of every stored field, since Portal equality is by id only. */
    private List<String> storedState() {
        return portalCrudService
            .storage()
            .stream()
            .map(p ->
                String.join(
                    "|",
                    p.getId().toString(),
                    p.getEnvironmentId(),
                    p.getOrganizationId(),
                    p.getName(),
                    String.valueOf(p.isDefault()),
                    p.getUrl().toString()
                )
            )
            .toList();
    }

    @Nested
    class EnsureDefault {

        @Test
        void should_create_default_portal_with_resolved_url_when_environment_has_none() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(portalCrudService.storage())
                .singleElement()
                .satisfies(portal -> {
                    assertThat(portal.getName()).isEqualTo("Developer Portal");
                    assertThat(portal.isDefault()).isTrue();
                    assertThat(portal.getEnvironmentId()).isEqualTo(ENV_ID);
                    assertThat(portal.getOrganizationId()).isEqualTo(ORG_ID);
                    assertThat(portal.getUrl()).hasToString("https://portal.acme.io/next");
                });
            assertThat(result).hasValueSatisfying(portal ->
                assertThat(portal.getId()).isEqualTo(portalCrudService.storage().getFirst().getId())
            );
        }

        @Test
        void should_postpone_the_default_portal_while_its_url_cannot_be_resolved() {
            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).isEmpty();
            assertThat(portalCrudService.storage()).isEmpty();
        }

        @Test
        void should_create_the_postponed_default_portal_once_its_url_can_be_resolved() {
            cut.ensureDefault(ORG_ID, ENV_ID);
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).hasValueSatisfying(portal -> assertThat(portal.getUrl()).hasToString("https://portal.acme.io/next"));
            assertThat(portalCrudService.storage()).hasSize(1);
        }

        @Test
        void should_not_create_nor_mark_default_when_environment_has_only_an_automation_portal() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(anAutomationPortal(STALE_URL)));
            var before = storedState();

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).isEmpty();
            assertThat(storedState()).isEqualTo(before);
        }

        @Test
        void should_return_existing_default_portal_unchanged() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://other.acme.io");
            portalCrudService.initWith(List.of(aDefaultPortal(url("https://portal.acme.io/next"))));
            var before = storedState();

            var first = cut.ensureDefault(ORG_ID, ENV_ID);
            var second = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(first).hasValueSatisfying(portal -> assertThat(portal.getId()).isEqualTo(DEFAULT_PORTAL_ID));
            assertThat(second).hasValueSatisfying(portal -> assertThat(portal.getId()).isEqualTo(DEFAULT_PORTAL_ID));
            assertThat(storedState()).isEqualTo(before);
        }

        @Test
        void should_create_a_single_default_portal_when_called_twice() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            cut.ensureDefault(ORG_ID, ENV_ID);
            cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(portalCrudService.storage()).hasSize(1);
        }

        @Test
        void should_only_look_at_the_given_environment() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(aPortal(AUTOMATION_PORTAL_ID, OTHER_ENV_ID, "Other", false, STALE_URL)));

            var result = cut.ensureDefault(ORG_ID, ENV_ID);

            assertThat(result).hasValueSatisfying(portal -> assertThat(portal.getEnvironmentId()).isEqualTo(ENV_ID));
            assertThat(portalCrudService.storage()).hasSize(2);
        }

        @Test
        void should_fail_when_environment_has_several_portals() {
            portalCrudService.initWith(List.of(aDefaultPortal(STALE_URL), anAutomationPortal(STALE_URL)));
            var before = storedState();

            assertThatThrownBy(() -> cut.ensureDefault(ORG_ID, ENV_ID))
                .isInstanceOf(SeveralPortalsInEnvironmentException.class)
                .hasMessageContaining(ENV_ID)
                .hasMessageContaining(DEFAULT_PORTAL_ID.toString())
                .hasMessageContaining("Developer Portal")
                .hasMessageContaining(AUTOMATION_PORTAL_ID.toString())
                .hasMessageContaining("Automation Portal");
            assertThat(storedState()).isEqualTo(before);
        }
    }

    @Nested
    class CheckSinglePortal {

        @Test
        void should_accept_an_environment_with_one_portal_or_none() {
            portalCrudService.initWith(List.of(anAutomationPortal(STALE_URL)));

            cut.checkSinglePortal(ENV_ID);
            cut.checkSinglePortal(OTHER_ENV_ID);
        }

        @Test
        void should_name_the_environment_its_portals_and_the_fix_when_it_has_several() {
            portalCrudService.initWith(List.of(aDefaultPortal(STALE_URL), anAutomationPortal(STALE_URL)));

            assertThatThrownBy(() -> cut.checkSinglePortal(ENV_ID))
                .isInstanceOf(SeveralPortalsInEnvironmentException.class)
                .hasMessageContaining("Environment [env-1] has 2 portals")
                .hasMessageContaining("[11111111-1111-1111-1111-111111111111] \"Developer Portal\"")
                .hasMessageContaining("[22222222-2222-2222-2222-222222222222] \"Automation Portal\"")
                .hasMessageContaining("remove");
        }
    }

    @Nested
    class GetDefault {

        @Test
        void should_return_default_portal() {
            portalCrudService.initWith(List.of(aDefaultPortal(STALE_URL)));

            assertThat(cut.getDefault(ENV_ID).getId()).isEqualTo(DEFAULT_PORTAL_ID);
        }

        @Test
        void should_throw_default_portal_not_found_when_none() {
            portalCrudService.initWith(List.of(anAutomationPortal(STALE_URL)));

            assertThatThrownBy(() -> cut.getDefault(ENV_ID))
                .isInstanceOf(DefaultPortalNotFoundException.class)
                .hasMessageContaining(ENV_ID);
        }
    }

    @Nested
    class SyncUrl {

        @Test
        void should_update_default_portal_url_when_resolved_url_changed() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://new.acme.io");
            portalCrudService.initWith(List.of(aDefaultPortal(url("https://old.acme.io/next"))));

            cut.syncUrl(ENV_ID);

            assertThat(portalCrudService.storage())
                .singleElement()
                .satisfies(portal -> assertThat(portal.getUrl()).hasToString("https://new.acme.io/next"));
        }

        @Test
        void should_replace_the_placeholder_once_the_url_can_be_resolved() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(aDefaultPortal(STALE_URL)));

            cut.syncUrl(ENV_ID);

            assertThat(portalCrudService.storage())
                .singleElement()
                .satisfies(portal -> assertThat(portal.getUrl()).hasToString("https://portal.acme.io/next"));
        }

        @Test
        void should_not_update_when_url_unchanged() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            var stored = new AtomicInteger();
            var countingCrudService = new PortalCrudServiceInMemory() {
                @Override
                public Portal update(Portal portal) {
                    stored.incrementAndGet();
                    return super.update(portal);
                }
            };
            countingCrudService.initWith(List.of(aDefaultPortal(url("https://portal.acme.io/next"))));
            var service = new DefaultPortalDomainService(countingCrudService, new PortalUrlDomainService(installationAccessQueryService));

            service.syncUrl(ENV_ID);

            assertThat(stored).hasValue(0);
        }

        @Test
        void should_not_update_when_url_cannot_be_resolved() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "localhost:4100");
            portalCrudService.initWith(List.of(aDefaultPortal(url("https://portal.acme.io/next"))));

            cut.syncUrl(ENV_ID);

            assertThat(portalCrudService.storage())
                .singleElement()
                .satisfies(portal -> assertThat(portal.getUrl()).hasToString("https://portal.acme.io/next"));
        }

        @Test
        void should_do_nothing_when_environment_has_no_default_portal() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(anAutomationPortal(STALE_URL)));
            var before = storedState();

            cut.syncUrl(ENV_ID);

            assertThat(storedState()).isEqualTo(before);
        }
    }

    @Nested
    class ReplacePlaceholderUrls {

        @Test
        void should_replace_the_placeholder_on_every_portal_of_the_environment() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(
                List.of(anAutomationPortal(STALE_URL), aPortal(DEFAULT_PORTAL_ID, OTHER_ENV_ID, "Other", true, STALE_URL))
            );

            var replaced = cut.replaceUrl(ENV_ID, STALE_URL);

            assertThat(replaced).isEqualTo(1);
            assertThat(portalCrudService.findByIdAndEnvironmentId(AUTOMATION_PORTAL_ID, ENV_ID)).hasValueSatisfying(portal ->
                assertThat(portal.getUrl()).hasToString("https://portal.acme.io/next")
            );
            assertThat(portalCrudService.findByIdAndEnvironmentId(DEFAULT_PORTAL_ID, OTHER_ENV_ID)).hasValueSatisfying(portal ->
                assertThat(portal.getUrl()).hasToString(STALE_URL.toString())
            );
        }

        @Test
        void should_keep_a_url_that_is_not_the_placeholder() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");
            portalCrudService.initWith(List.of(aDefaultPortal(url("https://custom.acme.io/next"))));

            var replaced = cut.replaceUrl(ENV_ID, STALE_URL);

            assertThat(replaced).isZero();
            assertThat(portalCrudService.storage())
                .singleElement()
                .satisfies(portal -> assertThat(portal.getUrl()).hasToString("https://custom.acme.io/next"));
        }

        @Test
        void should_keep_the_placeholder_when_url_cannot_be_resolved() {
            portalCrudService.initWith(List.of(aDefaultPortal(STALE_URL)));

            var replaced = cut.replaceUrl(ENV_ID, STALE_URL);

            assertThat(replaced).isZero();
            assertThat(portalCrudService.storage())
                .singleElement()
                .satisfies(portal -> assertThat(portal.getUrl()).hasToString(STALE_URL.toString()));
        }
    }
}
