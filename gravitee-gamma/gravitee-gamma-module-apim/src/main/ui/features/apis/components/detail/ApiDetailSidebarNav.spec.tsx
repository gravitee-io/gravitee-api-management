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

    it('opens each entry in a new tab, on the given href', () => {
        renderNav(`${BASE}/overview`, withObservabilityLinks(GROUPS, OBSERVABILITY_LINKS));
        const dashboard = screen.getByRole('link', { name: /^dashboard$/i });
        expect(dashboard).toHaveAttribute('href', OBSERVABILITY_LINKS.dashboardHref);
        expect(dashboard).toHaveAttribute('target', '_blank');
        expect(dashboard).toHaveAttribute('rel', 'noopener noreferrer');
        expect(screen.getByRole('link', { name: /^logs$/i })).toHaveAttribute('href', OBSERVABILITY_LINKS.logsHref);
    });
});

// ─── TCP restrictions ─────────────────────────────────────────────────────────

describe('withTcpRestrictions', () => {
    it('returns the groups unchanged when the API has no TCP listeners', () => {
        expect(withTcpRestrictions(GROUPS, false)).toBe(GROUPS);
    });

    it('marks Policy Studio, CORS, and Response Templates as comingSoon when the API has TCP listeners', () => {
        const restricted = withTcpRestrictions(GROUPS, true);
        const design = restricted.find(g => g.label === 'Design')!;
        const policyStudio = design.items.find(i => i.path === 'policy-studio')!;
        const cors = design.items.find(i => i.path === 'cors')!;
        const responseTemplates = design.items.find(i => i.path === 'response-templates')!;

        expect(policyStudio.comingSoon).toBe(true);
        expect(policyStudio.comingSoonReason).toBe('Coming soon for V4 APIs');
        expect(cors.comingSoon).toBe(true);
        expect(cors.comingSoonReason).toBe('Coming soon for V4 APIs');
        expect(responseTemplates.comingSoon).toBe(true);
        expect(responseTemplates.comingSoonReason).toBe('Coming soon for V4 APIs');
    });

    it('does not affect unrelated items', () => {
        const restricted = withTcpRestrictions(GROUPS, true);
        const plans = restricted.find(g => g.label === 'Consumers')!.items.find(i => i.path === 'plans')!;
        expect(plans.comingSoon).toBeUndefined();
    });

    it('omits Failover and Health Check Dashboard for TCP APIs', () => {
        const paths = withTcpRestrictions(GROUPS, true).flatMap(g => g.items.map(i => i.path));
        expect(paths).not.toContain('endpoints/failover');
        expect(paths).not.toContain('endpoints/health-check-dashboard');
        expect(paths).toContain('endpoints/list');
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

describe('ApiDetailSidebarNav — TCP restrictions', () => {
    it('renders Policy Studio as a disabled row instead of a link for a TCP API', () => {
        renderNav(`${BASE}/overview`, withTcpRestrictions(GROUPS, true));

        expect(screen.getByText('Policy Studio')).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /^policy studio$/i })).not.toBeInTheDocument();
    });
});
