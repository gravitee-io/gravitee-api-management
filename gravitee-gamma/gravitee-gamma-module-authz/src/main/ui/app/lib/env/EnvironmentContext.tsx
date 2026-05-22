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
import { type ReactNode, createContext, useContext } from 'react';

/**
 * Story G1 — current Gravitee environment id, sourced from the host shell.
 *
 * <p>Gravitee Console (the federation host that mounts this remote) decides which
 * environment the admin is currently looking at. This module must respect that
 * choice — every REST call to the authz backend is scoped by env, so a stale or
 * wrong id silently shows the wrong policies / entities to the admin.
 *
 * <p><b>Resolution chain</b> (see <code>resolveEnvironmentId.ts</code>):
 * <ol>
 *   <li>Prop passed from the host's federation entry — the canonical path.</li>
 *   <li><code>?env=&lt;id&gt;</code> URL param — convenience for local dev / debugging.</li>
 *   <li>Fallback to <code>'DEFAULT'</code> with a one-time WARN — Phase 1 default
 *       so the module is usable in standalone <code>nx serve</code> mode.</li>
 * </ol>
 *
 * <p>Tests use {@link EnvironmentProvider} directly with an explicit id — there is
 * no global default in tests. The hook throws when used outside a provider so a
 * forgotten wrapper fails loudly during a test rather than silently returning
 * <code>'DEFAULT'</code> and hiding a regression.
 */
const EnvironmentContext = createContext<string | null>(null);

interface EnvironmentProviderProps {
    readonly environmentId: string;
    readonly children: ReactNode;
}

export function EnvironmentProvider({ environmentId, children }: EnvironmentProviderProps) {
    return <EnvironmentContext.Provider value={environmentId}>{children}</EnvironmentContext.Provider>;
}

/**
 * Returns the current environment id from the surrounding {@link EnvironmentProvider}.
 * Throws when no provider is mounted — that always indicates a bug (missing wrapper
 * in a new entry point or test) rather than a runtime condition we can recover from.
 */
export function useEnvironment(): string {
    const value = useContext(EnvironmentContext);
    if (value === null) {
        throw new Error(
            'useEnvironment() called outside EnvironmentProvider. ' +
                'Wrap your tree (production: federation entry; local dev: LocalDevRoot; ' +
                'tests: render-with-providers helper).',
        );
    }
    return value;
}
