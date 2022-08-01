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
package io.gravitee.gateway.reactor.handler.impl;

import io.gravitee.gateway.reactor.handler.HttpAcceptorHandler;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Comparator used to sort {@link HttpAcceptorHandler} in a centralized acceptor collection.
 * The final entrypoint collection is a {@link ConcurrentSkipListSet} which rely on this comparator to add / remove entry keeping entries ordered.
 *
 * Http acceptor are first sorted by host (lower-cased), then in case of equality, by path and, in case of equality in path, the http acceptor priority is used (higher priority first).
 */
public class HttpAcceptorHandlerComparator implements Comparator<HttpAcceptorHandler> {

    @Override
    public int compare(HttpAcceptorHandler o1, HttpAcceptorHandler o2) {
        if (o1.equals(o2)) {
            return 0;
        }

        final int hostCompare = Objects.compare(
            toLower(o1.host()),
            toLower(o2.host()),
            (host1, host2) -> {
                if (host1 == null) {
                    return 1;
                } else if (host2 == null) {
                    return -1;
                }
                return host1.compareTo(host2);
            }
        );

        if (hostCompare == 0) {
            final int pathCompare = o1.path().compareTo(o2.path());

            if (pathCompare == 0) {
                if (o1.priority() <= o2.priority()) {
                    return 1;
                }
                return -1;
            }

            return pathCompare;
        }
        return hostCompare;
    }

    private String toLower(String value) {
        if (value != null) {
            return value.toLowerCase();
        }
        return value;
    }
}
