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

import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.core.endpoint.GroupManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.ref.EndpointReference;
import io.gravitee.gateway.core.endpoint.ref.Reference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import io.gravitee.gateway.core.endpoint.resolver.EndpointResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TargetEndpointResolver implements EndpointResolver {

    // Pattern reuse for duplicate slash removal
    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(https?:|wss?:|grpcs?:))[//]+");

    private static final String URI_PATH_SEPARATOR = "/";

    private static final String QUERY_SEPARATOR = "?";
    private static final String QUERYPARAM_SEPARATOR = "&";

    private static final Pattern URI_SCHEME_PATTERN = Pattern.compile("^(https?|wss?|grpcs?)://.*$");

    @Autowired
    private ReferenceRegister referenceRegister;

    @Autowired
    private GroupManager groupManager;

    public ConnectorEndpoint resolve(ExecutionContext context) {
        // Get target if overridden by a policy
        String targetUri = (String) context.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT);

        return (targetUri != null)
                ? selectUserDefinedEndpoint(context.request(), targetUri)
                : selectLoadBalancedEndpoint(context.request());
    }

    /**
     * The endpoint has not been defined by the user using a policy. This is the default behavior.
     * The resolver must select the next endpoint from the default group (considering that the default group is the
     * first one).
     */
    private ConnectorEndpoint selectLoadBalancedEndpoint(Request serverRequest) {
        // Get the first group
        LoadBalancedEndpointGroup group = groupManager.getDefault();

        // Resolve to the next endpoint from group LB
        Endpoint endpoint = group.next();

        return createEndpoint(endpoint, (endpoint != null) ? endpoint.target() + serverRequest.pathInfo() : null);
    }

    /**
     * Select an endpoint according to the URI passed in the execution request attribute.
     */
    private ConnectorEndpoint selectUserDefinedEndpoint(Request serverRequest, String target) {
        // Do we have a relative or an absolute path ?
        if (URI_SCHEME_PATTERN.matcher(target).matches()) {
            // When the user selected endpoint which is not defined (according to the given target), the gateway
            // is always returning the first endpoints reference and took into account its configuration.
            Collection<EndpointReference> endpoints = referenceRegister.referencesByType(EndpointReference.class);
            Reference reference = endpoints
                    .stream()
                    .filter(endpointEntry -> target.startsWith(endpointEntry.endpoint().target()))
                    .findFirst()
                    .orElse(endpoints.iterator().next());

            if (reference == null) {
                return null;
            }

            mergeQueryParameters(target, serverRequest.parameters());
            return createEndpoint(reference.endpoint(), getTargetWithoutQueryParams(target));
        } else if (target.startsWith(URI_PATH_SEPARATOR) || StringUtils.isEmpty(target)) {
            // Get the first group
            LoadBalancedEndpointGroup group = groupManager.getDefault();

            // Resolve to the next endpoint from group LB
            Endpoint endpoint = group.next();
            if (endpoint == null) {
                return createEndpoint(endpoint, null);
            }

            String fullTarget = getMergedTarget(endpoint.target(), target);
            mergeQueryParameters(fullTarget, serverRequest.parameters());
            return createEndpoint(endpoint, getTargetWithoutQueryParams(fullTarget));
        } else if (target.startsWith(Reference.UNKNOWN_REFERENCE)) {
            return null;
        } else {
            int refSeparatorIdx = target.indexOf(':');

            // Get the full reference
            String sRef = target.substring(0, refSeparatorIdx);
            final Reference reference = referenceRegister.lookup(sRef);

            // A null reference has been found (unknown reference ?), returning null to the caller
            if (reference == null) {
                return null ;
            }

            // Get next endpoint from reference
            Endpoint endpoint = reference.endpoint();
            if (endpoint == null) {
                return null;
            }

            String fullTarget = getMergedTarget(endpoint.target(), target.substring(refSeparatorIdx+1));
            mergeQueryParameters(fullTarget, serverRequest.parameters());
            return createEndpoint(endpoint, getTargetWithoutQueryParams(fullTarget));
        }
    }

    private String getTargetWithoutQueryParams(String uri) {
        int targetQueryIndex = uri.indexOf(QUERY_SEPARATOR);
        if (targetQueryIndex > -1) {
            return uri.substring(0, targetQueryIndex);
        } else {
            return uri;
        }
    }

    private String getMergedTarget(String endpointTarget, String userDefinedRawPathAndQuery) {
        int targetQueryIndex = endpointTarget.indexOf(QUERY_SEPARATOR);

        if (targetQueryIndex > -1) {
            // we have to put the query params at the end of the merged url
            String path = endpointTarget.substring(0, targetQueryIndex);
            String targetQuery = endpointTarget.substring(targetQueryIndex+1);

            String userPathAndQuery;
            int userDefinedQueryIndex = userDefinedRawPathAndQuery.indexOf(QUERY_SEPARATOR);
            if (userDefinedQueryIndex > -1) {
                userPathAndQuery = userDefinedRawPathAndQuery + QUERYPARAM_SEPARATOR;
            } else {
                userPathAndQuery = userDefinedRawPathAndQuery + QUERY_SEPARATOR;
            }

            return path + userPathAndQuery + targetQuery;
        } else {
            return endpointTarget + userDefinedRawPathAndQuery;
        }
    }

    /**
     * Extract query parameters from the uri parameter and copy to the parameters map
     * @param uri
     * @param parameters
     */
    private void mergeQueryParameters(String uri, MultiValueMap<String, String> parameters) {
        MultiValueMap<String, String> queryParameters = URIUtils.parameters(uri);

        // Merge query parameters from user target into incoming request query parameters
        for(Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
            for (String value: param.getValue()) {
                parameters.add(param.getKey(), value);
            }
        }
    }

    private ConnectorEndpoint createEndpoint(Endpoint endpoint, String uri) {
        // Is the endpoint reachable ?
        boolean reachable = uri != null && endpoint != null && endpoint.available();

        if (reachable) {
            // Remove duplicate slash
            final String target = DUPLICATE_SLASH_REMOVER.matcher(uri).replaceAll(URI_PATH_SEPARATOR);

            return new ConnectorEndpoint() {
                @Override
                public String getUri() {
                    return target;
                }

                @Override
                public Connector getConnector() {
                    return endpoint.connector();
                }
            };
        } else {
            return null;
        }
    }
}
