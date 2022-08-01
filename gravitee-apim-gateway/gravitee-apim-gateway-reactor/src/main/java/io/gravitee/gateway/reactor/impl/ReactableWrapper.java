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
package io.gravitee.gateway.reactor.impl;

import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactableWrapper<T> implements Reactable {

    private final T content;

    public ReactableWrapper(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public <D> Set<D> dependencies(Class<D> type) {
        return Collections.emptySet();
    }

    @Override
    public List<HttpAcceptor> httpAcceptors() {
        return Collections.emptyList();
    }
}
