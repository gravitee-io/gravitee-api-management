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
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> revisions = new ConcurrentHashMap<>();

    public boolean shouldApply(String environmentId, String scope, String docId, long updatedAt) {
        ConcurrentMap<String, Long> scopeRevisions = revisions.get(scopeKey(environmentId, scope));
        if (scopeRevisions == null) {
            return true;
        }
        Long applied = scopeRevisions.get(docId);
        return applied == null || updatedAt > applied;
    }

    public void markApplied(String environmentId, String scope, String docId, long updatedAt) {
        revisions.computeIfAbsent(scopeKey(environmentId, scope), k -> new ConcurrentHashMap<>()).merge(docId, updatedAt, Math::max);
    }

    public void forget(String environmentId, String scope, String docId) {
        ConcurrentMap<String, Long> scopeRevisions = revisions.get(scopeKey(environmentId, scope));
        if (scopeRevisions != null) {
            scopeRevisions.remove(docId);
        }
    }

    public void forgetScope(String environmentId, String scope) {
        revisions.remove(scopeKey(environmentId, scope));
    }

    private static String scopeKey(String environmentId, String scope) {
        return environmentId + SEP + scope;
    }
}
