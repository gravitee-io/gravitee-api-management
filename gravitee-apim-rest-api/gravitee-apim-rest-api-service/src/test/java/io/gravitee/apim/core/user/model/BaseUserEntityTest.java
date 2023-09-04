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
package io.gravitee.apim.core.user.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class BaseUserEntityTest {

    @Nested
    class DisplayName {

        @Test
        void should_return_display_name_when_user_has_firstname_and_lastname() {
            var user = BaseUserEntity.builder().firstname("Jane").lastname("Doe").build();
            assertThat(user.displayName()).isEqualTo("Jane Doe");
        }

        @Test
        void should_return_display_name_when_user_has_only_firstname() {
            var user = BaseUserEntity.builder().firstname("Jane").build();
            assertThat(user.displayName()).isEqualTo("Jane");
        }

        @Test
        void should_return_display_name_when_user_has_only_lastname() {
            var user = BaseUserEntity.builder().lastname("Doe").build();
            assertThat(user.displayName()).isEqualTo("Doe");
        }

        @Test
        void should_return_email_when_user_coming_from_idp() {
            var user = BaseUserEntity.builder().email("jane.doe@gravitee.io").source("google").build();
            assertThat(user.displayName()).isEqualTo("jane.doe@gravitee.io");
        }

        @Test
        void should_return_sourceId_when_user_coming_from_idp_without_email() {
            var user = BaseUserEntity.builder().sourceId("google-user-id").source("google").build();
            assertThat(user.displayName()).isEqualTo("google-user-id");
        }

        @Test
        void should_return_sourceId_when_user_is_in_memory_user() {
            var user = BaseUserEntity.builder().sourceId("source-id").source("memory").build();
            assertThat(user.displayName()).isEqualTo("source-id");
        }
    }
}
