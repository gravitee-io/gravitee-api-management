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

/**
 * Normalizes a request path following RFC 3986, so that the gateway decides on the same path a
 * conforming receiver will resolve.
 *
 * <p>Derived from Eclipse Vert.x, {@code io.vertx.core.internal.net.RFC3986} in Vert.x 5 and
 * {@code io.vertx.core.http.impl.HttpUtils} in Vert.x 4, originally written by Paulo Lopes.
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation, dual-licensed under EPL-2.0 and
 * Apache-2.0, and reused here under Apache-2.0.
 *
 * <p>It is carried in this codebase rather than called through Vert.x because no public entry point
 * exists: the class sits in {@code impl} on Vert.x 4 and in {@code internal} on Vert.x 5, and it
 * moved between the two. Vendoring keeps one behaviour across every branch we maintain.
 *
 * <p>Three behaviours are worth stating, because they go beyond resolving dot segments:
 *
 * <ul>
 *   <li>Percent-encoded <b>unreserved</b> characters are decoded, as RFC 3986 §6.2.2.2 requires.
 *       That is what makes {@code %2e%2e} a dot segment, and it also turns {@code %41} into
 *       {@code A}. Reserved characters are untouched, so {@code %2F} never becomes a separator.
 *   <li>Duplicate slashes are merged, and a path is forced to start with a slash. Neither is in the
 *       specification; both come from Vert.x and are kept so the behaviour matches the rest of the
 *       stack.
 *   <li>A malformed percent sequence has no normalized form. {@link #normalize(String)} answers
 *       {@code null} for it rather than throwing, leaving the caller to reject the request.
 * </ul>
 *
 * @author GraviteeSource Team
 */
public final class RequestPathNormalizer {

    private static final char SEGMENT_SEPARATOR = '/';
    private static final String ROOT = "/";

    private RequestPathNormalizer() {}

    /**
     * @param path the raw request path
     * @return the normalized path, the very same instance when there is nothing to resolve so
     *     callers can rely on {@code ==}, or {@code null} when the path carries a malformed percent
     *     sequence and therefore cannot be normalized at all.
     */
    public static String normalize(final String path) {
        if (path == null) {
            return null;
        }
        if (path.isEmpty()) {
            return ROOT;
        }

        final int firstPercent = path.indexOf('%');
        if (firstPercent == -1 && path.indexOf('.') == -1 && path.indexOf("//") == -1 && path.charAt(0) == SEGMENT_SEPARATOR) {
            return path;
        }

        try {
            final String normalized = normalizeSlow(path, firstPercent);
            return normalized.equals(path) ? path : normalized;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String normalizeSlow(final String path, int firstPercent) {
        final StringBuilder buffer;

        if (path.charAt(0) != SEGMENT_SEPARATOR) {
            buffer = new StringBuilder(path.length() + 1);
            buffer.append(SEGMENT_SEPARATOR);
            if (firstPercent != -1) {
                firstPercent++;
            }
        } else {
            buffer = new StringBuilder(path.length());
        }
        buffer.append(path);

        if (firstPercent != -1) {
            decodeUnreservedChars(buffer, firstPercent);
        }
        return removeDotSegments(buffer);
    }

    /**
     * RFC 3986 §5.2.4, plus the merging of duplicate slashes inherited from Vert.x.
     */
    private static String removeDotSegments(CharSequence path) {
        final StringBuilder out = new StringBuilder(path.length());
        int i = 0;

        while (i < path.length()) {
            if (matches(path, i, "./")) {
                i += 2;
            } else if (matches(path, i, "../")) {
                i += 3;
            } else if (matches(path, i, "/./")) {
                // Preserve the trailing slash.
                i += 2;
            } else if (matches(path, i, "/.", true)) {
                path = ROOT;
                i = 0;
            } else if (matches(path, i, "/../")) {
                i += 3;
                removeLastSegment(out);
            } else if (matches(path, i, "/..", true)) {
                path = ROOT;
                i = 0;
                removeLastSegment(out);
            } else if (matches(path, i, ".", true) || matches(path, i, "..", true)) {
                break;
            } else {
                if (path.charAt(i) == SEGMENT_SEPARATOR) {
                    i++;
                    // Not standard, but every hop around us collapses "//" into "/".
                    if (out.length() == 0 || out.charAt(out.length() - 1) != SEGMENT_SEPARATOR) {
                        out.append(SEGMENT_SEPARATOR);
                    }
                }
                final int nextSlash = indexOfSlash(path, i);
                if (nextSlash != -1) {
                    out.append(path, i, nextSlash);
                    i = nextSlash;
                } else {
                    out.append(path, i, path.length());
                    break;
                }
            }
        }
        return out.toString();
    }

    private static void removeLastSegment(final StringBuilder out) {
        final int lastSlash = out.lastIndexOf(ROOT);
        out.setLength(lastSlash == -1 ? 0 : lastSlash);
    }

    private static boolean matches(final CharSequence path, final int start, final String expected) {
        return matches(path, start, expected, false);
    }

    private static boolean matches(final CharSequence path, final int start, final String expected, final boolean exact) {
        if (exact && path.length() - start != expected.length()) {
            return false;
        }
        if (path.length() - start < expected.length()) {
            return false;
        }
        for (int i = 0; i < expected.length(); i++) {
            if (path.charAt(start + i) != expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int indexOfSlash(final CharSequence path, final int start) {
        for (int i = start; i < path.length(); i++) {
            if (path.charAt(i) == SEGMENT_SEPARATOR) {
                return i;
            }
        }
        return -1;
    }

    private static void decodeUnreservedChars(final StringBuilder path, int start) {
        while (start < path.length()) {
            if (path.charAt(start) == '%') {
                decodeUnreserved(path, start);
            }
            start++;
        }
    }

    /**
     * Decodes one escape sequence when it spells an unreserved character, RFC 3986 §2.3. Anything
     * reserved is left encoded, which is what keeps {@code %2F} from becoming a separator.
     *
     * @throws IllegalArgumentException when the sequence is truncated or not hexadecimal
     */
    private static void decodeUnreserved(final StringBuilder path, final int start) {
        if (start + 3 > path.length()) {
            throw new IllegalArgumentException("Invalid position for escape character: " + start);
        }

        final String escapeSequence = path.substring(start + 1, start + 3);
        final int unescaped;
        try {
            unescaped = Integer.parseInt(escapeSequence, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid escape sequence: %" + escapeSequence, e);
        }
        if (unescaped < 0) {
            throw new IllegalArgumentException("Invalid escape sequence: %" + escapeSequence);
        }

        if (isUnreserved(unescaped)) {
            path.setCharAt(start, (char) unescaped);
            path.delete(start + 1, start + 3);
        }
    }

    private static boolean isUnreserved(final int octet) {
        return (
            (octet >= 0x41 && octet <= 0x5A) || // A-Z
            (octet >= 0x61 && octet <= 0x7A) || // a-z
            (octet >= 0x30 && octet <= 0x39) || // 0-9
            octet == 0x2D || // -
            octet == 0x2E || // .
            octet == 0x5F || // _
            octet == 0x7E // ~
        );
    }
}
