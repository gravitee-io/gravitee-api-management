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
package io.gravitee.apim.core.datetime;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;

public class TimeProvider {

    private TimeProvider() {}

    private static Clock clock = Clock.systemDefaultZone();

    public static ZonedDateTime now() {
        return ZonedDateTime.now(clock);
    }

    public static Instant instantNow() {
        return Instant.now(clock);
    }

    public static void overrideClock(Clock clock) {
        TimeProvider.clock = clock;
    }
}
