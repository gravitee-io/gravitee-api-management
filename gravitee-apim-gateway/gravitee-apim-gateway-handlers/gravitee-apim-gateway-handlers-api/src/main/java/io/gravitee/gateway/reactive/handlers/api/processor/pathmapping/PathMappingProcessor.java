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
package io.gravitee.gateway.reactive.handlers.api.processor.pathmapping;

import static java.util.Comparator.comparing;

import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathMappingProcessor implements Processor {

    public static final String ID = "processor-path-mapping";

    private PathMappingProcessor() {}

    public static PathMappingProcessor instance() {
        return Holder.INSTANCE;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            Map<String, Pattern> pathMappings = getPathMappings(ctx);

            String path = ctx.request().pathInfo();
            String finalPath = path.endsWith("/") ? path : path + '/';

            pathMappings
                .entrySet()
                .stream()
                .filter(regexMappedPath -> regexMappedPath.getValue().matcher(finalPath).matches())
                .map(Map.Entry::getKey)
                .min(comparing(this::countOccurrencesOf))
                .ifPresent(resolvedMappedPath -> {
                    ctx.metrics().setMappedPath(resolvedMappedPath);
                    ctx.setAttribute(ContextAttributes.ATTR_MAPPED_PATH, resolvedMappedPath);
                });
        });
    }

    private Map<String, Pattern> getPathMappings(final HttpExecutionContextInternal ctx) {
        try {
            io.gravitee.definition.model.v4.Api api = ctx.getComponent(io.gravitee.definition.model.v4.Api.class);
            return api
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.HTTP)
                .map(listener -> ((HttpListener) listener).getPathMappingsPattern())
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            io.gravitee.definition.model.Api api = ctx.getComponent(io.gravitee.definition.model.Api.class);
            return api.getPathMappings();
        }
    }

    private Integer countOccurrencesOf(final String str) {
        return str.length() - str.replace(":", "").length();
    }

    private static class Holder {

        private static final PathMappingProcessor INSTANCE = new PathMappingProcessor();
    }
}
