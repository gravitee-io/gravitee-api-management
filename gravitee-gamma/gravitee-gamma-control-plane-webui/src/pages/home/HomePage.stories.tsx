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
import type { Meta, StoryObj } from '@storybook/react';
import { useEffect } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { HomePage } from './HomePage';
import { useAuthStore } from '../../features/auth/auth.store';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import type { GammaModule } from '../../features/modules';
import { useBootstrapStore } from '../../shared/config/bootstrap.store';
import { buildEnvironment, buildUser, TEST_CONFIG } from '../../testing/factories';

/**
 * Reference catalog of modules a fully-licensed Gamma deployment exposes via
 * `GET /organizations/{orgId}/modules`. Stories pick subsets of this list to mimic
 * license tiers (e.g. a customer without the Agents pack). Shape is the **parsed**
 * `GammaModule` because the page receives the parsed list as a prop (parsing happens
 * upstream in `useGammaModules`, not in the story).
 */
const ALL_MODULES: readonly GammaModule[] = [
    { id: 'apim', name: 'APIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_apim', exposedModule: 'App' },
    { id: 'aim', name: 'AIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_aim', exposedModule: 'App' },
    { id: 'platform', name: 'Platform', version: '1.0.0', remoteName: 'gravitee_gamma_module_platform', exposedModule: 'App' },
    { id: 'catalog', name: 'Catalog', version: '1.0.0', remoteName: 'gravitee_gamma_module_catalog', exposedModule: 'App' },
    {
        id: 'authorization',
        name: 'Authorization',
        version: '1.0.0',
        remoteName: 'gravitee_gamma_module_authorization',
        exposedModule: 'App',
    },
];

/**
 * URLs the decorator intercepts. We match by full prefix against the storybook stub
 * `http://api.test/...` instead of a loose `includes()` so that Storybook's own JSON
 * loads or third-party fetches that happen to contain `apis/_search` don't get
 * short-circuited.
 *
 * NB — `/modules` is NOT intercepted: HomePage receives `modules` as a prop from
 * AppRoutes, so the only fetches we need to mock are the two count endpoints.
 */
const APIS_SEARCH_URL_PREFIX = `${TEST_CONFIG.managementBaseURL}/v2/environments/`;
const AGENTS_URL_PREFIX = `${TEST_CONFIG.gammaBaseURL}/organizations/${TEST_CONFIG.organizationId}/modules/aim/identity/`;

/**
 * Intercepts the two count fetches and returns deterministic responses so badges render
 * exactly the story we describe. Falls through to the real fetch for anything else.
 *
 * Returns a `restore()` that puts `window.fetch` back. Callers MUST call it in a
 * teardown that runs **after** the home has fetched — a synchronous `queueMicrotask`
 * fires before the component's `useEffect` and the interceptor never sees the real call.
 * We attach the teardown to the React unmount lifecycle instead (see `Decorator`).
 */
function installFetchInterceptor(apiCount: number | null, agentCount: number | null): () => void {
    const originalFetch = window.fetch;
    window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
        if (url.startsWith(APIS_SEARCH_URL_PREFIX) && url.includes('/apis/_search')) {
            const body = apiCount === null ? '{}' : JSON.stringify({ pagination: { totalCount: apiCount } });
            return new Response(body, { status: 200, headers: { 'Content-Type': 'application/json' } });
        }
        if (url.startsWith(AGENTS_URL_PREFIX) && url.includes('/agents')) {
            const body = agentCount === null ? '{}' : JSON.stringify({ items: [], page: 1, perPage: 1, total: agentCount });
            return new Response(body, { status: 200, headers: { 'Content-Type': 'application/json' } });
        }
        return originalFetch(input, init);
    };
    return () => {
        window.fetch = originalFetch;
    };
}

/** localStorage keys the decorator writes — tracked here so the teardown can clean
 *  them up and stories don't leak state between renders. */
const AIM_CONFIG_KEY = 'aim.am-config.v2';

/** Seeds the Zustand stores so the HomePage hooks have what they need to render. */
function seedStores() {
    useAuthStore.setState({ user: buildUser({ firstname: 'John', lastname: 'Doe', displayName: 'John Doe' }) });
    useBootstrapStore.setState({ config: TEST_CONFIG, loading: false, error: null });

    const env = buildEnvironment({ id: 'env-prod-id', hrids: ['production'], name: 'Production' });
    useEnvironmentStore.setState({
        organizationId: TEST_CONFIG.organizationId,
        environmentId: env.id,
        environments: [env],
        currentEnvironment: env,
        loading: false,
        error: null,
        initialized: true,
    });

    // useAgentCount only fetches when the AIM module has previously been configured.
    localStorage.setItem(
        AIM_CONFIG_KEY,
        JSON.stringify({ organizationId: TEST_CONFIG.organizationId, environmentId: env.id, domainId: 'demo-domain' }),
    );
}

/** Reverts everything `seedStores` set up. */
function teardownStores() {
    localStorage.removeItem(AIM_CONFIG_KEY);
    useAuthStore.setState({ user: null });
    useBootstrapStore.setState({ config: null, loading: false, error: null });
    useEnvironmentStore.setState({
        organizationId: '',
        environmentId: '',
        environments: [],
        currentEnvironment: null,
        loading: false,
        error: null,
        initialized: false,
    });
}

/**
 * Wraps the story tree. We use a child component (mounted under `MemoryRouter`) so we
 * can use `useEffect` cleanup to restore `window.fetch` and clear seeded localStorage.
 *
 * Deps list is `[apiCount, agentCount]` only — `modules` is forwarded as a prop to
 * `HomePage` directly and doesn't drive the interceptor. If we later add Storybook
 * controls that mutate `apiCount` / `agentCount` interactively, watch out for
 * Storybook duplicating the args object between updates: ref equality may break and
 * the interceptor would re-install/restore in tight loops. Memoising via `useMemo` or
 * passing a stable `key={...}` to `Decorator` would defuse this. Not an issue today.
 */
function Decorator({ children, apiCount, agentCount }: { children: React.ReactNode; apiCount: number | null; agentCount: number | null }) {
    useEffect(() => {
        seedStores();
        const restoreFetch = installFetchInterceptor(apiCount, agentCount);
        return () => {
            restoreFetch();
            teardownStores();
        };
    }, [apiCount, agentCount]);

    return (
        <MemoryRouter initialEntries={['/environments/production']}>
            <Routes>
                <Route path="/environments/:envHrid" element={<>{children}</>} />
            </Routes>
        </MemoryRouter>
    );
}

const meta: Meta<typeof HomePage> = {
    title: 'Pages/HomePage',
    component: HomePage,
    parameters: {
        layout: 'fullscreen',
        docs: {
            description: {
                component:
                    'Landing page of the Gamma console. Renders the Suggested next steps panel and one card per Gravitee product. Cards are wired to the modules returned by `GET /organizations/{orgId}/modules` (passed in as a prop from `AppRoutes`) — when a module is absent from the response (license missing, deployment missing, anything) the card is rendered without an `Open →` CTA and is not clickable, mirroring the app-switcher behaviour. Counts (e.g. "24 APIs") come from live calls; in stories we mock the underlying fetch to keep things deterministic, but the page itself contains no hard-coded numbers.',
            },
        },
    },
    decorators: [
        (Story, ctx) => {
            const args = ctx.args as StoryArgs;
            return (
                <Decorator apiCount={args.apiCount ?? null} agentCount={args.agentCount ?? null}>
                    <Story />
                </Decorator>
            );
        },
    ],
};

export default meta;

/** Story args — `modules` is a real HomePage prop; the two counts drive the decorator's interceptor. */
interface StoryArgs {
    modules?: readonly GammaModule[];
    apiCount?: number | null;
    agentCount?: number | null;
}
type Story = StoryObj<StoryArgs>;

/**
 * Happy path: licensed customer with the full module catalog. All five module cards
 * (Agent Management, API Management, Platform, Catalog, Authorization) render an active
 * link; Event API Management sits at the bottom as a "Coming soon" placeholder.
 */
export const FullAccess: Story = {
    args: {
        modules: [...ALL_MODULES],
        apiCount: 24,
        agentCount: 12,
    },
    render: args => <HomePage modules={args.modules ?? ALL_MODULES} loading={false} error={null} />,
};

/**
 * License-restricted scenario: the customer's license does not include Agent Management,
 * so the backend doesn't return that module. The card stays visible but is rendered
 * without an `Open →` CTA and is not clickable (same UX pattern as the app switcher).
 */
export const WithoutAgentManagement: Story = {
    args: {
        modules: ALL_MODULES.filter(m => m.id !== 'aim'),
        apiCount: 24,
        agentCount: null,
    },
    parameters: {
        docs: {
            description: {
                story: 'Agent Management is missing from `/modules` — the card is rendered without the `Open →` CTA and the agent-count fetch is skipped.',
            },
        },
    },
    render: args => <HomePage modules={args.modules ?? []} loading={false} error={null} />,
};
