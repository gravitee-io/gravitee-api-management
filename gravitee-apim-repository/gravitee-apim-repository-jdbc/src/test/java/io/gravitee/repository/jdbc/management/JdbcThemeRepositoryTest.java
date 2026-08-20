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
package io.gravitee.repository.jdbc.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.gravitee.repository.management.model.ThemeAutomationMetadata;
import org.junit.jupiter.api.Test;

class JdbcThemeRepositoryTest {

    @Test
    void serializeAutomationMetadata_writes_hrid_field_as_json() {
        var meta = ThemeAutomationMetadata.builder().hrid("brand-theme").build();

        var json = JdbcThemeRepository.serializeAutomationMetadata(meta);

        assertThat(json).contains("\"hrid\"").contains("brand-theme");
    }

    @Test
    void deserializeAutomationMetadata_reads_hrid_field_from_json() {
        var meta = JdbcThemeRepository.deserializeAutomationMetadata("{\"hrid\":\"brand-theme\"}");

        assertThat(meta).isNotNull();
        assertThat(meta.getHrid()).isEqualTo("brand-theme");
    }

    @Test
    void serializeAutomationMetadata_wraps_exception_as_illegal_argument() {
        var self = new ThemeAutomationMetadata();
        // Force a Jackson failure by creating a value that references itself indirectly is hard;
        // easier: pass a subclass overriding a getter to throw.
        var throwing = new ThemeAutomationMetadata() {
            @Override
            public String getHrid() {
                throw new RuntimeException("boom");
            }
        };

        var ex = catchThrowable(() -> JdbcThemeRepository.serializeAutomationMetadata(throwing));

        assertThat(ex)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Failed to serialize automation metadata")
            .hasCauseInstanceOf(Exception.class);
    }

    @Test
    void deserializeAutomationMetadata_wraps_exception_as_illegal_argument() {
        var ex = catchThrowable(() -> JdbcThemeRepository.deserializeAutomationMetadata("{not-valid-json"));

        assertThat(ex)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Failed to deserialize automation metadata")
            .hasCauseInstanceOf(Exception.class);
    }
}
