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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import io.gravitee.common.util.EnvironmentUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Set of named PDP scopes ({@code environmentId:targetPdpId}) whose engine is provisioned on
 * <strong>this node</strong>. Populated by {@link AuthzPdpSynchronizer} on provision / evict, and
 * read by {@link AbstractAuthzReactorSynchronizer} so a node only stages policies/entities into the
 * scopes it actually hosts — instead of blindly routing to every {@code targetPdpId} a document
 * declares and hitting {@code NO_HANDLERS} on scopes that live on other nodes.
 *
 * <p>The global {@code default} scope (no tag) is always served (the bootstrap engine present on every
 * node). The {@code *} wildcard is expanded by the engine port to {@link #hostedFor(String)} plus
 * {@code default} and routed per-scope, so each node delivers a wildcard document to exactly the engines
 * it hosts.
 *
 * <p>Tag gating mirrors API sharding ({@link io.gravitee.common.util.EnvironmentUtils#hasMatchingTags}):
 * an untagged gateway is a catch-all that serves every scope, a tagged gateway serves a scope whose tag
 * it carries (and is not excluded via {@code !tag}). A tag-scoped default ({@code default@<tag>}) needs no
 * provisioning — the default engine is always present — so it is served from this node's static sharding
 * tags, not from the provisioned {@link #hosted} set.
 */
public class AuthzHostedScopes {

    static final String DEFAULT_SCOPE = "default";
    static final String WILDCARD = "*";
    static final String SCOPE_TAG_SEPARATOR = "@";

    private final Set<String> hosted = ConcurrentHashMap.newKeySet();
    private final Set<String> nodeTags;

    public AuthzHostedScopes() {
        this(Set.of());
    }

    public AuthzHostedScopes(Set<String> nodeTags) {
        this.nodeTags = nodeTags == null ? Set.of() : Set.copyOf(nodeTags);
    }

    private static String key(String environmentId, String targetPdpId) {
        return environmentId + ":" + targetPdpId;
    }

    public void markHosted(String environmentId, String targetPdpId) {
        hosted.add(key(environmentId, targetPdpId));
    }

    public void unmarkHosted(String environmentId, String targetPdpId) {
        hosted.remove(key(environmentId, targetPdpId));
    }

    /** The named scopes (targetPdpIds) whose engine is provisioned on this node for the given environment.
     *  A wildcard ("*") document expands to these (plus the always-on default) so it is routed per-scope to
     *  exactly the engines this node hosts, rather than via a fire-and-forget broadcast. */
    public Set<String> hostedFor(String environmentId) {
        String prefix = environmentId + ":";
        return hosted
            .stream()
            .filter(k -> k.startsWith(prefix))
            .map(k -> k.substring(prefix.length()))
            .collect(Collectors.toSet());
    }

    /**
     * Whether a document targeting {@code scope} should be applied on this node. The scope is
     * "<targetPdpId>@<tag>" when tagged, else the bare targetPdpId. {@code default} and the {@code *}
     * wildcard are always served; a tagged scope additionally requires this node to carry the tag; a named
     * scope requires its engine to be provisioned here.
     */
    public boolean serves(String environmentId, String scope) {
        if (WILDCARD.equals(scope)) {
            return true;
        }
        int at = scope.indexOf(SCOPE_TAG_SEPARATOR);
        String base = at >= 0 ? scope.substring(0, at) : scope;
        String tag = at >= 0 ? scope.substring(at + SCOPE_TAG_SEPARATOR.length()) : null;
        // The global default engine (default scope, no tag) is the always-on bootstrap on every node.
        if (DEFAULT_SCOPE.equals(base) && tag == null) {
            return true;
        }
        // The default engine is always present; a named engine must be provisioned here.
        boolean baseHosted = DEFAULT_SCOPE.equals(base) || hosted.contains(key(environmentId, base));
        // Tag gating mirrors API sharding (EnvironmentUtils#hasMatchingTags): an untagged node is a
        // catch-all, a tagged node serves a scope whose tag it carries (and is not excluded via "!tag").
        Set<String> scopeTags = tag == null ? Set.of() : Set.of(tag);
        return baseHosted && EnvironmentUtils.hasMatchingTags(Optional.of(List.copyOf(nodeTags)), scopeTags);
    }
}
