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
package io.gravitee.gateway.reactive.reactor.processor.forward;

import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpBaseRequest;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.reactivex.rxjava3.core.Completable;
import java.net.URI;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XForwardProcessor implements Processor {

    /**
     * {@link Pattern} for a comma delimited string that support whitespace characters
     */
    private static final Pattern COMMA_SEPARATED_VALUES_PATTERN = Pattern.compile("\\s*,\\s*");

    @Override
    public String getId() {
        return "processor-x-forward-for";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            final HttpBaseRequest request = ctx.request();

            ctx.setAttribute(ContextAttributes.ATTR_REQUEST_ORIGINAL_URL, generateOriginalUrl(ctx.request()));

            String xForwardedForHeader = request.headers().get(HttpHeaderNames.X_FORWARDED_FOR);

            if (StringUtils.hasText(xForwardedForHeader)) {
                Arrays.stream(COMMA_SEPARATED_VALUES_PATTERN.split(xForwardedForHeader))
                    .findFirst()
                    .ifPresent(xForwardFor -> {
                        int idx = xForwardFor.indexOf(':');
                        String[] splits = xForwardFor.split(":");

                        xForwardFor = (idx == -1) || (splits.length > 2) ? xForwardFor.trim() : xForwardFor.substring(0, idx).trim();

                        // X-Forwarded-For header must be reconstructed to include the gateway host address
                        ctx.request().remoteAddress(xForwardFor);

                        // And override the remote address provided by container in metrics
                        ctx.metrics().setRemoteAddress(xForwardFor);
                    });
            }
        });
    }

    static String generateOriginalUrl(HttpBaseRequest request) {
        String scheme = request.scheme();
        String originalHost = request.originalHost();
        String uri = request.uri();

        String forwardedHeaderValue = request.headers().get(HttpHeaderNames.FORWARDED);
        String protoHeaderValue = request.headers().get(HttpHeaderNames.X_FORWARDED_PROTO);
        String hostHeaderValue = request.headers().get(HttpHeaderNames.X_FORWARDED_HOST);

        if (!StringUtils.hasText(forwardedHeaderValue) && !StringUtils.hasText(protoHeaderValue) && !StringUtils.hasText(hostHeaderValue)) {
            return scheme + "://" + originalHost + uri;
        }

        if (forwardedHeaderValue != null) {
            String separator = forwardedHeaderValue.contains(";") ? ";" : ",";
            for (String element : forwardedHeaderValue.split(separator)) {
                String normalizedElement = element.toLowerCase().trim();
                if (normalizedElement.startsWith("proto=")) {
                    scheme = element.trim().substring(6);
                }
                if (normalizedElement.startsWith("host=")) {
                    String host = element.trim().substring(5);
                    int commaIndex = host.indexOf(',');
                    originalHost = commaIndex == -1 ? host : host.substring(0, commaIndex).trim();
                }
            }
        } else {
            if (protoHeaderValue != null) {
                scheme = protoHeaderValue;
            }

            if (hostHeaderValue != null) {
                int commaIndex = hostHeaderValue.indexOf(',');
                originalHost = commaIndex == -1 ? hostHeaderValue : hostHeaderValue.substring(0, commaIndex).trim();
            }
        }

        String originalHostWithScheme = scheme + "://" + originalHost;
        if (originalHost.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            originalHostWithScheme = originalHost;
        }

        String portHeaderValue = request.headers().get(HttpHeaderNames.X_FORWARDED_PORT);
        if (portHeaderValue != null) {
            URI hostUri = URI.create(originalHostWithScheme);
            if (
                hostUri.getPort() == -1 &&
                ((scheme.equals("http") && !portHeaderValue.equals("80")) || (scheme.equals("https") && !portHeaderValue.equals("443")))
            ) {
                originalHostWithScheme += ":" + portHeaderValue;
            }
        }

        String prefixHeaderValue = request.headers().get(HttpHeaderNames.X_FORWARDED_PREFIX);
        if (prefixHeaderValue != null) {
            uri = prefixHeaderValue + uri;
        }

        return originalHostWithScheme + uri;
    }
}
