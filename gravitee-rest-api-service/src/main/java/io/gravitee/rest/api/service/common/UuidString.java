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
package io.gravitee.rest.api.service.common;

import io.gravitee.common.utils.UUID;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * UuidString class generates UUID strings. Randomly, or from fields.
 *
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface UuidString {
    /**
     * Random version-4 UUID will have 6 predetermined variant and version bits, leaving 122 bits for the randomly generated part.
     * @return A random UUID as string
     */
    static String generateRandom() {
        return UUID.toString(UUID.random());
    }

    static String generateForEnvironment(String environmentId, String... fields) {
        if (Stream.of(fields).anyMatch(Objects::isNull)) {
            return generateRandom();
        }

        StringBuilder b = new StringBuilder();
        b.append(environmentId);
        for (String f : fields) {
            b.append(f);
        }
        String baseStringForUUID = b.toString();

        return UUID.toString(java.util.UUID.nameUUIDFromBytes(baseStringForUUID.getBytes()));
    }
}
