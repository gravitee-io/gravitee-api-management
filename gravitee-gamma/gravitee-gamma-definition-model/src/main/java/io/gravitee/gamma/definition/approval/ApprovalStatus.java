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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The status AIM publishes on {@link ApprovalState#getStatus()}. The wire field stays a string, so that a gateway keeps
 * working when a newer control plane adds a value: {@link #parse(String)} reads such a value as {@link #UNRECOGNIZED}.
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    EXECUTING,
    EXECUTED,
    /** The call started, but how it ended never came back. */
    UNKNOWN,
    REJECTED,
    EXPIRED,
    /** A time-based grant ended early. */
    ENDED,
    /** An approved call nobody came back for. */
    NOT_COLLECTED,
    /** Not published by AIM: a value this node does not know. It never lets a call through. */
    UNRECOGNIZED;

    private static final Map<String, ApprovalStatus> BY_NAME = Stream.of(values())
        .filter(status -> status != UNRECOGNIZED)
        .collect(Collectors.toUnmodifiableMap(Enum::name, Function.identity()));

    /** @return the status named {@code published}, or {@link #UNRECOGNIZED} for {@code null} or any other value */
    public static ApprovalStatus parse(String published) {
        return published == null ? UNRECOGNIZED : BY_NAME.getOrDefault(published, UNRECOGNIZED);
    }

    /** Whether a time-based grant in this status lets the calls it covers through: approved, until AIM ends it. */
    public boolean isGranting() {
        return this == APPROVED || this == EXECUTING || this == EXECUTED;
    }

    /**
     * Whether an identical call still finds the request by its open key: while it waits, runs, or has a conclusion
     * still to tell. AIM clears the open key once it released the request.
     */
    public boolean isFoundByOpenKey() {
        return this == PENDING || this == APPROVED || this == EXECUTING || this == REJECTED || this == EXPIRED;
    }
}
