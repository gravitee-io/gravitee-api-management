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
    BellIcon,
    ChevronDownIcon,
    ChevronRightIcon,
    FlaskConicalIcon,
    GlobeIcon,
    LayoutDashboardIcon,
    ListIcon,
    LockIcon,
    MessageSquareIcon,
    NetworkIcon,
    RocketIcon,
    ScrollTextIcon,
    ServerIcon,
    SettingsIcon,
    ShieldCheckIcon,
    ShieldIcon,
    SlidersHorizontalIcon,
    SparklesIcon,
    UsersIcon,
    UsersRoundIcon,
    WorkflowIcon,
} from '@gravitee/graphene-core/icons';
import type { ComponentType } from 'react';
import { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';

// ─── Types ────────────────────────────────────────────────────────────────────

const DEFAULT_COMING_SOON_REASON = 'Coming soon';

export interface DetailNavChild {
    path: string;
    label: string;
    /** When true, renders as a non-navigable item with a lab icon and tooltip instead of a link. */
    comingSoon?: boolean;
    /** Tooltip text for a `comingSoon` item. Defaults to "Coming soon". */
    comingSoonReason?: string;
}

export interface DetailNavItem {
    path: string;
    label: string;
    icon: ComponentType<{ className?: string }>;
    /** When false, matches any sub-path (prefix match). Defaults to true (exact match). */
    end?: boolean;
    children?: DetailNavChild[];
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

export const API_PROXY_NAV_GROUPS: DetailNavGroup[] = [
    {
        label: 'General',
        items: [
            { path: 'overview', label: 'Overview', icon: LayoutDashboardIcon },
            { path: 'general', label: 'General', icon: SlidersHorizontalIcon },
            { path: 'properties', label: 'API Properties', icon: SettingsIcon },
            { path: 'resources', label: 'Resources', icon: ServerIcon },
            { path: 'notifications', label: 'Notifications', icon: BellIcon },
            { path: 'api-score', label: 'API Score', icon: SparklesIcon, comingSoon: true },
            { path: 'response-templates', label: 'Response Templates', icon: ScrollTextIcon, comingSoon: true },
            { path: 'cors', label: 'CORS', icon: ShieldCheckIcon },
        ],
    },
    {
        label: 'Gateway',
        items: [
            { path: 'entrypoints', label: 'Entrypoints', icon: GlobeIcon },
            {
                path: 'endpoints',
                label: 'Endpoints',
                icon: NetworkIcon,
                children: [
                    { path: 'list', label: 'Endpoints' },
                    { path: 'failover', label: 'Failover' },
                    { path: 'health-check-dashboard', label: 'Health Check Dashboard' },
                ],
            },
            { path: 'reporter-settings', label: 'Reporter Settings', icon: ListIcon },
        ],
    },
    {
        label: 'Design',
        items: [{ path: 'policy-studio', label: 'Policy Studio', icon: WorkflowIcon }],
    },
    {
        label: 'Consumer Access',
        items: [
            { path: 'plans', label: 'Plans', icon: ShieldIcon },
            { path: 'consumers', label: 'Consumers', icon: UsersRoundIcon, end: false },
            { path: 'broadcasts', label: 'Broadcasts', icon: MessageSquareIcon },
        ],
    },
    {
        label: 'Security',
        items: [
            { path: 'authorization', label: 'Authorization', icon: LockIcon, comingSoon: true },
            { path: 'user-permissions', label: 'User Permissions', icon: UsersIcon },
        ],
    },
    {
        label: 'Monitoring',
        items: [
            { path: 'alerts', label: 'Alerts', icon: ActivityIcon },
            { path: 'audit-logs', label: 'Audit Logs', icon: ScrollTextIcon },
        ],
    },
    {
        label: 'Operations',
        items: [
            {
                path: 'deployment',
                label: 'Deployment',
                icon: RocketIcon,
                children: [
                    { path: 'configuration', label: 'Configuration' },
                    { path: 'history', label: 'History' },
                ],
            },
        ],
    },
];

/** Classic console parity (`api-v4-menu.service.ts`, `hasTcpListeners`) — TCP has no HTTP policy-chain semantics. */
const TCP_UNSUPPORTED_PATHS = new Set(['policy-studio', 'cors']);
const TCP_UNSUPPORTED_REASON = 'Coming soon for V4 APIs';

/** Classic console never adds these menu entries for TCP APIs at all — omitted, not just disabled. */
const TCP_OMITTED_CHILD_PATHS = new Set(['failover', 'health-check-dashboard']);

/** Overlays `comingSoon` on the items TCP Proxy APIs don't support, and omits child routes that don't exist for TCP — matching classic console. */
export function withTcpRestrictions(groups: DetailNavGroup[], apiHasTcpListeners: boolean): DetailNavGroup[] {
    if (!apiHasTcpListeners) return groups;
    return groups.map(group => ({
        ...group,
        items: group.items.map(item => {
            const disabled = TCP_UNSUPPORTED_PATHS.has(item.path)
                ? { ...item, comingSoon: true, comingSoonReason: TCP_UNSUPPORTED_REASON }
                : item;
            if (!disabled.children) return disabled;
            return { ...disabled, children: disabled.children.filter(child => !TCP_OMITTED_CHILD_PATHS.has(child.path)) };
        }),
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

// ─── Collapsible item ─────────────────────────────────────────────────────────

interface CollapsibleNavItemProps {
    item: DetailNavItem & { children: DetailNavChild[] };
    basePath: string;
}

function CollapsibleNavItem({ item, basePath }: CollapsibleNavItemProps) {
    const { pathname } = useLocation();
    const parentPath = `${basePath}/${item.path}`;
    const isActive = pathname.startsWith(`${parentPath}/`) || pathname === parentPath;
    const [collapsed, setCollapsed] = useState(!isActive);
    const [prevIsActive, setPrevIsActive] = useState(isActive);
    if (prevIsActive !== isActive) {
        setPrevIsActive(isActive);
        if (isActive && collapsed) setCollapsed(false);
    }
    const open = !collapsed;
    const Icon = item.icon;

    return (
        <div>
            <button
                type="button"
                onClick={() => setCollapsed(c => !c)}
                className={cn(
                    'flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors',
                    isActive ? 'text-foreground font-medium' : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                )}
            >
                <Icon className="size-4 shrink-0" aria-hidden />
                <span className="flex-1 text-left">{item.label}</span>
                {open ? (
                    <ChevronDownIcon className="size-3.5 shrink-0 text-muted-foreground" aria-hidden />
                ) : (
                    <ChevronRightIcon className="size-3.5 shrink-0 text-muted-foreground" aria-hidden />
                )}
            </button>
            {open && (
                <div className="ml-4 border-l border-border pl-2 space-y-0.5">
                    {item.children.map(child =>
                        child.comingSoon ? (
                            <ComingSoonRow
                                key={child.path}
                                label={child.label}
                                reason={child.comingSoonReason ?? DEFAULT_COMING_SOON_REASON}
                                indented
                            />
                        ) : (
                            <NavLink
                                end
                                key={child.path}
                                to={`${parentPath}/${child.path}`}
                                className={({ isActive: active }) =>
                                    cn(
                                        'flex w-full items-center rounded-lg px-3 py-1.5 text-sm transition-colors',
                                        active ? 'bg-accent text-foreground' : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                                    )
                                }
                            >
                                {child.label}
                            </NavLink>
                        ),
                    )}
                </div>
            )}
        </div>
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
                            if (item.children && item.children.length > 0) {
                                return (
                                    <CollapsibleNavItem
                                        key={item.path}
                                        item={item as DetailNavItem & { children: DetailNavChild[] }}
                                        basePath={basePath}
                                    />
                                );
                            }
                            const Icon = item.icon;
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
