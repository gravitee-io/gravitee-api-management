/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { WebStorageStateStore } from 'oidc-client-ts';

/**
 * A StateStore that removes the `openid` scope from the sign-in state before it is persisted.
 *
 * When the Management API does not expose the IdP `id_token`, its token response carries an empty one, and
 * oidc-client-ts rejects a sign-in whose state scope includes `openid` without a valid ID token. The scope sent to
 * the identity provider is unchanged: the authorization request is built before the state is stored.
 */
export class OpenIdScopeFilteringStateStore extends WebStorageStateStore {
    override set(key: string, value: string): Promise<void> {
        return super.set(key, removeOpenIdScope(value));
    }
}

function removeOpenIdScope(value: string): string {
    let parsed: { scope?: unknown } | null;
    try {
        parsed = JSON.parse(value) as { scope?: unknown } | null;
    } catch {
        // Not a JSON-serialised state: store it as is.
        return value;
    }
    if (typeof parsed?.scope !== 'string') {
        return value;
    }
    const scope = parsed.scope
        .split(' ')
        .filter(name => name !== 'openid')
        .join(' ');
    return JSON.stringify({ ...parsed, scope });
}
