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
package io.gravitee.apim.gateway.integration.tests.fake;

import static java.util.Optional.ofNullable;

import io.gravitee.apim.gateway.integration.tests.fake.AddHeadersPolicyConfiguration.Header;
import io.gravitee.apim.gateway.integration.tests.fake.AddHeadersPolicyConfiguration.Scope;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;
import io.reactivex.rxjava3.core.Completable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Stand-in for the transform-headers policy: edits the headers of the request or of the response, the values being
 * Expression Language templates.
 *
 * <p>Implements both the reactive and the legacy policy API so the same assertions hold whichever engine executes
 * the API. Applied in this order: whitelist, removal, addition (replaces the value), append (adds a value).
 */
public class AddHeadersPolicy implements Policy {

    private final AddHeadersPolicyConfiguration configuration;

    public AddHeadersPolicy(AddHeadersPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return "add-headers";
    }

    @Override
    public Completable onRequest(HttpExecutionContext ctx) {
        return configuration.scope() == Scope.REQUEST
            ? transform(ctx.request().headers(), ctx.getTemplateEngine())
            : Completable.complete();
    }

    @Override
    public Completable onResponse(HttpExecutionContext ctx) {
        return configuration.scope() == Scope.RESPONSE
            ? transform(ctx.response().headers(), ctx.getTemplateEngine())
            : Completable.complete();
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext ctx, PolicyChain policyChain) {
        if (configuration.scope() == Scope.REQUEST) {
            transformLegacy(request.headers(), ctx.getTemplateEngine());
        }
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(Request request, Response response, ExecutionContext ctx, PolicyChain policyChain) {
        if (configuration.scope() == Scope.RESPONSE) {
            transformLegacy(response.headers(), ctx.getTemplateEngine());
        }
        policyChain.doNext(request, response);
    }

    private Completable transform(HttpHeaders headers, TemplateEngine engine) {
        return Completable.defer(() -> {
            prune(headers);
            Completable additions = Completable.concat(
                list(configuration.addHeaders())
                    .stream()
                    .map(h -> evaluate(engine, h, value -> headers.set(h.name(), value)))
                    .toList()
            );
            Completable appends = Completable.concat(
                list(configuration.appendHeaders())
                    .stream()
                    .map(h -> evaluate(engine, h, value -> headers.add(h.name(), value)))
                    .toList()
            );
            return additions.andThen(appends);
        });
    }

    private Completable evaluate(TemplateEngine engine, Header header, Consumer<String> apply) {
        return engine.eval(header.value(), String.class).doOnSuccess(apply::accept).ignoreElement();
    }

    private void transformLegacy(HttpHeaders headers, TemplateEngine engine) {
        prune(headers);
        list(configuration.addHeaders()).forEach(h -> headers.set(h.name(), engine.convert(h.value())));
        list(configuration.appendHeaders()).forEach(h -> headers.add(h.name(), engine.convert(h.value())));
    }

    private void prune(HttpHeaders headers) {
        List<String> whitelist = list(configuration.whitelistHeaders());
        if (!whitelist.isEmpty()) {
            Set<String> kept = new HashSet<>();
            whitelist.forEach(name -> kept.add(name.toLowerCase(Locale.ROOT)));
            new HashSet<>(headers.names())
                .stream()
                .filter(name -> !kept.contains(name.toLowerCase(Locale.ROOT)))
                .forEach(headers::remove);
        }
        list(configuration.removeHeaders()).forEach(headers::remove);
    }

    private static <T> List<T> list(List<T> values) {
        return ofNullable(values).orElse(List.of());
    }
}
