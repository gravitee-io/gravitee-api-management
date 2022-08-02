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
package io.gravitee.plugin.entrypoint.sse.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@ToString
@EqualsAndHashCode
public class SseEvent {

    private String id;
    private String event;
    private byte[] data;
    private Integer retry;
    private Map<String, Object> comments;

    public String format() {
        List<String> event = new ArrayList<>();

        if (retry != null) {
            event.add("retry: " + retry);
        }
        if (id != null) {
            event.add("id: " + id);
        }
        if (this.event != null) {
            event.add("event: " + this.event);
        }
        if (data != null) {
            event.add("data: " + new String(data));
        }
        if (comments != null) {
            comments.forEach(
                (s, o) -> {
                    event.add(String.format(":%s: %s", s, o.toString()));
                }
            );
        }
        return String.join("\n", event);
    }
}
