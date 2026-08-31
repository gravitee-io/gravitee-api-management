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

export const AGENT_TABS = ['overview', 'docs', 'subscribe', 'chat'] as const;

export type AgentTab = (typeof AGENT_TABS)[number];

const TAB_LABELS: Record<AgentTab, string> = {
    overview: 'Overview',
    docs: 'Documentation',
    subscribe: 'Subscribe',
    chat: 'Chat',
};

export function isAgentTab(value: string): value is AgentTab {
    return (AGENT_TABS as readonly string[]).includes(value);
}

export function pathForAgentTab(apiId: string, tab: AgentTab): string {
    if (tab === 'overview') {
        return `/catalog/${apiId}`;
    }
    return `/catalog/${apiId}/${tab}`;
}

export function agentTabLabel(tab: AgentTab): string {
    return TAB_LABELS[tab];
}

export function agentDetailBreadcrumbs(
    apiId: string,
    agentName: string,
    tab: AgentTab,
): Array<{ label: string; to?: string }> {
    const crumbs: Array<{ label: string; to?: string }> = [
        { label: 'Home', to: '/' },
        { label: 'Catalog', to: '/catalog' },
        { label: agentName, to: tab === 'overview' ? undefined : pathForAgentTab(apiId, 'overview') },
    ];
    if (tab !== 'overview') {
        crumbs.push({ label: agentTabLabel(tab) });
    }
    return crumbs;
}
