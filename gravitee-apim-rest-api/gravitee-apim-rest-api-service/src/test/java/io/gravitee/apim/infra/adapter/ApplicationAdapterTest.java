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
package io.gravitee.apim.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Origin;
import io.gravitee.repository.management.model.ApiKeyMode;
import io.gravitee.repository.management.model.Application;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.management.model.ApplicationType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import java.sql.Date;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApplicationAdapterTest {

    @Test
    public void should_convert_application_to_application_entity() {
        final Application toConvert = Application.builder()
            .id("app-id")
            .name("app-name")
            .description("app-description")
            .domain("app-domain")
            .type(ApplicationType.SIMPLE)
            .status(ApplicationStatus.ACTIVE)
            .picture("app-id")
            .background("app-id")
            .groups(Set.of("group-1", "group-2"))
            .createdAt(Date.from(Instant.now()))
            .updatedAt(Date.from(Instant.now()))
            .disableMembershipNotifications(true)
            .apiKeyMode(ApiKeyMode.EXCLUSIVE)
            .origin(Origin.MANAGEMENT)
            .build();

        BaseApplicationEntity applicationEntity = ApplicationAdapter.INSTANCE.toEntity(toConvert);

        assertThat(applicationEntity.getId()).isEqualTo(toConvert.getId());
        assertThat(applicationEntity.getName()).isEqualTo(toConvert.getName());
        assertThat(applicationEntity.getDescription()).isEqualTo(toConvert.getDescription());
        assertThat(applicationEntity.getDomain()).isEqualTo(toConvert.getDomain());
        assertThat(applicationEntity.getType()).isEqualTo(toConvert.getType().name());
        assertThat(applicationEntity.getStatus()).isEqualTo(toConvert.getStatus().name());
        assertThat(applicationEntity.getPicture()).isEqualTo(toConvert.getPicture());
        assertThat(applicationEntity.getBackground()).isEqualTo(toConvert.getBackground());
        assertThat(applicationEntity.getGroups()).isEqualTo(toConvert.getGroups());
        assertThat(applicationEntity.getCreatedAt()).isEqualTo(toConvert.getCreatedAt());
        assertThat(applicationEntity.getUpdatedAt()).isEqualTo(toConvert.getUpdatedAt());
        assertThat(applicationEntity.isDisableMembershipNotifications()).isEqualTo(toConvert.isDisableMembershipNotifications());
        assertThat(applicationEntity.getApiKeyMode().name()).isEqualTo(toConvert.getApiKeyMode().name());
        assertThat(applicationEntity.getOrigin()).isEqualTo(toConvert.getOrigin());
    }

    @Test
    public void should_convert_application_to_application_entity_null_app() {
        assertThat(ApplicationAdapter.INSTANCE.toEntity(null)).isNull();
    }

    @Test
    public void should_convert_application_to_application_entity_null_fields() {
        final Application toConvert = Application.builder()
            .id("app-id")
            .name("app-name")
            .description("app-description")
            .domain("app-domain")
            .type(null)
            .status(null)
            .picture("app-id")
            .background("app-id")
            .groups(null)
            .createdAt(Date.from(Instant.now()))
            .updatedAt(Date.from(Instant.now()))
            .disableMembershipNotifications(true)
            .apiKeyMode(null)
            .origin(null)
            .build();

        BaseApplicationEntity applicationEntity = ApplicationAdapter.INSTANCE.toEntity(toConvert);

        assertThat(applicationEntity.getId()).isEqualTo(toConvert.getId());
        assertThat(applicationEntity.getName()).isEqualTo(toConvert.getName());
        assertThat(applicationEntity.getDescription()).isEqualTo(toConvert.getDescription());
        assertThat(applicationEntity.getDomain()).isEqualTo(toConvert.getDomain());
        assertThat(applicationEntity.getType()).isNull();
        assertThat(applicationEntity.getStatus()).isNull();
        assertThat(applicationEntity.getPicture()).isEqualTo(toConvert.getPicture());
        assertThat(applicationEntity.getBackground()).isEqualTo(toConvert.getBackground());
        assertThat(applicationEntity.getGroups()).isEqualTo(toConvert.getGroups());
        assertThat(applicationEntity.getCreatedAt()).isEqualTo(toConvert.getCreatedAt());
        assertThat(applicationEntity.getUpdatedAt()).isEqualTo(toConvert.getUpdatedAt());
        assertThat(applicationEntity.isDisableMembershipNotifications()).isEqualTo(toConvert.isDisableMembershipNotifications());
        assertThat(applicationEntity.getApiKeyMode()).isNull();
        assertThat(applicationEntity.getOrigin()).isEqualTo(Origin.MANAGEMENT);
    }
}
