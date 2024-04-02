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
package io.gravitee.definition.model.v4.failover;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@Schema(name = "FailoverV4")
public class Failover implements Serializable {

    public static final int DEFAULT_MAX_RETRIES = 2;
    public static final long DEFAULT_SLOW_CALL_DURATION_MILLIS = 2_000L;
    public static final long DEFAULT_OPEN_STATE_DURATION_MILLIS = 10_000L;
    public static final int DEFAULT_MAX_FAILURES = 5;
    public static final boolean DEFAULT_PER_SUBSCRIPTION = true;

    @Builder.Default
    private boolean enabled = false;

    @Builder.Default
    private int maxRetries = DEFAULT_MAX_RETRIES;

    @Builder.Default
    private long slowCallDuration = DEFAULT_SLOW_CALL_DURATION_MILLIS;

    @Builder.Default
    private long openStateDuration = DEFAULT_OPEN_STATE_DURATION_MILLIS;

    @Builder.Default
    private int maxFailures = DEFAULT_MAX_FAILURES;

    @Builder.Default
    private boolean perSubscription = DEFAULT_PER_SUBSCRIPTION;
}
