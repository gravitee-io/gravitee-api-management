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
package io.gravitee.gamma.definition.approval;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/**
 * How large the arguments of a call may be for an approval to hold it: an approver reviews the arguments in full, so a
 * call whose arguments are larger is refused instead of held. Every gateway and AIM measure it with this class, on a
 * tree read the same way (see {@link #sizeInBytes}).
 */
public final class ApprovalArguments {

    public static final int MAX_BYTES = 256 * 1024;

    private ApprovalArguments() {}

    /**
     * Read {@code arguments} with {@code USE_BIG_DECIMAL_FOR_FLOATS}, as the gateway and AIM do: a {@code double} keeps
     * fewer digits than the JSON text, so the size would differ from theirs.
     *
     * @return the size in bytes of the compact JSON text of {@code arguments}, in UTF-8
     */
    public static int sizeInBytes(JsonNode arguments) {
        Objects.requireNonNull(arguments, "arguments");
        return arguments.toString().getBytes(UTF_8).length;
    }

    public static boolean isTooLarge(JsonNode arguments) {
        return sizeInBytes(arguments) > MAX_BYTES;
    }
}
