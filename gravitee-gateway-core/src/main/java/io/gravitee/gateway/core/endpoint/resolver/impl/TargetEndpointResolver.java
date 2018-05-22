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
package io.gravitee.gateway.core.endpoint.resolver.impl;

import com.google.common.net.UrlEscapers;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.core.endpoint.GroupManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.ref.EndpointReference;
import io.gravitee.gateway.core.endpoint.ref.GroupReference;
import io.gravitee.gateway.core.endpoint.ref.Reference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.endpoint.resolver.EndpointResolver;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TargetEndpointResolver implements EndpointResolver {

    // Pattern reuse for duplicate slash removal
    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:))[//]+");

    private static final String URI_PATH_SEPARATOR = "/";

    @Autowired
    private ReferenceRegister referenceRegister;

    @Autowired
    private GroupManager groupManager;

    public ResolvedEndpoint resolve(Request serverRequest, ExecutionContext executionContext) {
        // Get target if overridden by a policy
        String targetUri = (String) executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT);

        return (targetUri != null)
                ? selectUserDefinedEndpoint(serverRequest, targetUri)
                : selectLoadBalancedEndpoint(serverRequest);
    }

    /**
     * The endpoint has not been defined by the user using a policy. This is the default behavior.
     * The resolver must select the next endpoint from the default group (considering that the default group is the
     * first one).
     */
    private ResolvedEndpoint selectLoadBalancedEndpoint(Request serverRequest) {
        // Get the first group
        LoadBalancedEndpointGroup group = groupManager.getDefault();

        // Resolve to the next endpoint from group LB
        Endpoint endpoint = group.next();

        return createEndpoint(endpoint, (endpoint != null) ? endpoint.target() + serverRequest.pathInfo() : null);
    }

    /**
     * Select an endpoint according to the URI passed in the execution request attribute.
     */
    private ResolvedEndpoint selectUserDefinedEndpoint(Request serverRequest, String target) {
        QueryStringDecoder decoder = new QueryStringDecoder(target);
        Map<String, List<String>> queryParameters = decoder.parameters();

        // Merge query parameters from user target into incoming request query parameters
        for(Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
            serverRequest.parameters().put(param.getKey(), param.getValue());
        }

        // Path segments must be encoded to avoid bad URI syntax
        String [] segments = decoder.path().split(URI_PATH_SEPARATOR);
        StringBuilder builder = new StringBuilder();

        for(String pathSeg : segments) {
            builder.append(UrlEscapers.urlPathSegmentEscaper().escape(pathSeg)).append(URI_PATH_SEPARATOR);
        }

        String encodedTarget = builder.substring(0, builder.length() - 1);

        // Do we have a relative or an absolute path ?
        if (encodedTarget.startsWith(URI_PATH_SEPARATOR)) {
            // Get the first group
            LoadBalancedEndpointGroup group = groupManager.getDefault();

            // Resolve to the next endpoint from group LB
            Endpoint endpoint = group.next();

            return createEndpoint(endpoint, (endpoint != null) ? endpoint.target() + encodedTarget : null);
        } else {
            if (encodedTarget.startsWith(GroupReference.REFERENCE_PREFIX) ||
                    encodedTarget.startsWith(EndpointReference.REFERENCE_PREFIX)) {
                // Get the full reference
                int sep = encodedTarget.indexOf(':');
                String sRef = encodedTarget.substring(0, encodedTarget.indexOf(':', sep + 1));
                Reference reference = referenceRegister.get(sRef);

                // Get next endpoint from reference
                Endpoint endpoint = reference.endpoint();

                return createEndpoint(endpoint, encodedTarget.replaceFirst(sRef + ':', endpoint.target()));
            } else {
                // Try to match an endpoint according to the target URL
                Collection<Reference> endpoints = referenceRegister.referencesByType(EndpointReference.class);
                Reference reference = endpoints
                        .stream()
                        .filter(endpointEntry -> encodedTarget.startsWith(endpointEntry.endpoint().target()))
                        .findFirst()
                        .orElse(endpoints.iterator().next());

                return createEndpoint(reference.endpoint(), encodedTarget);
            }
        }
    }

    private ResolvedEndpoint createEndpoint(Endpoint endpoint, String uri) {
        // Is the endpoint reachable ?
        boolean reachable = uri != null && endpoint != null && endpoint.available();

        if (reachable) {
            // Remove duplicate slash
            final String target = DUPLICATE_SLASH_REMOVER.matcher(uri).replaceAll(URI_PATH_SEPARATOR);

            return new ResolvedEndpoint() {
                @Override
                public String getUri() {
                    return target;
                }

                @Override
                public Connector getConnector() {
                    return endpoint.connector();
                }

                @Override
                public Endpoint getEndpoint() {
                    return endpoint;
                }
            };
        } else {
            return null;
        }
    }
}
