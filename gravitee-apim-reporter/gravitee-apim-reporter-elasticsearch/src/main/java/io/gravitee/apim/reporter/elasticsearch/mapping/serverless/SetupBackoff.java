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
package io.gravitee.apim.reporter.elasticsearch.mapping.serverless;

import java.time.Duration;

/**
 * The delay before setting up Serverless again: doubled after each failure, from one second up to one minute.
 */
final class SetupBackoff {

    private static final Duration FIRST_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_DELAY = Duration.ofMinutes(1);
    private static final int DOUBLINGS_TO_MAX = 6;

    private SetupBackoff() {}

    static Duration delay(int failedAttempts) {
        final Duration delay = FIRST_DELAY.multipliedBy(1L << Math.min(failedAttempts - 1, DOUBLINGS_TO_MAX));
        return delay.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : delay;
    }
}
