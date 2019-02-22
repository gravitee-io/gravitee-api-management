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
import io.gravitee.common.util.MultiValueMap;
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
        // Do we have a relative or an absolute path ?
        if (target.startsWith(URI_PATH_SEPARATOR)) {
            // Get the first group
            LoadBalancedEndpointGroup group = groupManager.getDefault();

            // Resolve to the next endpoint from group LB
            Endpoint endpoint = group.next();

            return createEndpoint(endpoint, (endpoint != null) ? endpoint.target() +
                    encode(target, serverRequest.parameters()) : null);
        } else {
            if (target.startsWith(GroupReference.REFERENCE_PREFIX) ||
                    target.startsWith(EndpointReference.REFERENCE_PREFIX)) {
                int first = target.indexOf((int) ':');
                int last = target.indexOf((int) ':', first + 1);

                // Get the full reference
                String sRef = target.substring(0, last);
                final Reference reference = referenceRegister.get(sRef);

                // A null reference has been found (unknown reference ?), returning null to the caller
                if (reference == null) {
                    return null;
                }

                // Get next endpoint from reference
                Endpoint endpoint = reference.endpoint();

                String encodedTarget = encode(endpoint.target() + target.substring(last+1), serverRequest.parameters());
                return createEndpoint(endpoint, encodedTarget);
            } else if (target.startsWith(Reference.UNKNOWN_REFERENCE)) {
                return null;
            } else {
                // When the user selected endpoint which is not defined (according to the given target), the gateway
                // is always returning the first endpoints reference and took into account its configuration.
                Collection<Reference> endpoints = referenceRegister.referencesByType(EndpointReference.class);
                Reference reference = endpoints
                        .stream()
                        .filter(endpointEntry -> target.startsWith(endpointEntry.endpoint().target()))
                        .findFirst()
                        .orElse(endpoints.iterator().next());

                return (reference != null) ? createEndpoint(reference.endpoint(), encode(target, serverRequest.parameters())) : null;
            }
        }
    }

    private String encode(String uri, MultiValueMap<String, String> parameters) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        Map<String, List<String>> queryParameters = decoder.parameters();

        // Merge query parameters from user target into incoming request query parameters
        for(Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
            parameters.put(param.getKey(), param.getValue());
        }

        // Path segments must be encoded to avoid bad URI syntax
        String [] segments = decoder.path().split(URI_PATH_SEPARATOR);
        StringBuilder builder = new StringBuilder();

        for(String pathSeg : segments) {
            builder.append(UrlEscapers.urlPathSegmentEscaper().escape(pathSeg)).append(URI_PATH_SEPARATOR);
        }

        return builder.substring(0, builder.length() - 1);
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
