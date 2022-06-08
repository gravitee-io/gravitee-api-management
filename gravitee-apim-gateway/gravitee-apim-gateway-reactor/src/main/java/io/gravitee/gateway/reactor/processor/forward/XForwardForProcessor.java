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
package io.gravitee.gateway.reactor.processor.forward;

import static io.gravitee.gateway.api.http.HttpHeaderNames.X_FORWARDED_FOR;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class XForwardForProcessor extends AbstractProcessor<ExecutionContext> {

    /**
     * {@link java.util.regex.Pattern} for a comma delimited string
     */
    private static final Pattern COMMA_SEPARATED_VALUES_PATTERN = Pattern.compile(",");

    @Override
    public void handle(ExecutionContext context) {
        final Request request = context.request();

        String xForwardedForHeader = request.headers().get(X_FORWARDED_FOR);

        if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
            String[] xForwardedForValues = commaDelimitedListToStringArray(xForwardedForHeader);

            if (xForwardedForValues.length > 0) {
                String xForwardFor = xForwardedForValues[0];
                int idx = xForwardFor.indexOf(':');
                String[] splits = xForwardFor.split(":");

                xForwardFor = (idx == -1) || (splits.length > 2) ? xForwardFor.trim() : xForwardFor.substring(0, idx).trim();

                // X-Forwarded-For header must be reconstructed to include the gateway host address
                ((MutableExecutionContext) context).request(new XForwardForRequest(request, xForwardFor));

                // And override the remote address provided by container in metrics
                request.metrics().setRemoteAddress(xForwardFor);
            }
        }

        next.handle(context);
    }

    /**
     * Convert a given comma delimited list of regular expressions into an array of {@link String}
     */
    private static String[] commaDelimitedListToStringArray(String commaDelimitedStrings) {
        if (commaDelimitedStrings == null || commaDelimitedStrings.length() == 0) {
            return new String[0];
        }

        return Arrays.stream(COMMA_SEPARATED_VALUES_PATTERN.split(commaDelimitedStrings)).map(String::strip).toArray(String[]::new);
    }
}
