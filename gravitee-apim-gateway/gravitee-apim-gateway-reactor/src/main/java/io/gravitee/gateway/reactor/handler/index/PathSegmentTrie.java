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
package io.gravitee.gateway.reactor.handler.index;

import io.gravitee.gateway.reactor.handler.HttpAcceptor;

/**
 * Indexes {@link HttpAcceptor} by the segments of their context path, so that resolving a request
 * costs the length of the request instead of the number of deployed acceptors.
 *
 * <p>A path is decomposed into the substrings lying between its {@code '/'} characters, <em>including</em>
 * the one before the first slash. Acceptor paths are normalised by
 * {@code AbstractHttpAcceptor} to always end with a slash, so {@code "/store/"} decomposes to
 * {@code ["", "store"]} and the root {@code "/"} decomposes to {@code [""]}. Keeping the leading empty
 * segment is what makes a path without a leading slash unreachable from a request that has one, which
 * is the behaviour of the plain {@code startsWith} it replaces.
 *
 * <p>The trie only decides <em>which</em> acceptors are worth testing and in which order. Whether an
 * acceptor actually matches is still answered by {@link HttpAcceptor#accept(String, String, String)},
 * so host and server filtering keep exactly the semantics they have today.
 *
 * <p>This class is built once and then read; it is not safe for concurrent mutation. Hot redeployment
 * is out of scope for this proof of concept.
 *
 * @author GraviteeSource Team
 */
public class PathSegmentTrie {

    private static final char URI_PATH_SEPARATOR = '/';

    private final Node root = new Node();
    private int size;

    /**
     * Indexes an acceptor under its normalised path.
     *
     * @param path the acceptor path, as returned by {@link HttpAcceptor#path()}
     */
    public void add(final String path, final HttpAcceptor acceptor) {
        Node node = root;
        int i = 0;
        final int length = path.length();
        while (i < length) {
            int separator = path.indexOf(URI_PATH_SEPARATOR, i);
            int end = separator < 0 ? length : separator;
            node = node.children.computeIfAbsent(path.substring(i, end));
            i = end + 1;
        }
        node.add(acceptor);
        size++;
    }

    /**
     * Returns the acceptor with the <em>shortest</em> matching context path, which is the order the
     * non-overlapping mode sorts its acceptors in. The descent stops at the first match.
     */
    public HttpAcceptor resolveShortest(final String host, final String path, final String serverId) {
        Node node = root;
        int i = 0;
        final int length = path.length();
        while (i < length) {
            int separator = path.indexOf(URI_PATH_SEPARATOR, i);
            int end = separator < 0 ? length : separator;
            node = node.children.get(path, i, end);
            if (node == null) {
                return null;
            }
            HttpAcceptor accepted = node.accept(host, path, serverId);
            if (accepted != null) {
                return accepted;
            }
            i = end + 1;
        }
        return null;
    }

    /**
     * Returns the acceptor with the <em>longest</em> matching context path, which is the order the
     * overlapping mode sorts its acceptors in. The descent goes as deep as the trie allows and keeps
     * the deepest match seen.
     */
    public HttpAcceptor resolveLongest(final String host, final String path, final String serverId) {
        Node node = root;
        HttpAcceptor deepest = null;
        int i = 0;
        final int length = path.length();
        while (i < length) {
            int separator = path.indexOf(URI_PATH_SEPARATOR, i);
            int end = separator < 0 ? length : separator;
            node = node.children.get(path, i, end);
            if (node == null) {
                break;
            }
            HttpAcceptor accepted = node.accept(host, path, serverId);
            if (accepted != null) {
                deepest = accepted;
            }
            i = end + 1;
        }
        return deepest;
    }

    public int size() {
        return size;
    }

    private static final class Node {

        private final SegmentMap children = new SegmentMap();
        private HttpAcceptor[] acceptors;

        private void add(final HttpAcceptor acceptor) {
            if (acceptors == null) {
                acceptors = new HttpAcceptor[] { acceptor };
                return;
            }
            HttpAcceptor[] grown = new HttpAcceptor[acceptors.length + 1];
            System.arraycopy(acceptors, 0, grown, 0, acceptors.length);
            grown[acceptors.length] = acceptor;
            acceptors = grown;
        }

        /**
         * Ties between acceptors sharing the same path are broken by registration order. The sorted list
         * this index replaces leaves that order unspecified, so nothing is lost.
         */
        private HttpAcceptor accept(final String host, final String path, final String serverId) {
            if (acceptors == null) {
                return null;
            }
            for (HttpAcceptor acceptor : acceptors) {
                if (acceptor.accept(host, path, serverId)) {
                    return acceptor;
                }
            }
            return null;
        }
    }

    /**
     * Open-addressed map from a path segment to a child node, able to look up a segment in place.
     *
     * <p>A {@code HashMap} would force a {@code substring} per segment on every request, which would
     * put back one allocation per segment on the hot path. Insertion still allocates, but it only
     * happens at deployment time.
     */
    private static final class SegmentMap {

        private static final int INITIAL_CAPACITY = 8;

        private String[] keys = new String[INITIAL_CAPACITY];
        private Node[] values = new Node[INITIAL_CAPACITY];
        private int size;

        private Node get(final String source, final int start, final int end) {
            int index = hash(source, start, end) & (keys.length - 1);
            while (true) {
                String key = keys[index];
                if (key == null) {
                    return null;
                }
                if (key.length() == end - start && key.regionMatches(0, source, start, end - start)) {
                    return values[index];
                }
                index = (index + 1) & (keys.length - 1);
            }
        }

        private Node computeIfAbsent(final String segment) {
            Node existing = get(segment, 0, segment.length());
            if (existing != null) {
                return existing;
            }
            if ((size + 1) * 2 > keys.length) {
                grow();
            }
            Node created = new Node();
            insert(segment, created);
            size++;
            return created;
        }

        private void insert(final String key, final Node value) {
            int index = hash(key, 0, key.length()) & (keys.length - 1);
            while (keys[index] != null) {
                index = (index + 1) & (keys.length - 1);
            }
            keys[index] = key;
            values[index] = value;
        }

        private void grow() {
            String[] previousKeys = keys;
            Node[] previousValues = values;
            keys = new String[previousKeys.length << 1];
            values = new Node[previousValues.length << 1];
            for (int i = 0; i < previousKeys.length; i++) {
                if (previousKeys[i] != null) {
                    insert(previousKeys[i], previousValues[i]);
                }
            }
        }

        /**
         * {@code String.hashCode} over a range, spread the way {@code HashMap} does so that the high bits
         * survive the power-of-two mask.
         */
        private static int hash(final String source, final int start, final int end) {
            int hash = 0;
            for (int i = start; i < end; i++) {
                hash = 31 * hash + source.charAt(i);
            }
            return hash ^ (hash >>> 16);
        }
    }
}
