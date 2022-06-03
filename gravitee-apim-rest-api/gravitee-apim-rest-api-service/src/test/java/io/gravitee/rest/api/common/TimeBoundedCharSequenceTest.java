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
package io.gravitee.rest.api.common;

import static org.assertj.core.api.Assertions.*;

import io.gravitee.rest.api.service.common.TimeBoundedCharSequence;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class TimeBoundedCharSequenceTest {

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIfTimeoutIsReached() throws InterruptedException {
        final Duration timeout = Duration.ofMillis(500);
        final CharSequence charSequence = new TimeBoundedCharSequence("gravitee", timeout);
        TimeUnit.MILLISECONDS.sleep(timeout.plusMillis(100).toMillis());
        charSequence.charAt(0);
    }

    @Test
    public void shouldNotThrowIfTimeoutIsNotReached() throws InterruptedException {
        final Duration timeout = Duration.ofMillis(500);
        final CharSequence charSequence = new TimeBoundedCharSequence("gravitee", timeout);
        TimeUnit.MILLISECONDS.sleep(timeout.minusMillis(100).toMillis());
        assertThat(charSequence.charAt(0)).isEqualTo('g');
    }

    @Test(expected = IllegalStateException.class)
    public void shouldApplyTimeoutOnSubsequence() throws InterruptedException {
        final Duration timeout = Duration.ofMillis(500);
        final CharSequence charSequence = new TimeBoundedCharSequence("gravitee", timeout);
        CharSequence subSequence = charSequence.subSequence(5, 7);
        TimeUnit.MILLISECONDS.sleep(timeout.plusMillis(100).toMillis());
        subSequence.charAt(0);
    }

    @Test
    public void shouldImplementLengthProperly() {
        final Duration timeout = Duration.ofMillis(500);
        final CharSequence charSequence = new TimeBoundedCharSequence("gravitee", timeout);
        assertThat(charSequence.length()).isEqualTo(8);
    }

    @Test
    public void shouldImplementSubsequenceProperly() {
        final Duration timeout = Duration.ofMillis(500);
        final CharSequence charSequence = new TimeBoundedCharSequence("gravitee", timeout);
        assertThat(charSequence.subSequence(5, 8)).hasToString("tee");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldTimeoutWithRegexMatcher() throws InterruptedException {
        final Duration timeout = Duration.ofMillis(500);
        final CharSequence charSequence = new TimeBoundedCharSequence("gravitee", timeout);
        final Pattern pattern = Pattern.compile("[a-z]+");
        final Matcher matcher = pattern.matcher(charSequence);
        TimeUnit.MILLISECONDS.sleep(timeout.plusMillis(100).toMillis());
        assertThat(matcher.matches()).isFalse();
    }

    @Test
    public void shouldResolveRegexMatcherBeforeTimeout() throws InterruptedException {
        final Duration timeout = Duration.ofMillis(500);
        final CharSequence charSequence = new TimeBoundedCharSequence("gravitee", timeout);
        final Pattern pattern = Pattern.compile("[a-z]+");
        final Matcher matcher = pattern.matcher(charSequence);
        TimeUnit.MILLISECONDS.sleep(timeout.minusMillis(100).toMillis());
        assertThat(matcher.matches()).isTrue();
    }
}
