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
import { Button, Collapsible, CollapsibleContent, CollapsibleTrigger } from '@gravitee/graphene-core';
import {
    ActivityIcon,
    ArrowLeftIcon,
    BarChart3Icon,
    BellIcon,
    ChevronDownIcon,
    CreditCardIcon,
    DatabaseIcon,
    FileTextIcon,
    GlobeIcon,
    InfoIcon,
    LayoutGridIcon,
    NetworkIcon,
    RadioIcon,
    RocketIcon,
    ScrollTextIcon,
    ServerIcon,
    ShieldIcon,
    SlidersHorizontalIcon,
    StarIcon,
    UserCogIcon,
    UsersIcon,
    WorkflowIcon,
} from '@gravitee/graphene-core/icons';
import type { LucideIcon } from 'lucide-react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';

// ── Types ────────────────────────────────────────────────────────────────────

type ChildNavItem = { label: string; path: string };

type LeafNavItem = { label: string; path: string; icon: LucideIcon; children?: never };
type ParentNavItem = { label: string; path?: string; icon: LucideIcon; children: ChildNavItem[] };
type NavItem = LeafNavItem | ParentNavItem;

type NavGroup = { group: string; items: NavItem[] };

// ── Nav configuration ────────────────────────────────────────────────────────
// Add children: [...] to any item to make it expandable — no component changes needed.

const NAV: NavGroup[] = [
    {
        group: 'General',
        items: [
            { label: 'Overview', path: 'overview', icon: LayoutGridIcon },
            { label: 'General', path: 'general', icon: InfoIcon },
            { label: 'API Properties', path: 'properties', icon: SlidersHorizontalIcon },
            { label: 'Resources', path: 'resources', icon: DatabaseIcon },
            { label: 'Notifications', path: 'notifications', icon: BellIcon },
            { label: 'API Score', path: 'score', icon: StarIcon },
            { label: 'Response Templates', path: 'response-templates', icon: FileTextIcon },
            { label: 'CORS', path: 'cors', icon: GlobeIcon },
        ],
    },
    {
        group: 'Gateway',
        items: [
            { label: 'Entrypoints', path: 'entrypoints', icon: NetworkIcon },
            { label: 'Endpoints', path: 'endpoints', icon: ServerIcon },
            { label: 'Reporter Settings', path: 'reporter-settings', icon: BarChart3Icon },
        ],
    },
    {
        group: 'Design',
        items: [
            { label: 'Policy Studio', path: 'policy-studio', icon: WorkflowIcon },
            { label: 'Documentation', path: 'documentation', icon: ScrollTextIcon },
        ],
    },
    {
        group: 'Consumer Access',
        items: [
            { label: 'Plans', path: 'plans', icon: CreditCardIcon },
            { label: 'Consumers', path: 'consumers', icon: UsersIcon },
            { label: 'Broadcasts', path: 'broadcasts', icon: RadioIcon },
        ],
    },
    {
        group: 'Security',
        items: [
            { label: 'Authorization', path: 'authorization', icon: ShieldIcon },
            { label: 'User Permissions', path: 'user-permissions', icon: UserCogIcon },
        ],
    },
    {
        group: 'Observability',
        items: [{ label: 'Audit Logs', path: 'audit-logs', icon: ActivityIcon }],
    },
    {
        group: 'Operations',
        items: [
            {
                label: 'Deployment',
                icon: RocketIcon,
                children: [
                    { label: 'Configuration', path: 'deployment/configuration' },
                    { label: 'History', path: 'deployment/history' },
                ],
            },
        ],
    },
];

// ── Style tokens ─────────────────────────────────────────────────────────────

const navBase = 'flex items-center gap-2.5 rounded-md px-3 py-1.5 text-sm transition-colors';
const navActive = `${navBase} bg-primary/10 text-primary font-medium`;
const navInactive = `${navBase} text-muted-foreground hover:bg-muted hover:text-foreground`;
const navLinkClass = ({ isActive }: { isActive: boolean }) => (isActive ? navActive : navInactive);

// ml-5(20px=icon centre) + border-l(1px) + pl-2.5(10px) + px-2(8px) = 39px ≈ parent text start(38px)
const treeItemBase = 'block rounded-md px-2 py-1 text-sm transition-colors';
const treeItemActive = `${treeItemBase} bg-primary/10 text-primary font-medium`;
const treeItemInactive = `${treeItemBase} text-muted-foreground hover:bg-muted hover:text-foreground`;
const treeItemClass = ({ isActive }: { isActive: boolean }) => (isActive ? treeItemActive : treeItemInactive);

// ── Nav item renderers ───────────────────────────────────────────────────────

function LeafNavItemView({ item }: { item: LeafNavItem }) {
    return (
        <NavLink to={item.path} className={navLinkClass}>
            <item.icon className="size-4 shrink-0" />
            {item.label}
        </NavLink>
    );
}

function CollapsibleNavItemView({ item }: { item: ParentNavItem }) {
    const location = useLocation();
    const navigate = useNavigate();

    const isOpen = item.children.some(({ path }) => location.pathname.includes(path));
    const defaultPath = item.path ?? item.children[0].path;

    return (
        <Collapsible open={isOpen}>
            <CollapsibleTrigger asChild>
                <button type="button" className={`w-full ${isOpen ? navActive : navInactive}`} onClick={() => navigate(defaultPath)}>
                    <item.icon className="size-4 shrink-0" />
                    <span className="flex-1 text-left">{item.label}</span>
                    <ChevronDownIcon className={`size-3.5 shrink-0 transition-transform duration-200 ${isOpen ? '' : '-rotate-90'}`} />
                </button>
            </CollapsibleTrigger>
            <CollapsibleContent>
                <ul className="mt-1 ml-5 border-l border-border/60 pl-2.5 space-y-0.5">
                    {item.children.map(child => (
                        <li key={child.path}>
                            <NavLink to={child.path} className={treeItemClass}>
                                {child.label}
                            </NavLink>
                        </li>
                    ))}
                </ul>
            </CollapsibleContent>
        </Collapsible>
    );
}

function NavItemRenderer({ item }: { item: NavItem }) {
    return item.children ? <CollapsibleNavItemView item={item} /> : <LeafNavItemView item={item} />;
}

// ── Exported component ───────────────────────────────────────────────────────

type ApiDetailSidebarNavProps = { onBack: () => void };

export function ApiDetailSidebarNav({ onBack }: ApiDetailSidebarNavProps) {
    return (
        <aside
            className="w-52 shrink-0 overflow-y-auto pr-1 pb-4"
            style={{ position: 'sticky', top: 0, maxHeight: '100dvh', alignSelf: 'flex-start' }}
        >
            <div className="flex flex-col gap-4">
                <Button type="button" variant="ghost" size="sm" className="-ml-2 justify-start text-muted-foreground" onClick={onBack}>
                    <ArrowLeftIcon className="size-3.5" aria-hidden="true" />
                    API Proxies
                </Button>

                <nav className="space-y-6">
                    {NAV.map(({ group, items }) => (
                        <div key={group}>
                            <p className="mb-3 px-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground/50">{group}</p>
                            <ul className="space-y-0.5">
                                {items.map(item => (
                                    <li key={item.label}>
                                        <NavItemRenderer item={item} />
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </nav>
            </div>
        </aside>
    );
}
