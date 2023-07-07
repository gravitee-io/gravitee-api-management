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
package io.gravitee.apim.integration.tests.messages.sse.utils;

import io.reactivex.rxjava3.core.Maybe;
import io.vertx.rxjava3.core.buffer.Buffer;
import java.util.Map;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SseEventHandler {

    private static final String END_OF_EVENT = "\n\n";
    private final String requestId;
    SseEvent.SseEventBuilder sseEventBuilder;
    StringBuilder data = null;
    StringBuilder comment = null;

    public SseEventHandler(String requestId) {
        this.requestId = requestId;
    }

    public Maybe<SseEvent> handleMessage(Buffer buffer) {
        if (sseEventBuilder == null) {
            sseEventBuilder = SseEvent.builder();
        }

        final String bufferStr = buffer.toString();
        boolean end = bufferStr.endsWith(END_OF_EVENT);
        final String[] lines = bufferStr.split("\n");

        for (String line : lines) {
            if (line.startsWith("data:")) {
                int length = line.length();
                if (length > 5) {
                    int index = (line.charAt(5) != ' ' ? 5 : 6);
                    if (length > index) {
                        data = (data != null ? data : new StringBuilder());
                        data.append(line, index, line.length());
                    }
                }
            } else if (line.startsWith("id:")) {
                sseEventBuilder.id(line.substring(3).trim());
            } else if (line.startsWith("event:")) {
                sseEventBuilder.event(line.substring(6).trim());
            } else if (line.startsWith("retry:")) {
                sseEventBuilder = null;
                return Maybe.empty();
            } else if (line.startsWith(":")) {
                comment = (comment != null ? comment : new StringBuilder());
                comment.append(line.substring(1).trim());
            }
        }

        if (end) {
            sseEventBuilder.requestId(requestId);

            if (data != null) {
                final String content = data.toString();
                sseEventBuilder.counter(Integer.parseInt(content.replaceFirst(".*message-", "")));
                sseEventBuilder.data(content);
            }

            if (comment != null) {
                sseEventBuilder.comments(Map.of("comments", comment.toString()));
            }

            final SseEvent sseEvent = sseEventBuilder.build();
            sseEventBuilder = null;
            data = null;
            comment = null;
            return Maybe.just(sseEvent);
        }

        return Maybe.empty();
    }
}
