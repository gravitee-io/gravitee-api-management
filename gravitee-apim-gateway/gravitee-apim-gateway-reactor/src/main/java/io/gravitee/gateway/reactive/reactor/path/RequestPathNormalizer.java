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
package io.gravitee.gateway.reactive.reactor.path;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Resolves the dot segments of a request path, following RFC 3986 §5.2.4.
 *
 * <p>Two deliberate boundaries:
 *
 * <ul>
 *   <li><b>Percent-encoding is decoded only where it spells a dot segment.</b> RFC 3986 §6.2.2.2
 *       allows decoding every unreserved character, but doing so would rewrite paths that have
 *       nothing to do with traversal. A segment is decoded only when it resolves to {@code .} or
 *       {@code ..}, which is exactly what {@code %2e%2e} is used for. Any other segment is carried
 *       over untouched, encoding included.
 *   <li><b>Encoded slashes are left alone.</b> Turning {@code %2F} into a separator is a separate
 *       decision with its own breakage profile, and it belongs to its own option rather than being
 *       folded in here.
 * </ul>
 *
 * <p>Duplicate slashes are preserved for the same reason: an empty segment is a valid segment, and
 * merging them is a distinct behaviour change.
 *
 * @author GraviteeSource Team
 */
public final class RequestPathNormalizer {

    private static final char SEGMENT_SEPARATOR = '/';
    private static final String CURRENT = ".";
    private static final String PARENT = "..";

    private RequestPathNormalizer() {}

    /**
     * @return the path with its dot segments resolved, or the very same instance when there is
     *     nothing to resolve, so callers can rely on {@code ==} for the common case.
     */
    public static String normalize(final String path) {
        if (path == null || path.isEmpty() || !mayContainDotSegments(path)) {
            return path;
        }

        final boolean absolute = path.charAt(0) == SEGMENT_SEPARATOR;
        final String[] segments = path.split("/", -1);
        final Deque<String> resolved = new ArrayDeque<>(segments.length);
        boolean lastWasDotSegment = false;

        for (int i = absolute ? 1 : 0; i < segments.length; i++) {
            final String segment = segments[i];
            final String dotSegment = asDotSegment(segment);

            if (CURRENT.equals(dotSegment)) {
                lastWasDotSegment = true;
            } else if (PARENT.equals(dotSegment)) {
                // Popping past the root is not an error: RFC 3986 §5.2.4 discards the extra step.
                resolved.pollLast();
                lastWasDotSegment = true;
            } else {
                resolved.addLast(segment);
                lastWasDotSegment = false;
            }
        }

        final StringBuilder normalized = new StringBuilder(path.length());
        if (absolute) {
            normalized.append(SEGMENT_SEPARATOR);
        }
        normalized.append(String.join("/", resolved));
        // "/a/b/.." resolves to "/a/", not "/a": a dot segment always leaves a directory behind.
        if (lastWasDotSegment && (normalized.length() == 0 || normalized.charAt(normalized.length() - 1) != SEGMENT_SEPARATOR)) {
            normalized.append(SEGMENT_SEPARATOR);
        }

        final String result = normalized.toString();
        return result.equals(path) ? path : result;
    }

    /**
     * A dot segment can be spelled several ways in the same request: {@code ..}, {@code %2e%2e},
     * {@code .%2E}. All of them mean the parent directory to a conforming receiver.
     *
     * @return {@code "."}, {@code ".."}, or {@code null} when the segment is an ordinary one.
     */
    private static String asDotSegment(final String segment) {
        if (segment.isEmpty() || segment.length() > 6) {
            return null;
        }
        final String decoded = segment.indexOf('%') < 0 ? segment : replaceEncodedDots(segment);
        return CURRENT.equals(decoded) || PARENT.equals(decoded) ? decoded : null;
    }

    private static String replaceEncodedDots(final String segment) {
        final StringBuilder decoded = new StringBuilder(segment.length());
        for (int i = 0; i < segment.length(); i++) {
            final char c = segment.charAt(i);
            if (c == '%' && i + 2 < segment.length() && segment.charAt(i + 1) == '2' && (segment.charAt(i + 2) | 0x20) == 'e') {
                decoded.append('.');
                i += 2;
            } else {
                decoded.append(c);
            }
        }
        return decoded.toString();
    }

    private static boolean mayContainDotSegments(final String path) {
        return path.indexOf('.') >= 0 || path.indexOf('%') >= 0;
    }
}
