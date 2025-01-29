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
package io.gravitee.apim.infra.query_service.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.media.model.Media;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaQueryServiceImplTest {

    @Mock
    MediaRepository mediaRepository;

    @InjectMocks
    MediaQueryServiceImpl sut;

    @Nested
    class FindAllByApiId {

        @Test
        public void shouldCorrectlyWapTheResult() throws TechnicalException {
            // Given
            var media = new io.gravitee.repository.media.model.Media("type", "sub type", "file name", 12L);
            media.setId("ID");
            media.setApi("api-id");
            when(mediaRepository.findAllByApi(anyString())).thenReturn(List.of(media));

            // When
            List<Media> allByApiId = sut.findAllByApiId("api-id");

            // Then
            assertThat(allByApiId).containsOnly(new Media("ID", null, "type", "sub type", "file name", null, 12L, null, "api-id"));
        }

        @Test
        public void shouldWrapException() throws TechnicalException {
            // Given
            when(mediaRepository.findAllByApi(anyString())).thenThrow(new TechnicalException("error"));

            // When
            var th = catchThrowable(() -> sut.findAllByApiId("api-id"));

            // Then
            assertThat(th).isInstanceOf(TechnicalManagementException.class);
        }
    }
}
