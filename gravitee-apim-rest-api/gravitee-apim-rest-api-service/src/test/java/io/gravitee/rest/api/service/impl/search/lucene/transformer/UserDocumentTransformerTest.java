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
package io.gravitee.rest.api.service.impl.search.lucene.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.rest.api.model.UserEntity;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UserDocumentTransformerTest {

    UserDocumentTransformer transformer = new UserDocumentTransformer();

    @Test
    public void shouldTransformWithoutError_OnMissingReferenceId() {
        UserEntity user = new UserEntity();
        user.setId("user-uuid");
        Document doc = transformer.transform(user);
        assertThat(doc.get("id")).isEqualTo(user.getId());
    }

    @Nested
    class CustomFields {

        @Test
        public void should_transform_a_user_with_custom_fields() {
            // Given
            var user = UserEntity.builder()
                .id("user-uuid")
                .customFields(Map.ofEntries(Map.entry("prop1", "value1"), Map.entry("prop2", "value2")))
                .build();

            // When
            var doc = transformer.transform(user);

            // Then
            assertThat(doc.getValues("custom")).containsExactlyInAnyOrder("value1", "value2");
        }

        @Test
        public void should_ignore_empty_custom_fields() {
            // Given
            var user = UserEntity.builder()
                .id("user-uuid")
                .customFields(Map.ofEntries(Map.entry("prop1", ""), Map.entry("prop3", "value")))
                .build();

            // When
            var doc = transformer.transform(user);

            // Then
            assertThat(doc.getValues("custom")).containsExactlyInAnyOrder("value");
        }

        @Test
        public void should_ignore_custom_fields_containing_base64_images() {
            // Given
            var user = UserEntity.builder()
                .id("user-uuid")
                .customFields(
                    Map.ofEntries(
                        Map.entry("prop1", "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAASABIAAD/4QBMRXhpZgAATU0A"),
                        Map.entry("prop2", "value")
                    )
                )
                .build();

            // When
            var doc = transformer.transform(user);

            // Then
            assertThat(doc.getValues("custom")).containsExactlyInAnyOrder("value");
        }
    }
}
