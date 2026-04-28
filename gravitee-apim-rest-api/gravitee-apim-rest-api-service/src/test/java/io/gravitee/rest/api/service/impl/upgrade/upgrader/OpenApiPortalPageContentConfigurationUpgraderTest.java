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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PortalPageContentRepository;
import io.gravitee.repository.management.model.PortalPageContent;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenApiPortalPageContentConfigurationUpgraderTest {

    private static final String DEFAULT_REDOC_CONFIGURATION = "{\"viewer\":\"REDOC\"}";

    private FakePortalPageContentRepository portalPageContentRepository;
    private OpenApiPortalPageContentConfigurationUpgrader upgrader;

    @BeforeEach
    void setUp() {
        portalPageContentRepository = new FakePortalPageContentRepository();
        upgrader = new OpenApiPortalPageContentConfigurationUpgrader(portalPageContentRepository);
    }

    @Test
    @SneakyThrows
    void should_do_nothing_when_there_is_no_openapi_content() {
        portalPageContentRepository.initWith(Collections.emptyList());

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalPageContentRepository.updatedContents()).isEmpty();
    }

    @Test
    @SneakyThrows
    void should_set_default_viewer_configuration_on_openapi_contents_without_configuration() {
        var contentWithoutConfiguration = openApiContent("content-without-configuration", null);
        var contentWithBlankConfiguration = openApiContent("content-with-blank-configuration", " ");
        portalPageContentRepository.initWith(List.of(contentWithoutConfiguration, contentWithBlankConfiguration));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalPageContentRepository.updatedContents())
            .extracting(PortalPageContent::getId, PortalPageContent::getConfiguration)
            .containsExactlyInAnyOrder(
                tuple("content-without-configuration", DEFAULT_REDOC_CONFIGURATION),
                tuple("content-with-blank-configuration", DEFAULT_REDOC_CONFIGURATION)
            );
    }

    @Test
    @SneakyThrows
    void should_not_override_existing_viewer_configuration() {
        var existingConfiguration = "{\"displayOperationId\":true}";
        var contentWithConfiguration = openApiContent("content-with-configuration", existingConfiguration);
        portalPageContentRepository.initWith(List.of(contentWithConfiguration));

        assertThat(upgrader.upgrade()).isTrue();

        assertThat(portalPageContentRepository.updatedContents()).isEmpty();
        assertThat(contentWithConfiguration.getConfiguration()).isEqualTo(existingConfiguration);
    }

    @Test
    @SneakyThrows
    void should_throw_upgrader_exception_on_repository_error() {
        portalPageContentRepository.failOnFindAllByType();

        assertThatThrownBy(() -> upgrader.upgrade())
            .isInstanceOf(UpgraderException.class)
            .hasMessageContaining("connection failed");
    }

    private static PortalPageContent openApiContent(String id, String configuration) {
        return PortalPageContent.builder()
            .id(id)
            .organizationId("organization-id")
            .environmentId("environment-id")
            .type(PortalPageContent.Type.OPENAPI)
            .content("openapi: 3.0.0")
            .configuration(configuration)
            .build();
    }

    private static class FakePortalPageContentRepository implements PortalPageContentRepository {

        private final List<PortalPageContent> contents = new java.util.ArrayList<>();
        private final List<PortalPageContent> updatedContents = new java.util.ArrayList<>();
        private boolean failOnFindAllByType;

        void initWith(List<PortalPageContent> contents) {
            this.contents.clear();
            this.contents.addAll(contents);
            this.updatedContents.clear();
            this.failOnFindAllByType = false;
        }

        void failOnFindAllByType() {
            this.failOnFindAllByType = true;
        }

        List<PortalPageContent> updatedContents() {
            return updatedContents;
        }

        @Override
        public List<PortalPageContent> findAllByType(PortalPageContent.Type type) throws TechnicalException {
            if (failOnFindAllByType) {
                throw new TechnicalException("connection failed");
            }
            return contents
                .stream()
                .filter(content -> content.getType() == type)
                .toList();
        }

        @Override
        public Set<PortalPageContent> findAll() {
            return new LinkedHashSet<>(contents);
        }

        @Override
        public Optional<PortalPageContent> findById(String id) {
            return contents
                .stream()
                .filter(content -> content.getId().equals(id))
                .findFirst();
        }

        @Override
        public PortalPageContent create(PortalPageContent item) {
            contents.add(item);
            return item;
        }

        @Override
        public PortalPageContent update(PortalPageContent item) {
            updatedContents.add(item);
            return item;
        }

        @Override
        public void delete(String id) {
            contents.removeIf(content -> content.getId().equals(id));
        }
    }
}
