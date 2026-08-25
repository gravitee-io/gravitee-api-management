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
package io.gravitee.repository.elasticsearch.utils;

import java.time.Duration;
import java.time.Instant;

/**
 * Placement of the bucket grid of a date histogram run over a caller-supplied window.
 *
 * <p>By default Elasticsearch aligns buckets on epoch multiples of the interval, so the bucket holding the
 * start of the window generally begins before it. That leaves no good option: filtering strictly on the window
 * yields a short first bucket, and filtering wider admits documents that come back as a bucket outside the
 * window — {@code extended_bounds} extends the returned range, it does not clamp it. Since these responses
 * carry no timestamp per point, such a bucket shifts the whole series by one interval.
 *
 * <p>Shifting the grid onto the window itself removes the dilemma: the first bucket starts exactly at the
 * start of the window, so the filter can begin there and the first bucket is still complete.
 */
public final class HistogramGrid {

    private HistogramGrid() {}

    /**
     * Offset, in milliseconds, that places a bucket boundary exactly on {@code windowStart}.
     *
     * @return zero when the window already starts on a boundary, or when the interval is unusable (see
     *         {@link #usableMillis(Duration)}).
     */
    public static long offsetMillis(final Instant windowStart, final Duration interval) {
        final long intervalMillis = usableMillis(interval);
        return intervalMillis == 0 ? 0L : Math.floorMod(windowStart.toEpochMilli(), intervalMillis);
    }

    /**
     * End of the last bucket the histogram emits for the window, which is the instant a range filter must stop
     * short of.
     *
     * <p>{@code extended_bounds.max} rounds down onto the grid, so the last emitted bucket is the one holding
     * {@code windowEnd}, not the one starting on it. Filtering up to {@code windowEnd + interval} instead is
     * only equivalent when the window spans a whole number of intervals; otherwise it admits documents that
     * open a bucket past {@code extended_bounds.max} — and since {@code extended_bounds} does not clamp, that
     * bucket is returned, adding a point that carries traffic from after the window.
     *
     * @return {@code windowEnd} itself when the interval is unusable (see {@link #usableMillis(Duration)}).
     */
    public static Instant endOfLastBucket(final Instant windowStart, final Instant windowEnd, final Duration interval) {
        final long intervalMillis = usableMillis(interval);
        if (intervalMillis == 0) {
            return windowEnd;
        }
        final long startMillis = windowStart.toEpochMilli();
        final long span = windowEnd.toEpochMilli() - startMillis;
        final long buckets = Math.floorDiv(span, intervalMillis) + 1;
        return Instant.ofEpochMilli(startMillis + buckets * intervalMillis);
    }

    /**
     * @return the interval in milliseconds, or zero when it does not amount to at least one — which covers a
     *         null, zero or negative duration, and also a positive one shorter than the millisecond the
     *         histogram is expressed in.
     */
    private static long usableMillis(final Duration interval) {
        if (interval == null) {
            return 0L;
        }
        final long intervalMillis = interval.toMillis();
        return intervalMillis <= 0 ? 0L : intervalMillis;
    }
}
