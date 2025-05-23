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
package io.gravitee.rest.api.service.common;

import io.gravitee.common.utils.UUID;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * UuidString class generates UUID strings. Randomly, or from fields.
 *
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UuidString {

    private static final Function<String, String> DEFAULT_GENERATOR = (String seed) -> {
        if (seed == null) {
            return UUID.toString(UUID.random());
        }
        return UUID.toString(java.util.UUID.nameUUIDFromBytes(seed.getBytes()));
    };

    private static Function<String, String> uuidGenerator = DEFAULT_GENERATOR;

    private UuidString() {}

    /**
     * Random version-4 UUID will have 6 predetermined variant and version bits, leaving 122 bits for the randomly generated part.
     * @return A random UUID as string
     */
    public static String generateRandom() {
        return uuidGenerator.apply(null);
    }

    public static String generateFrom(String... seeds) {
        if (Stream.of(seeds).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Seeds must not be null");
        }

        StringBuilder b = new StringBuilder();

        for (String seed : seeds) {
            b.append(seed);
        }

        return uuidGenerator.apply(b.toString());
    }

    public static String generateForEnvironment(String environmentId, String... fields) {
        if (Stream.of(fields).anyMatch(Objects::isNull)) {
            return generateRandom();
        }

        StringBuilder b = new StringBuilder();
        b.append(environmentId);
        for (String f : fields) {
            b.append(f);
        }

        return uuidGenerator.apply(b.toString());
    }

    public static void overrideGenerator(Function<String, String> newGenerator) {
        UuidString.uuidGenerator = newGenerator;
    }

    public static void overrideGenerator(Supplier<String> newGenerator) {
        UuidString.uuidGenerator = (String seed) -> newGenerator.get();
    }

    public static void reset() {
        UuidString.uuidGenerator = DEFAULT_GENERATOR;
    }
}
