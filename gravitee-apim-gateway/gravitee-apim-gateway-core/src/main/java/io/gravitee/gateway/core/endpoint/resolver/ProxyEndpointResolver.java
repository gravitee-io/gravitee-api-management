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
package io.gravitee.gateway.core.endpoint.resolver;

import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.api.endpoint.resolver.ProxyEndpoint;
import io.gravitee.gateway.core.endpoint.GroupManager;
import io.gravitee.gateway.core.endpoint.lifecycle.LoadBalancedEndpointGroup;
import io.gravitee.gateway.core.endpoint.ref.EndpointReference;
import io.gravitee.gateway.core.endpoint.ref.Reference;
import io.gravitee.gateway.core.endpoint.ref.ReferenceRegister;
import java.util.Collection;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProxyEndpointResolver implements EndpointResolver {

    private static final String URI_PATH_SEPARATOR = "/";
    private static final String QUERY_SEPARATOR = "?";
    private static final String QUERY_PARAM_SEPARATOR = "&";

    private static final Pattern URI_SCHEME_PATTERN = Pattern.compile("^(https?|wss?|grpcs?)://.*$");

    private final ReferenceRegister referenceRegister;

    private final GroupManager groupManager;

    public ProxyEndpointResolver(ReferenceRegister referenceRegister, GroupManager groupManager) {
        this.referenceRegister = referenceRegister;
        this.groupManager = groupManager;
    }

    @Override
    public ProxyEndpoint resolve(String reference) {
        return (reference != null) ? selectUserDefinedEndpoint(reference) : selectLoadBalancedEndpoint();
    }

    /**
     * The endpoint has not been defined by the user using a policy. This is the default behavior.
     * The resolver must select the next endpoint from the default group (considering that the default group is the
     * first one).
     */
    private ProxyEndpoint selectLoadBalancedEndpoint() {
        // Get the first group
        final LoadBalancedEndpointGroup group = groupManager.getDefault();

        // Resolve to the next endpoint from group LB
        final Endpoint endpoint = group.next();

        return (endpoint != null) ? new DefaultProxyEndpoint(endpoint) : null;
    }

    /**
     * Select an endpoint according to the URI passed in the execution request attribute.
     */
    private ProxyEndpoint selectUserDefinedEndpoint(String uri) {
        // Do we have a relative or an absolute path ?
        if (URI_SCHEME_PATTERN.matcher(uri).matches()) {
            // When the user selected endpoint which is not defined (according to the given uri), the gateway
            // is always returning the first endpoints reference and took into account its configuration.
            Collection<EndpointReference> endpoints = referenceRegister.referencesByType(EndpointReference.class);
            Reference reference = endpoints
                .stream()
                .filter(endpointEntry -> uri.startsWith(endpointEntry.endpoint().target()))
                .findFirst()
                .orElse(endpoints.iterator().next());

            if (reference == null) {
                return null;
            }

            return new UserDefinedProxyEndpoint(reference.endpoint(), uri);
        } else if (uri.startsWith(URI_PATH_SEPARATOR) || StringUtils.isEmpty(uri)) {
            // Get the first group
            LoadBalancedEndpointGroup group = groupManager.getDefault();

            // Resolve to the next endpoint from default group
            final Endpoint endpoint = group.next();

            if (endpoint == null) {
                return null;
            }

            return new UserDefinedProxyEndpoint(endpoint, getMergedTarget(endpoint.target(), uri));
        } else if (uri.startsWith(Reference.UNKNOWN_REFERENCE)) {
            return null;
        } else {
            int refSeparatorIdx = uri.indexOf(':');

            // Get the full reference
            String sRef = uri.substring(0, refSeparatorIdx);
            final Reference reference = referenceRegister.lookup(sRef);

            // A null reference has been found (unknown reference ?), returning null to the caller
            if (reference == null) {
                return null;
            }

            // Get next endpoint from reference
            final Endpoint endpoint = reference.endpoint();

            if (endpoint == null) {
                return null;
            }

            return new UserDefinedProxyEndpoint(endpoint, getMergedTarget(endpoint.target(), uri.substring(refSeparatorIdx + 1)));
        }
    }

    private static String getMergedTarget(String endpointTarget, String userDefinedRawPathAndQuery) {
        int targetQueryIndex = endpointTarget.indexOf(QUERY_SEPARATOR);

        if (targetQueryIndex > -1) {
            // we have to put the query params at the end of the merged url
            String path = endpointTarget.substring(0, targetQueryIndex);
            String targetQuery = endpointTarget.substring(targetQueryIndex + 1);

            String userPathAndQuery;
            int userDefinedQueryIndex = userDefinedRawPathAndQuery.indexOf(QUERY_SEPARATOR);
            if (userDefinedQueryIndex > -1) {
                userPathAndQuery = userDefinedRawPathAndQuery + QUERY_PARAM_SEPARATOR;
            } else {
                userPathAndQuery = userDefinedRawPathAndQuery + QUERY_SEPARATOR;
            }

            return path + userPathAndQuery + targetQuery;
        } else {
            return endpointTarget + userDefinedRawPathAndQuery;
        }
    }
}
