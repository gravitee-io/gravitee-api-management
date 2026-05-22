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
import { AppRoutes } from './app/AppRoutes';
import { EnvironmentProvider } from './app/lib/env/EnvironmentContext';
import { resolveEnvironmentId } from './app/lib/env/resolveEnvironmentId';

/**
 * Module Federation entry. Mounted under the host (Gravitee Console) router and
 * shell layout. Story G1: the host MUST pass <code>environmentId</code> so the
 * module scopes every authz REST call to the env the admin is currently looking
 * at; missing prop falls back to <code>?env=</code> URL param then to
 * <code>'DEFAULT'</code> with a one-time WARN (see <code>resolveEnvironmentId</code>).
 *
 * <p>Props are intentionally narrow: just <code>environmentId</code>. The host
 * already provides router, shell, theme, and i18n at higher levels — we don't
 * re-introduce them here. Future host-injected attributes (organizationId,
 * userId, feature flags) can join this prop bag without breaking the contract.
 */
interface FederationEntryProps {
    readonly environmentId?: string;
}

export default function FederationEntry({ environmentId }: FederationEntryProps) {
    const resolved = resolveEnvironmentId({ hostProp: environmentId });
    return (
        <EnvironmentProvider environmentId={resolved}>
            <AppRoutes />
        </EnvironmentProvider>
    );
}
