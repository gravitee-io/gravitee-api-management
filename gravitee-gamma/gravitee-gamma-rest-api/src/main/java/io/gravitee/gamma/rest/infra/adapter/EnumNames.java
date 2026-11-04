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
package io.gravitee.gamma.rest.infra.adapter;

import java.util.Arrays;
import java.util.Optional;

/** Looks an enum constant up by its exact name, without the exception {@link Enum#valueOf} throws on a miss. */
final class EnumNames {

    private EnumNames() {}

    static <E extends Enum<E>> Optional<E> find(Class<E> type, String name) {
        return Arrays.stream(type.getEnumConstants())
            .filter(constant -> constant.name().equals(name))
            .findFirst();
    }
}
