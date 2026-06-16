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
package io.gravitee.gamma.rest.core.observability.logs.model;

import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * HTTP request or response content from the {@code v4-log} index. Used for both entrypoint and
 * endpoint payloads in the merged log detail. Request payloads carry {@code method} and {@code uri};
 * response payloads carry {@code status}.
 *
 * @author GraviteeSource Team
 */
@Builder
public record HttpPayload(String method, String uri, Integer status, Map<String, List<String>> headers, String body) {}
