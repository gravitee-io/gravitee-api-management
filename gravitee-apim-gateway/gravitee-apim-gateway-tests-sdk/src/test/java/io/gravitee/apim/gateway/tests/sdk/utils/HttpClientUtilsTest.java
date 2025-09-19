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
package io.gravitee.apim.gateway.tests.sdk.utils;

import static org.mockito.Mockito.when;

import io.vertx.rxjava3.core.MultiMap;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpClientUtilsTest {

    @Mock
    private HttpClientResponse response;

    @Test
    void should_extract_headers() {
        when(response.headers()).thenReturn(
            MultiMap.caseInsensitiveMultiMap().set("X-Header-one", "value-one").set("X-Header-two", "value-two")
        );

        Assertions.assertThat(HttpClientUtils.extractHeaders(response))
            .contains(Map.entry("X-Header-one", "value-one"), Map.entry("X-Header-two", "value-two"))
            .doesNotContainKeys("random", "X-Header-three");
    }
}
