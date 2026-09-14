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
import { cn, Skeleton, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@gravitee/graphene-core';
import {
    ActivityIcon,
    AlignLeftIcon,
    BellIcon,
    ClockIcon,
    DatabaseIcon,
    ExternalLinkIcon,
    FlaskConicalIcon,
    GlobeIcon,
    LayoutDashboardIcon,
    ListIcon,
    LockIcon,
    MessageSquareIcon,
    NetworkIcon,
    RefreshCwIcon,
    RocketIcon,
    ScrollTextIcon,
    ServerIcon,
    SettingsIcon,
    ShieldCheckIcon,
    ShieldIcon,
    SlidersHorizontalIcon,
    SparklesIcon,
    TriangleAlertIcon,
    UsersIcon,
    UsersRoundIcon,
    WorkflowIcon,
} from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';
import { NavLink } from 'react-router-dom';

// ─── Types ────────────────────────────────────────────────────────────────────

const DEFAULT_COMING_SOON_REASON = 'Coming soon';

export interface DetailNavItem {
    path: string;
    label: string;
    icon: ComponentType<{ className?: string }>;
    /** When false, matches any sub-path (prefix match). Defaults to true (exact match). */
    end?: boolean;
    /**
     * Absolute link target outside the detail's own routes. Rendered as a plain anchor opening in a
     * new tab: the Observability group sends the user to a section that keeps its own time range and
     * filters, and coming back should restore the API page rather than replace it.
     */
    externalHref?: string;
    /** When true, renders as a non-navigable item with a lab icon and tooltip instead of a link. */
    comingSoon?: boolean;
    /** Tooltip text for a `comingSoon` item. Defaults to "Coming soon". */
    comingSoonReason?: string;
}

export interface DetailNavGroup {
    label: string;
    items: DetailNavItem[];
}

// ─── Nav structure ────────────────────────────────────────────────────────────

/**
 * Canonical Gamma API-detail navigation (FOUND-304): General / Design / Consumers / Monitoring /
 * Observability / Operations, in that order, with the same labels every other API object type — an
 * LLM proxy, a Kafka Service, a Message API — uses for the same page.
 *
 * Observability is not here: its two entries are deep links whose targets depend on the API, so
 * {@link withObservabilityLinks} inserts the group at render time.
 */
export const API_PROXY_NAV_GROUPS: DetailNavGroup[] = [
    {
        label: 'General',
        items: [
            { path: 'overview', label: 'Overview', icon: LayoutDashboardIcon },
            { path: 'general', label: 'Settings', icon: SlidersHorizontalIcon },
            { path: 'user-permissions', label: 'User Permissions', icon: UsersIcon },
            { path: 'authorization', label: 'Authorization', icon: LockIcon, comingSoon: true },
            { path: 'metadata', label: 'Metadata', icon: DatabaseIcon },
        ],
    },
    {
        // Canonical order: how a request enters (Entrypoints), what happens to it (Policy Studio),
        // where it goes (Endpoints), then the settings that qualify those three.
        label: 'Design',
        items: [
            { path: 'entrypoints', label: 'Entrypoints', icon: GlobeIcon },
            { path: 'policy-studio', label: 'Policy Studio', icon: WorkflowIcon },
            { path: 'endpoints/list', label: 'Endpoints', icon: NetworkIcon },
            { path: 'endpoints/failover', label: 'Failover', icon: RefreshCwIcon },
            { path: 'response-templates', label: 'Response Templates', icon: ScrollTextIcon, end: false },
            { path: 'resources', label: 'Resources', icon: ServerIcon },
            { path: 'properties', label: 'API Properties', icon: SettingsIcon },
            { path: 'cors', label: 'CORS', icon: ShieldCheckIcon },
        ],
    },
    {
        label: 'Consumers',
        items: [
            { path: 'plans', label: 'Plans', icon: ShieldIcon },
            { path: 'consumers', label: 'Subscriptions', icon: UsersRoundIcon, end: false },
            { path: 'broadcasts', label: 'Broadcasts', icon: MessageSquareIcon },
        ],
    },
    {
        label: 'Monitoring',
        items: [
            { path: 'notifications', label: 'Notifications', icon: BellIcon },
            { path: 'alerts', label: 'Alerts', icon: TriangleAlertIcon },
            { path: 'audit-logs', label: 'Audit Logs', icon: ScrollTextIcon },
            { path: 'endpoints/health-check-dashboard', label: 'Health Check Dashboard', icon: ActivityIcon },
            { path: 'api-score', label: 'API Score', icon: SparklesIcon },
        ],
    },
    {
        // Flat: choosing where this API runs, reading what changed, and what it reports are three
        // unrelated jobs, not tabs of one page.
        label: 'Operations',
        items: [
            { path: 'deployment/configuration', label: 'Sharding Tags', icon: RocketIcon },
            { path: 'deployment/history', label: 'Deployment History', icon: ClockIcon },
            { path: 'reporter-settings', label: 'Reporter Settings', icon: ListIcon },
        ],
    },
];

/** Classic console parity (`api-v4-menu.service.ts`, `hasTcpListeners`) — TCP has no HTTP policy-chain semantics. */
const TCP_UNSUPPORTED_PATHS = new Set(['policy-studio', 'cors', 'response-templates']);
const TCP_UNSUPPORTED_REASON = 'Coming soon for V4 APIs';

/**
 * Classic console never adds these menu entries for TCP APIs at all — omitted, not just disabled.
 * The observability deep links join them: the console disables API Traffic and Logs for TCP too.
 */
const TCP_OMITTED_PATHS = new Set(['endpoints/failover', 'endpoints/health-check-dashboard', 'observe-dashboard', 'observe-logs']);

/** Overlays `comingSoon` on the items TCP Proxy APIs don't support, and omits the entries that don't exist for TCP — matching classic console. */
export function withTcpRestrictions(groups: DetailNavGroup[], apiHasTcpListeners: boolean): DetailNavGroup[] {
    if (!apiHasTcpListeners) return groups;
    return dropEmptyGroups(
        groups.map(group => ({
            ...group,
            items: group.items
                .filter(item => !TCP_OMITTED_PATHS.has(item.path))
                .map(item =>
                    TCP_UNSUPPORTED_PATHS.has(item.path) ? { ...item, comingSoon: true, comingSoonReason: TCP_UNSUPPORTED_REASON } : item,
                ),
        })),
    );
}

/** Deep links into the Observability section, pre-filtered on this API. Sits between Monitoring and Operations. */
export function withObservabilityLinks(groups: DetailNavGroup[], links: { dashboardHref?: string; logsHref?: string }): DetailNavGroup[] {
    const items: DetailNavItem[] = [
        ...(links.dashboardHref
            ? [{ path: 'observe-dashboard', label: 'Dashboard', icon: LayoutDashboardIcon, externalHref: links.dashboardHref }]
            : []),
        ...(links.logsHref ? [{ path: 'observe-logs', label: 'Logs', icon: AlignLeftIcon, externalHref: links.logsHref }] : []),
    ];
    if (items.length === 0) return groups;
    return groups.flatMap(group => (group.label === 'Monitoring' ? [group, { label: 'Observability', items }] : [group]));
}

function dropEmptyGroups(groups: DetailNavGroup[]): DetailNavGroup[] {
    return groups.filter(group => group.items.length > 0);
}

export function withMetadataPermission(groups: DetailNavGroup[], canReadMetadata: boolean): DetailNavGroup[] {
    if (canReadMetadata) return groups;
    return groups.map(group => ({
        ...group,
        items: group.items.filter(item => item.path !== 'metadata'),
    }));
}

export function withResponseTemplatesPermission(groups: DetailNavGroup[], showResponseTemplates: boolean): DetailNavGroup[] {
    if (showResponseTemplates) return groups;
    return groups.map(group => ({
        ...group,
        items: group.items.filter(item => item.path !== 'response-templates'),
    }));
}

export function withApiScoreEnabled(groups: DetailNavGroup[], apiScoreEnabled: boolean): DetailNavGroup[] {
    if (apiScoreEnabled) return groups;
    return groups.map(group => ({
        ...group,
        items: group.items.filter(item => item.path !== 'api-score'),
    }));
}

// ─── "Coming soon" row ────────────────────────────────────────────────────────

interface ComingSoonRowProps {
    icon?: ComponentType<{ className?: string }>;
    label: string;
    reason: string;
    indented?: boolean;
}

function ComingSoonRow({ icon: Icon, label, reason, indented }: ComingSoonRowProps) {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <div
                    className={cn(
                        'flex w-full items-center gap-2.5 rounded-lg px-3 text-sm text-muted-foreground/50 cursor-default',
                        indented ? 'py-1.5' : 'py-2',
                    )}
                    role="button"
                    tabIndex={0}
                    aria-disabled="true"
                    title={reason}
                >
                    {Icon && <Icon className="size-4 shrink-0" aria-hidden />}
                    <span className="flex-1 text-left">{label}</span>
                    <FlaskConicalIcon className="size-3.5 shrink-0" aria-hidden />
                </div>
            </TooltipTrigger>
            <TooltipContent side="right">{reason}</TooltipContent>
        </Tooltip>
    );
}

// ─── Component ────────────────────────────────────────────────────────────────

interface ApiDetailSidebarNavProps {
    groups: DetailNavGroup[];
    basePath: string;
    /** When false, renders a loading skeleton in place of nav items. Defaults to true. */
    permissionsReady?: boolean;
}

export function ApiDetailSidebarNav({ groups, basePath, permissionsReady = true }: ApiDetailSidebarNavProps) {
    if (!permissionsReady) {
        return (
            <div className="space-y-0.5 px-2 py-4">
                {Array.from({ length: 10 }).map((_, i) => (
                    <div key={i} className="px-3 py-2">
                        <Skeleton className="h-4 rounded" />
                    </div>
                ))}
            </div>
        );
    }

    return (
        <TooltipProvider delayDuration={200}>
            <div className="space-y-0.5 px-2 py-2">
                {groups.map(group => (
                    <div key={group.label} className="pt-4 first:pt-0">
                        <p className="mb-1 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">{group.label}</p>
                        {group.items.map(item => {
                            if (item.comingSoon) {
                                return (
                                    <ComingSoonRow
                                        key={item.path}
                                        icon={item.icon}
                                        label={item.label}
                                        reason={item.comingSoonReason ?? DEFAULT_COMING_SOON_REASON}
                                    />
                                );
                            }
                            const Icon = item.icon;
                            if (item.externalHref) {
                                return (
                                    <a
                                        key={item.path}
                                        href={item.externalHref}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                                    >
                                        <Icon className="size-4 shrink-0" aria-hidden />
                                        {item.label}
                                        <ExternalLinkIcon className="ml-auto size-3.5 shrink-0" aria-hidden />
                                    </a>
                                );
                            }
                            return (
                                <NavLink
                                    end={item.end !== false}
                                    key={item.path}
                                    to={`${basePath}/${item.path}`}
                                    className={({ isActive }) =>
                                        cn(
                                            'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors',
                                            isActive
                                                ? 'bg-accent text-foreground font-medium'
                                                : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                                        )
                                    }
                                >
                                    <Icon className="size-4 shrink-0" aria-hidden />
                                    {item.label}
                                </NavLink>
                            );
                        })}
                    </div>
                ))}
            </div>
        </TooltipProvider>
    );
}
