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
package io.gravitee.gateway.jupiter.reactor.processor.forward;

import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.HttpRequest;
import io.gravitee.gateway.jupiter.core.context.MutableHttpRequest;
import io.gravitee.gateway.jupiter.core.processor.Processor;
import io.reactivex.Completable;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XForwardForProcessor implements Processor {

    /**
     * {@link Pattern} for a comma delimited string that support whitespace characters
     */
    private static final Pattern COMMA_SEPARATED_VALUES_PATTERN = Pattern.compile("\\s*,\\s*");

    @Override
    public String getId() {
        return "processor-x-forward-for";
    }

    @Override
    public Completable execute(final HttpExecutionContext ctx) {
        return Completable.fromRunnable(
            () -> {
                final HttpRequest request = ctx.request();

                String xForwardedForHeader = request.headers().get(HttpHeaderNames.X_FORWARDED_FOR);

                if (StringUtils.hasText(xForwardedForHeader)) {
                    Arrays
                        .stream(COMMA_SEPARATED_VALUES_PATTERN.split(xForwardedForHeader))
                        .findFirst()
                        .ifPresent(
                            xForwardFor -> {
                                int idx = xForwardFor.indexOf(':');
                                String[] splits = xForwardFor.split(":");

                                xForwardFor =
                                    (idx == -1) || (splits.length > 2) ? xForwardFor.trim() : xForwardFor.substring(0, idx).trim();

                                // X-Forwarded-For header must be reconstructed to include the gateway host address
                                ((MutableHttpRequest) ctx.request()).remoteAddress(xForwardFor);

                                // And override the remote address provided by container in metrics
                                request.metrics().setRemoteAddress(xForwardFor);
                            }
                        );
                }
            }
        );
    }
}
