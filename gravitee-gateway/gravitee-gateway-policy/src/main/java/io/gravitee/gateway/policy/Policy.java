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
package io.gravitee.gateway.policy;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Policy {

    String id();

    default void onRequest(Object ... args) throws PolicyException {}

    default void onResponse(Object ... args) throws PolicyException {}

    default ReadWriteStream<Buffer> onRequestContent(Object ... args) throws PolicyException { return null; }

    default ReadWriteStream<Buffer> onResponseContent(Object ... args) throws PolicyException { return null; }

    default boolean isStreamable() { return false; }

    default boolean isRunnable() { return true; }
}
