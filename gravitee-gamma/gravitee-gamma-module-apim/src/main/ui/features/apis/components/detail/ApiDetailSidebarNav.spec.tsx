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
import { DatabaseIcon, ShieldCheckIcon, SparklesIcon } from '@gravitee/graphene-core/icons';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import {
    API_PROXY_NAV_GROUPS,
    ApiDetailSidebarNav,
    withApiScoreEnabled,
    withFederatedRestrictions,
    withMetadataPermission,
    withObservabilityLinks,
    withResponseTemplatesPermission,
    withTcpRestrictions,
} from './ApiDetailSidebarNav';

const GROUPS = API_PROXY_NAV_GROUPS;
const BASE = '/env/apis/abc-123';
const OBSERVABILITY_LINKS = { dashboardHref: '/env/observe/dashboards/http-proxy-overview?q=a', logsHref: '/env/observe/logs?q=a' };

function renderNav(currentPath: string, groups = GROUPS) {
    return render(
        <MemoryRouter initialEntries={[currentPath]}>
            <ApiDetailSidebarNav groups={groups} basePath={BASE} />
        </MemoryRouter>,
    );
}

// ─── Nav structure ────────────────────────────────────────────────────────────

describe('API_PROXY_NAV_GROUPS', () => {
    // The canonical Gamma API-detail IA (FOUND-304). Every API object type — an LLM proxy, a Kafka
    // Service, a Message API — uses these groups in this order; Observability joins at render time.
    it('declares the canonical groups, in order', () => {
        expect(GROUPS.map(g => g.label)).toEqual(['General', 'Design', 'Consumers', 'Monitoring', 'Operations']);
    });

    it('opens Design with Entrypoints, Policy Studio, Endpoints', () => {
        const design = GROUPS.find(g => g.label === 'Design')!;
        expect(design.items.slice(0, 3).map(i => i.path)).toEqual(['entrypoints', 'policy-studio', 'endpoints/list']);
    });

    it('names the deployment screens Sharding Tags and Deployment History', () => {
        const operations = GROUPS.find(g => g.label === 'Operations')!;
        expect(operations.items.map(i => [i.label, i.path])).toEqual([
            ['Sharding Tags', 'deployment/configuration'],
            ['Deployment History', 'deployment/history'],
            ['Reporter Settings', 'reporter-settings'],
        ]);
    });

    it('keeps General to what identifies the API, User Permissions included', () => {
        const general = GROUPS.find(g => g.label === 'General')!;
        expect(general.items.map(i => i.path)).toEqual(['overview', 'general', 'user-permissions', 'authorization', 'metadata']);
        expect(general.items.find(i => i.path === 'general')!.label).toBe('Settings');
    });

    it('uses SparklesIcon for API Score so CORS can keep ShieldCheckIcon', () => {
        const apiScore = GROUPS.find(g => g.label === 'Monitoring')!.items.find(item => item.path === 'api-score')!;
        const cors = GROUPS.find(g => g.label === 'Design')!.items.find(item => item.path === 'cors')!;
        expect(apiScore.icon).toBe(SparklesIcon);
        expect(cors.icon).toBe(ShieldCheckIcon);
        expect(apiScore.comingSoon).toBeUndefined();
    });

    it('uses the same Metadata icon as Platform Environment metadata', () => {
        const metadata = GROUPS.find(g => g.label === 'General')!.items.find(item => item.path === 'metadata')!;
        expect(metadata.icon).toBe(DatabaseIcon);
    });
});

// ─── Flat nav links ───────────────────────────────────────────────────────────

describe('ApiDetailSidebarNav — flat links', () => {
    it('renders a leaf link with the correct href', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.getByRole('link', { name: /^overview$/i })).toHaveAttribute('href', `${BASE}/overview`);
    });

    it('renders all group section headings', () => {
        renderNav(`${BASE}/overview`);
        for (const group of GROUPS) {
            expect(screen.getAllByText(group.label).length).toBeGreaterThanOrEqual(1);
        }
    });

    it('renders the Resources link with the correct href', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.getByRole('link', { name: /^resources$/i })).toHaveAttribute('href', `${BASE}/resources`);
    });

    it('renders the Metadata link with the correct href', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.getByRole('link', { name: /^metadata$/i })).toHaveAttribute('href', `${BASE}/metadata`);
    });

    it('renders API Score as a navigable link', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.getByRole('link', { name: /^api score$/i })).toHaveAttribute('href', `${BASE}/api-score`);
    });

    it('renders the endpoint screens as flat links, not behind a collapsible parent', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.getByRole('link', { name: /^endpoints$/i })).toHaveAttribute('href', `${BASE}/endpoints/list`);
        expect(screen.getByRole('link', { name: /^failover$/i })).toHaveAttribute('href', `${BASE}/endpoints/failover`);
        expect(screen.getByRole('link', { name: /^health check dashboard$/i })).toHaveAttribute(
            'href',
            `${BASE}/endpoints/health-check-dashboard`,
        );
    });

    it('renders "coming soon" items (Authorization) as disabled, non-navigable rows', () => {
        renderNav(`${BASE}/overview`);
        for (const label of ['Authorization']) {
            expect(screen.getByText(label)).toBeInTheDocument();
            expect(screen.queryByRole('link', { name: new RegExp(`^${label}$`, 'i') })).not.toBeInTheDocument();
        }
    });

    it('renders the Response Templates link with the correct href', () => {
        renderNav(`${BASE}/overview`);
        expect(screen.getByRole('link', { name: /^response templates$/i })).toHaveAttribute('href', `${BASE}/response-templates`);
    });

    it('makes "coming soon" rows reachable by keyboard, with their reason exposed for assistive tech', () => {
        renderNav(`${BASE}/overview`);
        const row = screen.getByText('Authorization').closest('[tabindex]');
        expect(row).not.toBeNull();
        expect(row).toHaveAttribute('tabindex', '0');
        expect(row).toHaveAttribute('aria-disabled', 'true');
        expect(row).toHaveAttribute('title');
    });
});

// ─── Observability deep links ─────────────────────────────────────────────────

describe('withObservabilityLinks', () => {
    it('inserts the group between Monitoring and Operations', () => {
        const groups = withObservabilityLinks(GROUPS, OBSERVABILITY_LINKS);
        expect(groups.map(g => g.label)).toEqual(['General', 'Design', 'Consumers', 'Monitoring', 'Observability', 'Operations']);
    });

    it('leaves the groups untouched when no deep link could be built', () => {
        expect(withObservabilityLinks(GROUPS, {})).toBe(GROUPS);
    });

    it('opens each entry in a new tab, announcing the change of context', () => {
        renderNav(`${BASE}/overview`, withObservabilityLinks(GROUPS, OBSERVABILITY_LINKS));
        // The external-link icon is decorative, so the accessible name carries the warning instead.
        const dashboard = screen.getByRole('link', { name: 'Dashboard (opens in a new tab)' });
        expect(dashboard).toHaveAttribute('href', OBSERVABILITY_LINKS.dashboardHref);
        expect(dashboard).toHaveAttribute('target', '_blank');
        expect(dashboard).toHaveAttribute('rel', 'noopener noreferrer');
        expect(screen.getByRole('link', { name: 'Logs (opens in a new tab)' })).toHaveAttribute('href', OBSERVABILITY_LINKS.logsHref);
    });
});

// ─── TCP restrictions ─────────────────────────────────────────────────────────

describe('withTcpRestrictions', () => {
    it('returns the groups unchanged when the API has no TCP listeners', () => {
        expect(withTcpRestrictions(GROUPS, false)).toBe(GROUPS);
    });

    it('keeps the canonical FOUND-304 group layout for TCP APIs', () => {
        const restricted = withTcpRestrictions(GROUPS, true);
        expect(restricted.map(g => g.label)).toEqual(['General', 'Design', 'Consumers', 'Monitoring', 'Operations']);
    });

    it('keeps Metadata and Reporter Settings for TCP APIs', () => {
        const paths = withTcpRestrictions(GROUPS, true).flatMap(g => g.items.map(i => i.path));
        expect(paths).toContain('metadata');
        expect(paths).toContain('reporter-settings');
    });

    it('omits HTTP-only Design and Consumer surfaces for TCP APIs', () => {
        const paths = withTcpRestrictions(GROUPS, true).flatMap(g => g.items.map(i => i.path));
        expect(paths).not.toContain('policy-studio');
        expect(paths).not.toContain('cors');
        expect(paths).not.toContain('response-templates');
        expect(paths).not.toContain('endpoints/failover');
        expect(paths).not.toContain('endpoints/health-check-dashboard');
        expect(paths).not.toContain('consumers');
    });

    it('keeps flat deployment and reporter links under Operations', () => {
        const operations = withTcpRestrictions(GROUPS, true).find(g => g.label === 'Operations')!;
        expect(operations.items.map(i => i.path)).toEqual(['deployment/configuration', 'deployment/history', 'reporter-settings']);
    });

    it('drops the whole Observability group for TCP APIs, which have no traffic or logs screen', () => {
        const restricted = withTcpRestrictions(withObservabilityLinks(GROUPS, OBSERVABILITY_LINKS), true);
        expect(restricted.map(g => g.label)).not.toContain('Observability');
    });
});

// ─── Federated restrictions ───────────────────────────────────────────────────

// Spelled out here rather than imported from the production set: a test that reads the implementation's own
// set passes no matter what the set contains.
const FEDERATED_HIDDEN_PATHS = [
    'overview',
    'properties',
    'resources',
    'cors',
    'entrypoints',
    'endpoints/list',
    'endpoints/failover',
    'endpoints/health-check-dashboard',
    'reporter-settings',
    'policy-studio',
    'notifications',
    'alerts',
    'deployment/configuration',
    'deployment/history',
    'authorization',
    'response-templates',
];

const FEDERATED_KEPT_PATHS = ['general', 'plans', 'consumers', 'broadcasts', 'user-permissions', 'audit-logs'];

// Snapshotted at module load, and by value rather than by reference: API_PROXY_NAV_GROUPS is one structure shared
// by every call, so a filter that pruned or re-shaped it in place would leave any expected value read later —
// even one read at the top of a test — already carrying the damage the test is meant to catch.
const SHIPPED_PATHS = GROUPS.flatMap(group => group.items.map(item => item.path));
const SHIPPED_API_SCORE_ITEM = { ...GROUPS.find(group => group.label === 'Monitoring')!.items.find(item => item.path === 'api-score')! };

describe('withFederatedRestrictions', () => {
    it('returns the groups unchanged when the API is not federated', () => {
        expect(withFederatedRestrictions(GROUPS, false)).toBe(GROUPS);
    });

    it('omits every item that does not apply to a federated API', () => {
        const restricted = withFederatedRestrictions(GROUPS, true);
        const allPaths = restricted.flatMap(group => group.items.map(item => item.path));

        for (const path of FEDERATED_HIDDEN_PATHS) {
            expect(allPaths).not.toContain(path);
        }
    });

    it('keeps the items a federated API does have backing data for', () => {
        const restricted = withFederatedRestrictions(GROUPS, true);
        const allPaths = restricted.flatMap(group => group.items.map(item => item.path));

        for (const path of FEDERATED_KEPT_PATHS) {
            expect(allPaths).toContain(path);
        }
    });

    it('leaves no orphaned child routes behind once their parent items are dropped', () => {
        const restricted = withFederatedRestrictions(GROUPS, true);

        expect(restricted.flatMap(group => group.items).flatMap(item => item.children ?? [])).toEqual([]);
    });

    it('keeps API Score exactly as the shipped nav declares it', () => {
        const restricted = withFederatedRestrictions(GROUPS, true);

        expect(restricted.flatMap(group => group.items).find(item => item.path === 'api-score')).toEqual(SHIPPED_API_SCORE_ITEM);
    });

    it('removes only the omitted paths, leaving every other shipped item in place', () => {
        const restricted = withFederatedRestrictions(GROUPS, true);

        expect(restricted.flatMap(group => group.items.map(item => item.path))).toEqual(
            SHIPPED_PATHS.filter(path => !FEDERATED_HIDDEN_PATHS.includes(path)),
        );
    });

    it('keeps exactly the groups that still have items, dropping the ones the omitted items leave empty', () => {
        const restricted = withFederatedRestrictions(GROUPS, true);

        expect(restricted.map(group => group.label)).toEqual(['General', 'Consumers', 'Monitoring']);
    });

    it('leaves the shipped nav intact, so the next natively-managed API still gets every item', () => {
        // API_PROXY_NAV_GROUPS is one module-level structure shared by every render: a filter that pruned it in
        // place would strip these items from every API opened after a federated one, for the rest of the session.
        withFederatedRestrictions(GROUPS, true);

        expect(withFederatedRestrictions(GROUPS, false).flatMap(group => group.items.map(item => item.path))).toEqual(SHIPPED_PATHS);
    });
});

describe('withMetadataPermission', () => {
    it('returns the groups unchanged when the user can read metadata', () => {
        expect(withMetadataPermission(GROUPS, true)).toBe(GROUPS);
    });

    it('omits Metadata from the General group when the user lacks api-metadata-r', () => {
        const restricted = withMetadataPermission(GROUPS, false);
        const general = restricted.find(g => g.label === 'General')!;
        expect(general.items.find(item => item.path === 'metadata')).toBeUndefined();
        expect(general.items.find(item => item.path === 'user-permissions')).toBeDefined();
    });
});

describe('withResponseTemplatesPermission', () => {
    it('returns the groups unchanged when the user can read response templates', () => {
        expect(withResponseTemplatesPermission(GROUPS, true)).toBe(GROUPS);
    });

    it('omits Response Templates from the Design group when the user lacks api-response_templates-r', () => {
        const restricted = withResponseTemplatesPermission(GROUPS, false);
        const design = restricted.find(g => g.label === 'Design')!;
        expect(design.items.find(item => item.path === 'response-templates')).toBeUndefined();
        expect(design.items.find(item => item.path === 'cors')).toBeDefined();
    });
});

describe('withApiScoreEnabled', () => {
    it('returns the groups unchanged when API Score is enabled', () => {
        expect(withApiScoreEnabled(GROUPS, true)).toBe(GROUPS);
    });

    it('omits API Score from the Monitoring group when the portal flag is off', () => {
        const restricted = withApiScoreEnabled(GROUPS, false);
        const monitoring = restricted.find(g => g.label === 'Monitoring')!;
        expect(monitoring.items.find(item => item.path === 'api-score')).toBeUndefined();
        expect(monitoring.items.find(item => item.path === 'notifications')).toBeDefined();
    });
});

describe('ApiDetailSidebarNav — HTTP proxy (master FOUND-304)', () => {
    const HTTP_GROUPS = withObservabilityLinks(GROUPS, OBSERVABILITY_LINKS);

    it('keeps the master Gamma group order when the API has no TCP listeners', () => {
        expect(withTcpRestrictions(HTTP_GROUPS, false)).toBe(HTTP_GROUPS);
        expect(withTcpRestrictions(HTTP_GROUPS, false).map(g => g.label)).toEqual([
            'General',
            'Design',
            'Consumers',
            'Monitoring',
            'Observability',
            'Operations',
        ]);
    });

    it('renders the flat master sidebar with Settings, Design, and Observability groups', () => {
        renderNav(`${BASE}/overview`, HTTP_GROUPS);

        expect(screen.getByText('General')).toBeInTheDocument();
        expect(screen.getByText('Design')).toBeInTheDocument();
        expect(screen.getByText('Consumers')).toBeInTheDocument();
        expect(screen.getByText('Monitoring')).toBeInTheDocument();
        expect(screen.getByText('Observability')).toBeInTheDocument();
        expect(screen.getByText('Operations')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /^settings$/i })).toHaveAttribute('href', `${BASE}/general`);
        expect(screen.getByRole('link', { name: /^policy studio$/i })).toHaveAttribute('href', `${BASE}/policy-studio`);
        expect(screen.getByRole('link', { name: /^subscriptions$/i })).toHaveAttribute('href', `${BASE}/consumers`);
        expect(screen.getByRole('link', { name: /^reporter settings$/i })).toHaveAttribute('href', `${BASE}/reporter-settings`);
        expect(screen.getByRole('link', { name: 'Dashboard (opens in a new tab)' })).toBeInTheDocument();
        expect(screen.queryByText('Gateway')).not.toBeInTheDocument();
        expect(screen.queryByText('Consumer Access')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /^deployment$/i })).not.toBeInTheDocument();
    });
});

describe('ApiDetailSidebarNav — TCP restrictions', () => {
    it('renders the canonical sidebar with HTTP-only items hidden', () => {
        renderNav(`${BASE}/overview`, withTcpRestrictions(GROUPS, true));

        expect(screen.getByText('General')).toBeInTheDocument();
        expect(screen.getByText('Design')).toBeInTheDocument();
        expect(screen.getByText('Consumers')).toBeInTheDocument();
        expect(screen.getByText('Monitoring')).toBeInTheDocument();
        expect(screen.getByText('Operations')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /^metadata$/i })).toHaveAttribute('href', `${BASE}/metadata`);
        expect(screen.getByRole('link', { name: /^reporter settings$/i })).toHaveAttribute('href', `${BASE}/reporter-settings`);
        expect(screen.getByRole('link', { name: /^endpoints$/i })).toHaveAttribute('href', `${BASE}/endpoints/list`);
        expect(screen.queryByRole('link', { name: /^policy studio$/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^subscriptions$/i })).not.toBeInTheDocument();
        expect(screen.queryByText('Gateway')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /^deployment$/i })).not.toBeInTheDocument();
    });
});
