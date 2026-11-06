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

import inmemory.InstallationAccessQueryServiceInMemory;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal.model.PortalNavigationStructure;
import java.net.URI;
import java.net.URL;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalUrlDomainServiceTest {

    private static final String ENV_ID = "env-1";

    private final InstallationAccessQueryServiceInMemory installationAccessQueryService = new InstallationAccessQueryServiceInMemory();
    private PortalUrlDomainService cut;

    @BeforeEach
    void setUp() {
        installationAccessQueryService.reset();
        cut = new PortalUrlDomainService(installationAccessQueryService);
    }

    @Test
    void should_append_next_to_portal_url() {
        installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");

        assertThat(cut.resolveDefaultPortalUrl(ENV_ID)).hasValueSatisfying(url ->
            assertThat(url).hasToString("https://portal.acme.io/next")
        );
    }

    @Test
    void should_keep_the_portal_url_path_before_next() {
        installationAccessQueryService.withPortalUrl(ENV_ID, "http://localhost:4100/portal");

        assertThat(cut.resolveDefaultPortalUrl(ENV_ID)).hasValueSatisfying(url ->
            assertThat(url).hasToString("http://localhost:4100/portal/next")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://portal.acme.io/", "https://portal.acme.io//" })
    void should_strip_trailing_slash_before_next(String portalUrl) {
        installationAccessQueryService.withPortalUrl(ENV_ID, portalUrl);

        assertThat(cut.resolveDefaultPortalUrl(ENV_ID)).hasValueSatisfying(url ->
            assertThat(url).hasToString("https://portal.acme.io/next")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "  https://portal.acme.io  ", "HTTPS://portal.acme.io" })
    void should_accept_a_padded_url_or_an_upper_case_scheme(String portalUrl) {
        installationAccessQueryService.withPortalUrl(ENV_ID, portalUrl);

        assertThat(cut.resolveDefaultPortalUrl(ENV_ID)).hasValueSatisfying(url ->
            assertThat(url.toString()).endsWith("://portal.acme.io/next")
        );
    }

    @Test
    void should_return_empty_when_portal_url_carries_credentials() {
        installationAccessQueryService.withPortalUrl(ENV_ID, "https://user:secret@portal.acme.io");

        assertThat(cut.resolveDefaultPortalUrl(ENV_ID)).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    void should_return_empty_when_no_portal_url(String portalUrl) {
        installationAccessQueryService.withPortalUrl(ENV_ID, portalUrl);

        assertThat(cut.resolveDefaultPortalUrl(ENV_ID)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "localhost:4100", "ftp://x", "/portal", "https://", "https://portal acme.io" })
    void should_return_empty_when_portal_url_is_not_an_absolute_http_url(String portalUrl) {
        installationAccessQueryService.withPortalUrl(ENV_ID, portalUrl);

        assertThat(cut.resolveDefaultPortalUrl(ENV_ID)).isEmpty();
    }

    @Nested
    class ResolveUrl {

        private static final URL STORED_URL = url("https://stored.acme.io/next");

        @Test
        void should_return_the_portal_url_followed_by_next_for_the_default_portal() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io");

            assertThat(cut.resolveUrl(aPortal(true))).hasValueSatisfying(url ->
                assertThat(url).hasToString(installationAccessQueryService.getPortalUrl(ENV_ID) + "/next")
            );
        }

        @Test
        void should_strip_trailing_slashes_for_the_default_portal() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://portal.acme.io//");

            assertThat(cut.resolveUrl(aPortal(true))).hasValueSatisfying(url -> assertThat(url).hasToString("https://portal.acme.io/next"));
        }

        @Test
        void should_resolve_the_default_portal_url_instead_of_returning_its_stored_url() {
            installationAccessQueryService.withPortalUrl(ENV_ID, "https://new.acme.io");

            assertThat(cut.resolveUrl(aPortal(true))).hasValueSatisfying(url -> assertThat(url).hasToString("https://new.acme.io/next"));
        }

        @Test
        void should_return_empty_for_the_default_portal_when_no_portal_url_is_configured() {
            installationAccessQueryService.withPortalUrl(ENV_ID, null);

            assertThat(cut.resolveUrl(aPortal(true))).isEmpty();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = "https://portal.acme.io")
        void should_return_the_stored_url_for_a_portal_that_is_not_the_default_one(String environmentPortalUrl) {
            installationAccessQueryService.withPortalUrl(ENV_ID, environmentPortalUrl);

            assertThat(cut.resolveUrl(aPortal(false))).hasValue(STORED_URL);
        }

        private static Portal aPortal(boolean defaultPortal) {
            return Portal.of(
                PortalId.of("11111111-1111-1111-1111-111111111111"),
                ENV_ID,
                "org-1",
                "Portal",
                PortalNavigationStructure.empty(),
                null,
                defaultPortal,
                STORED_URL
            );
        }

        @SneakyThrows
        private static URL url(String value) {
            return URI.create(value).toURL();
        }
    }
}
