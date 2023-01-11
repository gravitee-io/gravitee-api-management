/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.http.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.IdGenerator;
import io.vertx.rxjava3.core.MultiMap;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class AbstractVertxServerRequestTest {

    @Mock
    private HttpServerRequest nativeRequest;

    @Mock
    private IdGenerator idGenerator;

    private AbstractVertxServerRequest cut;

    @BeforeEach
    void setUp() {
        when(nativeRequest.host()).thenReturn("host");
        when(nativeRequest.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(nativeRequest.method()).thenReturn(io.vertx.core.http.HttpMethod.GET);
        cut = new AbstractVertxServerRequest(nativeRequest, idGenerator) {};
    }

    @Test
    void shouldOverrideHttpMethod() {
        cut.method(HttpMethod.PUT);

        assertThat(cut.method()).isEqualTo(HttpMethod.PUT);
    }

    @Test
    void shouldThrowWhenOverridingMethodWithNull() {
        assertThatThrownBy(() -> cut.method(null)).isInstanceOf(IllegalStateException.class).hasMessage("Http method should not be null");
    }
}
