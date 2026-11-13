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
package fakes.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;

/**
 * An {@link ObjectProvider} over a list a test can still change after the service under test was built — which is
 * what a plugin contributing a bean to an already refreshed context does. Reading it resolves the beans every time,
 * like the real provider.
 *
 * @author GraviteeSource Team
 */
public class MutableObjectProvider<T> implements ObjectProvider<T> {

    private final List<T> beans = new ArrayList<>();

    @SafeVarargs
    public static <T> MutableObjectProvider<T> of(T... beans) {
        var provider = new MutableObjectProvider<T>();
        provider.beans.addAll(List.of(beans));
        return provider;
    }

    public MutableObjectProvider<T> add(T bean) {
        beans.add(bean);
        return this;
    }

    public MutableObjectProvider<T> clear() {
        beans.clear();
        return this;
    }

    @Override
    public Stream<T> stream() {
        return List.copyOf(beans).stream();
    }

    @Override
    public T getObject() {
        throw new UnsupportedOperationException("The beans of this provider are only ever read as a collection");
    }
}
