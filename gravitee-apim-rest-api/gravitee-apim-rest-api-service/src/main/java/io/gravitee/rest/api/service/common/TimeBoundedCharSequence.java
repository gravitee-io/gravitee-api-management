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

import java.time.Duration;
import org.jetbrains.annotations.NotNull;

/**
 * @author GraviteeSource Team
 *
 * TimeBoundedCharSequence will fail if charAt is called after a given timeout duration.
 * It can therefore be passed as an argument to a regex matcher to protect from catastrophic backtracking.
 *
 * <pre>
 *   Duration timeout = Duration.ofMillis(1000);
 *   CharSequence charSequence = new TimeBoundedCharSequence("gravitee", timeout);
 *   Pattern pattern = Pattern.compile("[a-z]+");
 *   Matcher matcher = pattern.matcher(charSequence);
 *   matcher.matches(); // Will throw an IllegalStateException if matches takes more than 1 second to resolve
 * </pre>
 *
 * see https://sonarcloud.io/organizations/gravitee-io/rules?open=java%3AS5852&rule_key=java%3AS5852
 */
public class TimeBoundedCharSequence implements CharSequence {

    private final CharSequence charSequence;
    private final long timeThreshold;

    public TimeBoundedCharSequence(CharSequence charSequence, Duration timeoutDuration) {
        this.charSequence = charSequence;
        this.timeThreshold = System.currentTimeMillis() + timeoutDuration.toMillis();
    }

    @Override
    public int length() {
        return charSequence.length();
    }

    @Override
    public char charAt(int index) {
        if (System.currentTimeMillis() > timeThreshold) {
            throw new IllegalStateException("Timeout has expired for exploring char sequence");
        }
        return charSequence.charAt(index);
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return new TimeBoundedCharSequence(
            charSequence.subSequence(start, end),
            Duration.ofMillis(timeThreshold - System.currentTimeMillis())
        );
    }

    @Override
    public String toString() {
        return charSequence.toString();
    }
}
