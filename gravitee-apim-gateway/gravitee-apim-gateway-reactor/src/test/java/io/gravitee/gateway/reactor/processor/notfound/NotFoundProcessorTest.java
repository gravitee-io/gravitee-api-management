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
package io.gravitee.gateway.reactor.processor.notfound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class NotFoundProcessorTest {

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Test
    void should_set_content_length_from_the_encoded_body_when_the_message_is_not_ascii() {
        String message = "Aucune API à cette adresse.";
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("http.errors[404].message", message)));

        HttpHeaders headers = HttpHeaders.create();
        when(context.request()).thenReturn(request);
        when(request.path()).thenReturn("/no-such-api");
        when(context.response()).thenReturn(response);
        when(response.headers()).thenReturn(headers);

        new NotFoundProcessor(environment).handler(ctx -> {}).handle(context);

        ArgumentCaptor<Buffer> body = ArgumentCaptor.forClass(Buffer.class);
        verify(response).write(body.capture());
        assertThat(body.getValue().toString()).isEqualTo(message);
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isEqualTo(
            Integer.toString(message.getBytes(StandardCharsets.UTF_8).length)
        );
        assertThat(headers.get(HttpHeaderNames.CONTENT_LENGTH)).isNotEqualTo(Integer.toString(message.length()));
    }
}
