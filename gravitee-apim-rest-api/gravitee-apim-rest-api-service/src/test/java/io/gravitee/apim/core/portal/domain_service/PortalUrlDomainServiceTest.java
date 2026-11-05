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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
}
