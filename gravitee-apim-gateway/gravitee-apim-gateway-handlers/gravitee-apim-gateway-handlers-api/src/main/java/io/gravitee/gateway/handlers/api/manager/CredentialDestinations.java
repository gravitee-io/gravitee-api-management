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
package io.gravitee.gateway.handlers.api.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpoint;
import io.gravitee.definition.model.v4.endpointgroup.AbstractEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Where the credentials an API references can be sent: the origins of the endpoints whose configuration references each
 * one. Worked out once from the deployed definition, because the gateway cannot tell which endpoint is asking while a
 * reference is resolved.
 *
 * <p>An endpoint only has an origin with a literal http(s) target and no proxy of its own. A target built with an
 * expression is evaluated at deployment against values that change without one, and a proxy can send the request
 * anywhere. A reference outside an endpoint or group configuration could be resolved by any plugin field marked secret,
 * so it leaves every credential of the API unreachable. An endpoint whose reference does not spell out the credential id
 * counts for every credential.
 */
public final class CredentialDestinations {

    // Whitespace between '#' and the name may be raw, or JSON-escaped once or more in the serialized definition.
    private static final Pattern MENTION = Pattern.compile("#(?:\\s|\\\\+(?:[nrtbf]|u[0-9a-fA-F]{4}))*credentials\\b");
    private static final Pattern LITERAL_REFERENCE = Pattern.compile("#credentials\\.get\\(\\s*'([^']+)'\\s*,");
    private static final ObjectMapper MAPPER = new GraviteeMapper();
    private static final String NO_ORIGIN = "";
    private static final CredentialDestinations UNREACHABLE = new CredentialDestinations(Map.of(), Set.of(), true);

    private final Map<String, Set<String>> originsByCredential;
    private final Set<String> originsOfEveryCredential;
    private final boolean referencedOutsideEndpoints;

    private CredentialDestinations(
        Map<String, Set<String>> originsByCredential,
        Set<String> originsOfEveryCredential,
        boolean referencedOutsideEndpoints
    ) {
        this.originsByCredential = originsByCredential;
        this.originsOfEveryCredential = originsOfEveryCredential;
        this.referencedOutsideEndpoints = referencedOutsideEndpoints;
    }

    /** @param definition the deployed definition; {@code null} leaves every credential unreachable */
    public static CredentialDestinations of(AbstractApi definition) {
        if (definition == null) {
            return UNREACHABLE;
        }
        int mentionsInDefinition;
        try {
            mentionsInDefinition = countMentions(MAPPER.writeValueAsString(definition));
        } catch (JsonProcessingException e) {
            return UNREACHABLE;
        }

        var originsByCredential = new HashMap<String, Set<String>>();
        var originsOfEveryCredential = new HashSet<String>();
        int mentionsInEndpoints = 0;
        for (AbstractEndpointGroup<? extends AbstractEndpoint> group : endpointGroupsOf(definition)) {
            String groupConfiguration = group.getSharedConfiguration();
            int groupMentions = countMentions(groupConfiguration);
            mentionsInEndpoints += groupMentions;
            for (AbstractEndpoint endpoint : endpointsOf(group)) {
                int ownMentions = countMentions(endpoint.getConfiguration()) + countMentions(endpoint.getSharedConfigurationOverride());
                mentionsInEndpoints += ownMentions;
                int mentions = groupMentions + ownMentions;
                if (mentions == 0) {
                    continue;
                }
                String origin = originOf(endpoint, groupConfiguration);
                List<String> credentialIds = new ArrayList<>(literalCredentialIds(groupConfiguration));
                credentialIds.addAll(literalCredentialIds(endpoint.getConfiguration()));
                credentialIds.addAll(literalCredentialIds(endpoint.getSharedConfigurationOverride()));
                if (credentialIds.size() < mentions) {
                    originsOfEveryCredential.add(origin);
                }
                credentialIds.forEach(id -> originsByCredential.computeIfAbsent(id, k -> new HashSet<>()).add(origin));
            }
        }
        return new CredentialDestinations(originsByCredential, originsOfEveryCredential, mentionsInDefinition > mentionsInEndpoints);
    }

    /**
     * @param allowedTargets the origins the credential may be sent to
     * @return true when every endpoint referencing the credential sends it to one of those origins
     */
    public boolean onlyReach(String credentialId, Set<String> allowedTargets) {
        if (referencedOutsideEndpoints) {
            return false;
        }
        Set<String> origins = new HashSet<>(originsByCredential.getOrDefault(credentialId, Set.of()));
        origins.addAll(originsOfEveryCredential);
        Set<String> allowedOrigins = allowedTargets
            .stream()
            .map(CredentialDestinations::normalizedOrigin)
            .filter(origin -> !NO_ORIGIN.equals(origin))
            .collect(Collectors.toSet());
        return !origins.isEmpty() && allowedOrigins.containsAll(origins);
    }

    private static List<? extends AbstractEndpointGroup<? extends AbstractEndpoint>> endpointGroupsOf(AbstractApi definition) {
        List<? extends AbstractEndpointGroup<? extends AbstractEndpoint>> groups = switch (definition) {
            case Api api -> api.getEndpointGroups();
            case NativeApi nativeApi -> nativeApi.getEndpointGroups();
            default -> List.of();
        };
        return groups == null ? List.of() : groups;
    }

    private static List<? extends AbstractEndpoint> endpointsOf(AbstractEndpointGroup<? extends AbstractEndpoint> group) {
        return group.getEndpoints() == null ? List.of() : group.getEndpoints();
    }

    private static String originOf(AbstractEndpoint endpoint, String groupConfiguration) {
        if (hasOwnProxy(groupConfiguration) || hasOwnProxy(endpoint.getSharedConfigurationOverride())) {
            return NO_ORIGIN;
        }
        JsonNode target = readTree(endpoint.getConfiguration()).path("target");
        if (!target.isTextual() || target.textValue().contains("{")) {
            return NO_ORIGIN;
        }
        return normalizedOrigin(target.textValue());
    }

    private static boolean hasOwnProxy(String configuration) {
        JsonNode proxy = readTree(configuration).path("proxy");
        return proxy.path("enabled").asBoolean(false) && !proxy.path("useSystemProxy").asBoolean(false);
    }

    /** {@code scheme://host:port} in lower case with the default port filled in, or {@link #NO_ORIGIN}. */
    private static String normalizedOrigin(String url) {
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
            if (uri.getHost() == null || !("http".equals(scheme) || "https".equals(scheme))) {
                return NO_ORIGIN;
            }
            int port = uri.getPort() != -1 ? uri.getPort() : "https".equals(scheme) ? 443 : 80;
            return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
        } catch (IllegalArgumentException e) {
            return NO_ORIGIN;
        }
    }

    private static JsonNode readTree(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return MAPPER.missingNode();
        }
        try {
            return MAPPER.readTree(configuration);
        } catch (JsonProcessingException e) {
            return MAPPER.missingNode();
        }
    }

    private static int countMentions(String text) {
        return text == null ? 0 : (int) MENTION.matcher(text).results().count();
    }

    private static List<String> literalCredentialIds(String configuration) {
        if (configuration == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        Matcher matcher = LITERAL_REFERENCE.matcher(configuration);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }
}
