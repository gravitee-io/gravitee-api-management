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
 *   <li>A dot segment carrying path parameters, {@code ..;x}, is treated as a dot segment. RFC 3986
 *       says it is an ordinary segment; the Servlet specification says a container strips
 *       {@code ;params} first, which makes it a dot segment. Tomcat, Jetty and Spring follow the
 *       latter. The gateway takes the most permissive reading any receiver could, because being
 *       stricter than the receiver costs a refused request while being laxer re-opens the very
 *       bypass this class closes. See {@link #stripDotSegmentParameters(StringBuilder)}.
 * </ul>
 *
 * <p><b>What the modes assume of the receiver.</b> The rules above describe a receiver that decodes
 * percent sequences once, per RFC 3986 §2.3, and treats only {@code /} as a separator. Reserved
 * characters are deliberately left encoded, so {@code /a/..%2f../b} and {@code /a/%252e%252e/b} are
 * canonical here and are neither resolved nor refused. A receiver configured to decode {@code %2F}
 * into a separator (nginx with a URI in {@code proxy_pass}, Apache with {@code AllowEncodedSlashes
 * On}) or to accept {@code \} as one resolves paths this class does not, and neither mode protects
 * against that. {@code REJECT} refuses paths that are not canonical; it is not traversal hardening
 * for an arbitrary receiver.
 *
 * @author GraviteeSource Team
 */
public final class RequestPathNormalizer {

    private static final char SEGMENT_SEPARATOR = '/';
    private static final char PARAM_SEPARATOR = ';';
    private static final String ROOT = "/";

    private RequestPathNormalizer() {}

    /**
     * Answers, in a single pass and without allocating, whether normalizing this path would change
     * anything. It is the cheap question that both modes actually ask.
     *
     * <p>{@code REJECT} needs nothing else: a path that needs normalizing is not in the form it
     * claims to be, and is refused without ever being rewritten. {@code NORMALIZE} uses it as its
     * fast path, so an ordinary request pays one scan instead of a full resolution.
     *
     * <p>A path needs normalizing when any of these holds:
     *
     * <ul>
     *   <li>it is empty, or does not start with {@code /} — {@link #normalize(String)} forces both
     *   <li>it contains two consecutive separators, which are merged
     *   <li>one of its segments is exactly {@code .} or {@code ..}
     *   <li>it carries a percent sequence that decodes to an <b>unreserved</b> character, which is
     *       decoded. This is what catches {@code %2e} and its variants wherever they sit, so the
     *       segment test above only has to look at plain dots
     *   <li>it carries a percent sequence that is truncated or not hexadecimal, which has no
     *       normalized form at all
     * </ul>
     *
     * <p>And, deliberately, a path does <b>not</b> need normalizing merely because it contains a
     * dot inside a segment, {@code /v1/orders/12345.json}, or a percent sequence that decodes to a
     * <b>reserved</b> character, {@code /a/b%2Fc}. Both are extremely common and both are already
     * canonical; treating them as suspect would send the most ordinary traffic down the slow path,
     * and would make {@code REJECT} refuse requests that are perfectly well formed.
     *
     * <p><b>This method and {@link #normalize(String)} must agree.</b> Two implementations of the
     * same rules drift the moment one is touched alone, and the drift is silent: a false negative
     * here is a path that is quietly left unnormalized. That agreement is not maintained by review
     * but asserted, over a generated corpus, by the property {@code needsNormalization(p) ==
     * !p.equals(normalize(p))} in {@code RequestPathNormalizerTest}.
     *
     * @param path the raw request path
     * @return {@code true} when normalizing would change the path, or when it cannot be normalized
     */
    public static boolean needsNormalization(final String path) {
        if (path == null || path.isEmpty() || path.charAt(0) != SEGMENT_SEPARATOR) {
            return true;
        }

        final int length = path.length();
        int i = 0;

        while (i < length) {
            final char c = path.charAt(i);

            if (c == SEGMENT_SEPARATOR) {
                final int segmentStart = i + 1;
                if (segmentStart < length && path.charAt(segmentStart) == SEGMENT_SEPARATOR) {
                    return true;
                }
                if (isDotSegment(path, segmentStart, length)) {
                    return true;
                }
                i = segmentStart;
            } else if (c == '%') {
                if (i + 2 >= length) {
                    return true;
                }
                final int decoded = hexValue(path.charAt(i + 1), path.charAt(i + 2));
                if (decoded < 0 || isUnreserved(decoded)) {
                    return true;
                }
                i += 3;
            } else {
                i++;
            }
        }
        return false;
    }

    /**
     * @return whether the segment starting at {@code start} is a dot segment. Encoded spellings are
     *     not looked for here: they are already caught by the unreserved decoding rule, whatever
     *     their position. A path-parameter suffix does not disqualify one — see
     *     {@link #stripDotSegmentParameters(StringBuilder)}.
     */
    private static boolean isDotSegment(final String path, final int start, final int length) {
        if (start >= length || path.charAt(start) != '.') {
            return false;
        }
        int end = start + 1;
        if (end < length && path.charAt(end) == '.') {
            end++;
        }
        return end == length || path.charAt(end) == SEGMENT_SEPARATOR || path.charAt(end) == PARAM_SEPARATOR;
    }

    /**
     * Drops the path-parameter suffix of any segment whose content is exactly {@code .} or
     * {@code ..}, so that {@link #removeDotSegments(CharSequence)} sees the dot segment the receiver
     * downstream is going to see.
     *
     * <p><b>Why this is not RFC 3986.</b> The specification is clear that a segment may carry
     * parameters after a {@code ;} (§3.3) and that {@code remove_dot_segments} matches only the
     * exact strings {@code .} and {@code ..} (§5.2.4). By that reading {@code ..;x} is an ordinary
     * segment and this method is wrong. The Servlet specification is equally clear that a container
     * strips {@code ;params} from every segment <em>before</em> resolving anything, which is what
     * Tomcat, Jetty and Spring do. By that reading {@code ..;x} is a dot segment.
     *
     * <p>Both are right, and that disagreement is the whole bug this class exists to close. A
     * gateway cannot know what sits behind it, so it decides on the most permissive reading any
     * plausible receiver could take. Being stricter than a receiver costs an over-refused request;
     * being laxer authorises one resource and lets another be served, which is the escalation that
     * started this.
     *
     * <p>Only segments that are <em>entirely</em> dots before the {@code ;} are touched, so an
     * ordinary {@code /orders;v=2} or a session id on a real segment is left alone.
     */
    private static void stripDotSegmentParameters(final StringBuilder path) {
        int segmentStart = 0;
        while (segmentStart <= path.length()) {
            int segmentEnd = segmentStart;
            while (segmentEnd < path.length() && path.charAt(segmentEnd) != SEGMENT_SEPARATOR) {
                segmentEnd++;
            }
            int afterDots = segmentStart;
            while (afterDots < segmentEnd && path.charAt(afterDots) == '.') {
                afterDots++;
            }
            final int dots = afterDots - segmentStart;
            if ((dots == 1 || dots == 2) && afterDots < segmentEnd && path.charAt(afterDots) == PARAM_SEPARATOR) {
                path.delete(afterDots, segmentEnd);
                segmentEnd = afterDots;
            }
            segmentStart = segmentEnd + 1;
        }
    }

    /**
     * @return the value of a single <b>ASCII</b> hexadecimal digit, or {@code -1}.
     *     <p>Deliberately not {@link Character#digit(char, int)}, which is Unicode-aware:
     *     {@code Character.digit('٢', 16)} answers 2, so {@code %٢e} would decode to a dot that no
     *     receiver on earth resolves, and the gateway would route somewhere the raw path never
     *     named. A percent sequence is ASCII by definition (RFC 3986 §2.1).
     */
    private static int hexDigit(final char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return -1;
    }

    /**
     * @return the octet the two characters spell, or {@code -1} when they are not hexadecimal.
     *     Shared by both implementations on purpose: when they each had their own reading of what a
     *     hexadecimal digit is, they disagreed on {@code %+41}.
     */
    private static int hexValue(final char high, final char low) {
        final int h = hexDigit(high);
        final int l = hexDigit(low);
        return h < 0 || l < 0 ? -1 : (h << 4) + l;
    }

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
        // After decoding, so that %2e%2e; is stripped too — otherwise this method would itself
        // produce the ..; shape that a servlet receiver resolves and this one would not.
        stripDotSegmentParameters(buffer);
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

        final int unescaped = hexValue(path.charAt(start + 1), path.charAt(start + 2));
        if (unescaped < 0) {
            throw new IllegalArgumentException("Invalid escape sequence: %" + path.substring(start + 1, start + 3));
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
