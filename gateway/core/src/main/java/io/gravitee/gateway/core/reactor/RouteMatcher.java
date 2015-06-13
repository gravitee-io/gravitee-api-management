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
package io.gravitee.gateway.core.reactor;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.net.HttpHeaders;
import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.api.Request;
import io.gravitee.model.Api;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RouteMatcher {

    private Registry registry;

    @Autowired
    public RouteMatcher(final Registry registry) {
        this.registry = registry;
    }

    public Api match(final Request request) {
        // Matching rules:
        // - Context Path
        // - Virtual host (using HOST header)
        return FluentIterable.from(registry.listAll()).firstMatch(Predicates.and(
                new Predicate<Api>() {
                    @Override
                    public boolean apply(final Api api) {
                        return request.path().startsWith(api.getPublicURI().getPath());
                    }
                }, new Predicate<Api>() {
                    @Override
                    public boolean apply(Api api) {
                        return api.getPublicURI().getHost().equalsIgnoreCase(request.headers().get(HttpHeaders.HOST));
                    }
                })).orNull();
    }
}
