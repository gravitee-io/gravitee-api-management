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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Node-local record of which authz document is applied to which scope, at which {@code event.updatedAt}.
 * The authz analogue of the deployed-API map that {@code ApiManagerImpl} uses to skip an unchanged redeploy.
 */
public class AuthzAppliedRevisions {

    private static final String SEP = "\u001f";
    private static final String SCOPE_TAG_SEPARATOR = "@";
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> revisions = new ConcurrentHashMap<>();

    public boolean shouldApply(String environmentId, String scope, String docId, long updatedAt) {
        // A non-positive updatedAt is an unknown revision (the mappers default a missing event timestamp
        // to 0): never gate it, otherwise a real change carrying no timestamp would be suppressed after the
        // first apply. Such documents keep the pre-gate always-apply behaviour; they are simply not tracked.
        if (updatedAt <= 0) {
            return true;
        }
        ConcurrentMap<String, Long> scopeRevisions = revisions.get(scopeKey(environmentId, scope));
        if (scopeRevisions == null) {
            return true;
        }
        Long applied = scopeRevisions.get(docId);
        return applied == null || updatedAt > applied;
    }

    public void markApplied(String environmentId, String scope, String docId, long updatedAt) {
        if (updatedAt <= 0) {
            return;
        }
        revisions.computeIfAbsent(scopeKey(environmentId, scope), k -> new ConcurrentHashMap<>()).merge(docId, updatedAt, Math::max);
    }

    public void forget(String environmentId, String scope, String docId) {
        ConcurrentMap<String, Long> scopeRevisions = revisions.get(scopeKey(environmentId, scope));
        if (scopeRevisions != null) {
            scopeRevisions.remove(docId);
        }
    }

    /**
     * Forget every revision bucket for an engine, i.e. the bare {@code targetPdpId} and all its
     * {@code targetPdpId@<tag>} routing-scope variants. Called when a PDP is evicted: the engine address is
     * derived from the {@code targetPdpId} alone, so on a catch-all node every tag-variant scope aliases to
     * the one engine being torn down and its bucket is now stale — clearing only the bare id would leave a
     * tagged bucket behind and gate out every doc when the scope is re-provisioned. Walks the outer map, but
     * only on the (rare) evict path; the hot mutation path never touches it.
     */
    public void forgetEngine(String environmentId, String targetPdpId) {
        String bareKey = scopeKey(environmentId, targetPdpId);
        String taggedPrefix = bareKey + SCOPE_TAG_SEPARATOR;
        revisions.remove(bareKey);
        revisions.keySet().removeIf(k -> k.startsWith(taggedPrefix));
    }

    private static String scopeKey(String environmentId, String scope) {
        return environmentId + SEP + scope;
    }
}
