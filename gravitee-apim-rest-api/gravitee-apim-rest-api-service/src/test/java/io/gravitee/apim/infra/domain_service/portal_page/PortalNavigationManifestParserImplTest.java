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
package io.gravitee.apim.infra.domain_service.portal_page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemSourceException;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationManifestParser;
import io.gravitee.rest.api.service.impl.GraviteeDescriptorServiceImpl;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/** Exercises the real descriptor reading, which every other test replaces with an in-memory parser. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalNavigationManifestParserImplTest {

    private final PortalNavigationManifestParserImpl parser = new PortalNavigationManifestParserImpl(new GraviteeDescriptorServiceImpl());

    @Test
    void should_expose_the_gravitee_descriptor_file_name() {
        assertThat(parser.manifestFileName()).isEqualTo(".gravitee.json");
    }

    @Test
    void should_parse_the_documentation_pages_of_a_version_1_descriptor() {
        var manifest = """
            {
              "version": 1,
              "documentation": {
                "pages": [
                  { "src": "/docs/intro.md", "name": "Introduction", "dest": "/guides" },
                  { "src": "/docs/api.yaml" }
                ]
              }
            }
            """;

        assertThat(parser.parse(manifest))
            .extracting(
                PortalNavigationManifestParser.ManifestPage::src,
                PortalNavigationManifestParser.ManifestPage::name,
                PortalNavigationManifestParser.ManifestPage::dest
            )
            .containsExactly(tuple("/docs/intro.md", "Introduction", "/guides"), tuple("/docs/api.yaml", null, null));
    }

    @Test
    void should_return_no_pages_when_the_descriptor_declares_no_documentation() {
        assertThat(parser.parse("{ \"version\": 1 }")).isEmpty();
    }

    @Test
    void should_reject_a_descriptor_with_an_unsupported_version() {
        assertThatThrownBy(() -> parser.parse("{ \"version\": 2 }")).isInstanceOf(InvalidPortalNavigationItemSourceException.class);
    }

    @Test
    void should_reject_an_unreadable_descriptor() {
        assertThatThrownBy(() -> parser.parse("not json at all")).isInstanceOf(InvalidPortalNavigationItemSourceException.class);
    }
}
