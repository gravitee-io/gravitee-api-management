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

const ALL_MODULES: readonly GammaModule[] = [
    { id: 'apim', name: 'APIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_apim', exposedModule: 'App' },
    { id: 'aim', name: 'AIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_aim', exposedModule: 'App' },
    { id: 'platform', name: 'Platform Management', version: '1.0.0', remoteName: 'gravitee_gamma_module_platform', exposedModule: 'App' },
    { id: 'catalog', name: 'Catalog', version: '1.0.0', remoteName: 'gravitee_gamma_module_catalog', exposedModule: 'App' },
    {
        id: 'authz',
        name: 'Authorization',
        version: '1.0.0',
        remoteName: 'gravitee_gamma_module_authz',
        exposedModule: 'App',
    },
    { id: 'edge', name: 'Edge Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_edge', exposedModule: 'App' },
];

const MANAGEMENT_V2_ENV_PREFIX = `${TEST_CONFIG.managementBaseURL}/v2/environments/`;
const MANAGEMENT_V1_ORG_PREFIX = `${TEST_CONFIG.managementBaseURL}/organizations/${TEST_CONFIG.organizationId}/`;
const GAMMA_ORG_PREFIX = `${TEST_CONFIG.gammaBaseURL}/organizations/${TEST_CONFIG.organizationId}/`;

interface MetricOverrides {
    apiCount?: number | null;
    agentCount?: number | null;
    appCount?: number | null;
    policyCount?: number | null;
    principalCount?: number | null;
    mcpServerCount?: number | null;
    deviceCount?: number | null;
}

interface RouteMatch {
    readonly prefix: string;
    readonly path: string;
    readonly key: keyof MetricOverrides;
    readonly toBody: (value: number) => unknown;
}

const ROUTES: readonly RouteMatch[] = [
    { prefix: MANAGEMENT_V2_ENV_PREFIX, path: '/apis/_search', key: 'apiCount', toBody: n => ({ pagination: { totalCount: n } }) },
    { prefix: GAMMA_ORG_PREFIX, path: '/modules/aim/catalog/agents', key: 'agentCount', toBody: n => ({ pagination: { totalCount: n } }) },
    { prefix: MANAGEMENT_V1_ORG_PREFIX, path: '/applications/_paged', key: 'appCount', toBody: n => ({ page: { total_elements: n } }) },
    { prefix: GAMMA_ORG_PREFIX, path: '/modules/authz/policies', key: 'policyCount', toBody: n => ({ total: n }) },
    { prefix: GAMMA_ORG_PREFIX, path: '/modules/authz/entities', key: 'principalCount', toBody: n => ({ total: n }) },
    { prefix: GAMMA_ORG_PREFIX, path: '/modules/aim/catalog/items', key: 'mcpServerCount', toBody: n => ({ total: n }) },
    {
        prefix: MANAGEMENT_V2_ENV_PREFIX,
        path: '/analytics/facets',
        key: 'deviceCount',
        toBody: n => ({
            metrics: [{ name: 'EDGE_HEARTBEAT_COUNT', buckets: Array.from({ length: n }, (_v, i) => ({ key: `device-${i}` })) }],
        }),
    },
];

function installFetchInterceptor(overrides: MetricOverrides): () => void {
    const originalFetch = window.fetch;
    window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
        const match = ROUTES.find(r => url.startsWith(r.prefix) && url.includes(r.path));
        if (match) {
            const value = overrides[match.key] ?? null;
            const body = value === null ? '{}' : JSON.stringify(match.toBody(value));
            return new Response(body, { status: 200, headers: { 'Content-Type': 'application/json' } });
        }
        return originalFetch(input, init);
    };
    return () => {
        window.fetch = originalFetch;
    };
}

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
}

function teardownStores() {
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

function Decorator({ children, metrics }: { children: React.ReactNode; metrics: MetricOverrides }) {
    const metricsKey = JSON.stringify(metrics);
    useEffect(() => {
        seedStores();
        const restoreFetch = installFetchInterceptor(metrics);
        return () => {
            restoreFetch();
            teardownStores();
        };
    }, [metricsKey]); // eslint-disable-line react-hooks/exhaustive-deps -- stable key avoids teardown loop on object recreation

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
                    'Landing page of the Gamma console. Cards show live per-module metrics via progressive disclosure: modules with data show counts and stats, modules without data show a description and onboarding CTA, and unlicensed modules show a description with no CTA.',
            },
        },
    },
    decorators: [
        (Story, ctx) => {
            const args = ctx.args as StoryArgs;
            return (
                <Decorator metrics={args.metrics ?? {}}>
                    <Story />
                </Decorator>
            );
        },
    ],
};

export default meta;

interface StoryArgs {
    modules?: readonly GammaModule[];
    metrics?: MetricOverrides;
}
type Story = StoryObj<StoryArgs>;

/** All modules active, rich data across the board. */
export const FullAccess: Story = {
    args: {
        modules: [...ALL_MODULES],
        metrics: {
            apiCount: 54,
            agentCount: 12,
            appCount: 8,
            policyCount: 23,
            principalCount: 45,
            mcpServerCount: 6,
            deviceCount: 18,
        },
    },
    render: args => <HomePage modules={args.modules ?? ALL_MODULES} loading={false} error={null} />,
};

/** All modules active but no data — shows empty-state CTAs on every card. */
export const EmptyEnvironment: Story = {
    args: {
        modules: [...ALL_MODULES],
        metrics: {
            apiCount: 0,
            agentCount: 0,
            appCount: 0,
            policyCount: 0,
            principalCount: 0,
            mcpServerCount: 0,
            deviceCount: 0,
        },
    },
    parameters: {
        docs: {
            description: {
                story: 'Fresh environment: all modules are licensed but have no data yet. Each card shows its description and a contextual "Get started" CTA linking to the module onboarding flow.',
            },
        },
    },
    render: args => <HomePage modules={args.modules ?? ALL_MODULES} loading={false} error={null} />,
};

/** Mixed: some modules have data, some are empty, one is unlicensed. */
export const PartialData: Story = {
    args: {
        modules: ALL_MODULES.filter(m => m.id !== 'aim'),
        metrics: { apiCount: 24, agentCount: null, appCount: 0, policyCount: 5, principalCount: 12 },
    },
    parameters: {
        docs: {
            description: {
                story: 'Agent Management is unlicensed (no CTA, disabled card). API Management and Authorization have data (metric view). Platform has no data yet (empty-state CTA).',
            },
        },
    },
    render: args => <HomePage modules={args.modules ?? []} loading={false} error={null} />,
};

/** License-restricted: Agent Management missing. */
export const WithoutAgentManagement: Story = {
    args: {
        modules: ALL_MODULES.filter(m => m.id !== 'aim'),
        metrics: { apiCount: 24, agentCount: null, appCount: 3, policyCount: 10, principalCount: 8 },
    },
    parameters: {
        docs: {
            description: {
                story: 'Agent Management is missing from `/modules` — the card shows its description with no CTA. All other modules show their metrics.',
            },
        },
    },
    render: args => <HomePage modules={args.modules ?? []} loading={false} error={null} />,
};

/** Module fetch failed — error state with retry button. */
export const ErrorState: Story = {
    args: { modules: [], metrics: {} },
    parameters: {
        docs: {
            description: {
                story: 'The module list fetch failed. An inline error alert is shown with a Retry button that re-triggers the fetch.',
            },
        },
    },
    render: () => (
        <HomePage
            modules={[]}
            loading={false}
            error={new Error('Network request failed')}
            onRetry={() => window.alert('Retry triggered')}
        />
    ),
};
