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
 * Indexes host buckets by their characters, read backwards, so that the acceptors whose host is a
 * suffix of the request host are found from the longest match to the shortest.
 *
 * <p>This mirrors the order {@code OverlappingHttpAcceptor} sorts by, which compares reversed hosts in
 * descending order: if a stored host is a suffix of the request host, its reverse is a prefix of the
 * request host's reverse, so the longer stored host always sorts first. A trie of domain labels would
 * not do: a wildcard declared {@code *gravitee.io} is stored as {@code gravitee.io} and matched with a
 * raw {@code endsWith}, so it also matches {@code mygravitee.io}. Only a character trie reproduces that.
 *
 * <p>Exact and wildcard acceptors that end up storing the same host share the same bucket, which is
 * what the reversed-host comparison does today: their sort keys are equal, so they arbitrate by path.
 *
 * <p>Descent is done on the lower-cased host while the real predicate is case-sensitive for wildcards.
 * That can only widen the candidate set, never narrow it, and every candidate is confirmed by
 * {@link HttpAcceptor#accept(String, String, String)} before being returned.
 *
 * <p>This class is built once and then read; it is not safe for concurrent mutation.
 *
 * @author GraviteeSource Team
 */
public class ReverseHostTrie {

    private final Node root = new Node();
    private int size;

    /**
     * Returns the path index of the given host, creating it on first use.
     *
     * @param host the acceptor host, as stored by the acceptor once its wildcard marker is removed
     */
    public PathSegmentTrie computeIfAbsent(final String host) {
        String lowerCased = host.toLowerCase();
        Node node = root;
        for (int i = lowerCased.length() - 1; i >= 0; i--) {
            node = node.child(lowerCased.charAt(i));
        }
        if (node.paths == null) {
            node.paths = new PathSegmentTrie();
            size++;
        }
        return node.paths;
    }

    /**
     * Resolves against the bucket with the longest matching host first.
     *
     * @param lowerCasedHost the request host, lower-cased and stripped of its port, used to walk the trie
     * @param host the request host as received, handed untouched to the acceptors so their own predicate is unchanged
     */
    public HttpAcceptor resolve(final String lowerCasedHost, final String host, final String path, final String serverId) {
        return resolve(root, lowerCasedHost, lowerCasedHost.length() - 1, host, path, serverId);
    }

    /**
     * Recursion is what gives longest-first for free: the deepest bucket is reached before any bucket is
     * tried, and the stack unwinds from the longest stored host to the shortest.
     */
    private HttpAcceptor resolve(
        final Node node,
        final String lowerCasedHost,
        final int index,
        final String host,
        final String path,
        final String serverId
    ) {
        if (index >= 0) {
            Node child = node.get(lowerCasedHost.charAt(index));
            if (child != null) {
                HttpAcceptor longer = resolve(child, lowerCasedHost, index - 1, host, path, serverId);
                if (longer != null) {
                    return longer;
                }
            }
        }
        if (node.paths != null) {
            return node.paths.resolveLongest(host, path, serverId);
        }
        return null;
    }

    public int size() {
        return size;
    }

    /**
     * Children are held in parallel arrays scanned linearly rather than in a map: the alphabet of a host
     * name is small, so a branch never holds more than a few dozen children, and a linear scan over a
     * compact {@code char[]} beats a map lookup while allocating nothing.
     */
    private static final class Node {

        private char[] characters = new char[4];
        private Node[] children = new Node[4];
        private int count;
        private PathSegmentTrie paths;

        private Node get(final char character) {
            for (int i = 0; i < count; i++) {
                if (characters[i] == character) {
                    return children[i];
                }
            }
            return null;
        }

        private Node child(final char character) {
            Node existing = get(character);
            if (existing != null) {
                return existing;
            }
            if (count == characters.length) {
                char[] grownCharacters = new char[count << 1];
                Node[] grownChildren = new Node[count << 1];
                System.arraycopy(characters, 0, grownCharacters, 0, count);
                System.arraycopy(children, 0, grownChildren, 0, count);
                characters = grownCharacters;
                children = grownChildren;
            }
            Node created = new Node();
            characters[count] = character;
            children[count] = created;
            count++;
            return created;
        }
    }
}
