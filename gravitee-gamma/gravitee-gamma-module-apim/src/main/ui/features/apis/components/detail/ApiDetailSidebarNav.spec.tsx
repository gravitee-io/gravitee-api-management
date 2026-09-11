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

    it('lists every entry flat — no collapsible parents left', () => {
        expect(GROUPS.flatMap(g => g.items).every(item => !('children' in item))).toBe(true);
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

    it('reorganises TCP APIs to the Gamma Baby group layout', () => {
        const restricted = withTcpRestrictions(GROUPS, true);
        expect(restricted.map(g => g.label)).toEqual([
            'General',
            'Gateway',
            'Consumer Access',
            'Security',
            'Monitoring',
            'Operations',
        ]);
    });

    it('places General settings, properties, resources, notifications, and API Score under General', () => {
        const general = withTcpRestrictions(GROUPS, true).find(g => g.label === 'General')!;
        expect(general.items.map(i => [i.label, i.path])).toEqual([
            ['Overview', 'overview'],
            ['General', 'general'],
            ['API Properties', 'properties'],
            ['Resources', 'resources'],
            ['Notifications', 'notifications'],
            ['API Score', 'api-score'],
        ]);
    });

    it('lists Entrypoints and Endpoints flat under Gateway and omits reporter and policy surfaces', () => {
        const gateway = withTcpRestrictions(GROUPS, true).find(g => g.label === 'Gateway')!;
        expect(gateway.items.map(i => [i.label, i.path])).toEqual([
            ['Entrypoints', 'entrypoints'],
            ['Endpoints', 'endpoints/list'],
        ]);
        expect(gateway.items.every(i => !i.children?.length)).toBe(true);
        expect(gateway.items.some(i => i.path === 'reporter-settings')).toBe(false);
        expect(gateway.items.some(i => i.path === 'policy-studio')).toBe(false);
        expect(gateway.items.some(i => i.path === 'response-templates')).toBe(false);
        expect(gateway.items.some(i => i.path === 'cors')).toBe(false);
    });

    it('keeps Plans and Broadcasts under Consumer Access and omits Subscriptions', () => {
        const consumerAccess = withTcpRestrictions(GROUPS, true).find(g => g.label === 'Consumer Access')!;
        expect(consumerAccess.items.map(i => i.path)).toEqual(['plans', 'broadcasts']);
    });

    it('moves User Permissions to Security and omits Authorization from General', () => {
        const security = withTcpRestrictions(GROUPS, true).find(g => g.label === 'Security')!;
        expect(security.items.map(i => i.path)).toEqual(['user-permissions']);
        const general = withTcpRestrictions(GROUPS, true).find(g => g.label === 'General')!;
        expect(general.items.some(i => i.path === 'authorization')).toBe(false);
    });

    it('omits Documentation and Metadata for TCP APIs', () => {
        const paths = withTcpRestrictions(GROUPS, true).flatMap(g => g.items.map(i => i.path));
        expect(paths).not.toContain('documentation');
        expect(paths).not.toContain('metadata');
        expect(withTcpRestrictions(GROUPS, true).map(g => g.label)).not.toContain('Documentation');
    });

    it('keeps only Alerts and Audit Logs under Monitoring and omits Debug for TCP APIs', () => {
        const monitoring = withTcpRestrictions(GROUPS, true).find(g => g.label === 'Monitoring')!;
        expect(monitoring.items.map(i => i.path)).toEqual(['alerts', 'audit-logs']);
        expect(monitoring.items.some(i => i.path === 'debug')).toBe(false);
    });

    it('nests deployment screens under Operations and renames them to match Gamma Baby', () => {
        const operations = withTcpRestrictions(GROUPS, true).find(g => g.label === 'Operations')!;
        expect(operations.items.map(i => i.path)).toEqual(['deployment-section']);
        expect(operations.items[0].children?.map(c => [c.label, c.path])).toEqual([
            ['Configuration', 'deployment/configuration'],
            ['History', 'deployment/history'],
        ]);
    });

    it('drops the whole Observability group for TCP APIs, which have no traffic or logs screen', () => {
        const restricted = withTcpRestrictions(withObservabilityLinks(GROUPS, OBSERVABILITY_LINKS), true);
        expect(restricted.map(g => g.label)).not.toContain('Observability');
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
    it('renders the TCP-specific layout with flat Gateway links and collapsible Deployment', () => {
        renderNav(`${BASE}/overview`, withTcpRestrictions(GROUPS, true));

        expect(screen.getByText('Gateway')).toBeInTheDocument();
        expect(screen.getByText('Consumer Access')).toBeInTheDocument();
        expect(screen.getByText('Security')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /^general$/i })).toHaveAttribute('href', `${BASE}/general`);
        expect(screen.getByRole('link', { name: /^endpoints$/i })).toHaveAttribute('href', `${BASE}/endpoints/list`);
        expect(screen.queryByText('Documentation')).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^documentation$/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^metadata$/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^debug$/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^reporter settings$/i })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: /^deployment$/i })).toBeInTheDocument();
        expect(screen.queryByText('Policy Studio')).not.toBeInTheDocument();
        expect(screen.queryByText('Response Templates')).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^subscriptions$/i })).not.toBeInTheDocument();
        expect(screen.queryByText('Design')).not.toBeInTheDocument();
    });
});
