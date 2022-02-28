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
package io.gravitee.repository.bridge.client.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.gravitee.common.data.domain.Page;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.codec.impl.BodyCodecImpl;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BodyCodecs {
    static {
        DatabindCodec.mapper().enable(JsonGenerator.Feature.IGNORE_UNKNOWN).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public static <T> BodyCodec<Optional<T>> optional(Class<T> clazz) {
        return new BodyCodecImpl<>(buffer -> Optional.of(buffer.toJsonObject().mapTo(clazz)));
    }

    public static <T> BodyCodec<List<T>> list(Class<T> clazz) {
        return new BodyCodecImpl<>(
            buffer -> buffer.toJsonArray().stream().map(x -> ((JsonObject) x).mapTo(clazz)).collect(Collectors.toList())
        );
    }

    public static <T> BodyCodec<Set<T>> set(Class<T> clazz) {
        return new BodyCodecImpl<>(
            buffer -> buffer.toJsonArray().stream().map(x -> ((JsonObject) x).mapTo(clazz)).collect(Collectors.toSet())
        );
    }

    public static <T> BodyCodec<Page<T>> page(Class<T> clazz) {
        return new BodyCodecImpl<>(
            buffer -> {
                JsonObject pageObj = buffer.toJsonObject();
                int page = pageObj.getInteger("pageNumber");
                int size = pageObj.getInteger("pageElements");
                int total = pageObj.getInteger("totalElements");

                List<T> content = pageObj
                    .getJsonArray("content")
                    .stream()
                    .map(x -> ((JsonObject) x).mapTo(clazz))
                    .collect(Collectors.toList());
                return new Page<>(content, page, size, total);
            }
        );
    }
}
