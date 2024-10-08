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
package io.gravitee.gateway.reactive.handlers.api.processor.cors;

import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.processor.Processor;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractCorsRequestProcessor implements Processor {

    static final String ALLOW_ORIGIN_PUBLIC_WILDCARD = "*";

    static final String JOINER_CHAR_SEQUENCE = ", ";

    boolean isOriginAllowed(final Cors cors, final String origin) {
        if (origin == null) {
            return false;
        }

        boolean allowed =
            cors.getAccessControlAllowOrigin().contains(ALLOW_ORIGIN_PUBLIC_WILDCARD) ||
            cors.getAccessControlAllowOrigin().contains(origin);

        if (allowed) {
            return true;
        } else if (cors.getAccessControlAllowOriginRegex() != null && !cors.getAccessControlAllowOriginRegex().isEmpty()) {
            for (Pattern pattern : cors.getAccessControlAllowOriginRegex()) {
                if (pattern.matcher(origin).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected Cors getCors(final HttpPlainExecutionContext ctx) {
        try {
            Api api = ctx.getComponent(io.gravitee.definition.model.v4.Api.class);
            return api
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.HTTP)
                .map(listener -> ((HttpListener) listener).getCors())
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            io.gravitee.definition.model.Api api = ctx.getComponent(io.gravitee.definition.model.Api.class);
            return api.getProxy().getCors();
        }
    }
}
