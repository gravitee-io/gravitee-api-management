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
import { ArchiveIcon, BarChart2Icon, RadioIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

// ─── Single tile ──────────────────────────────────────────────────────────────

interface ActionTileProps {
    Icon: LucideIcon;
    label: string;
    description: string;
    onClick: () => void;
}

function ActionTile({ Icon, label, description, onClick }: ActionTileProps) {
    return (
        <button
            type="button"
            onClick={onClick}
            className="flex flex-col items-start gap-2 rounded-xl border bg-card p-4 text-left transition-colors hover:bg-muted flex-1"
        >
            <div className="rounded-lg bg-primary/10 p-2">
                <Icon className="size-4 text-primary" aria-hidden />
            </div>
            <div>
                <p className="text-sm font-semibold">{label}</p>
                <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
            </div>
        </button>
    );
}

// ─── Static tile definitions ──────────────────────────────────────────────────

const TILES = [
    {
        key: 'apis' as const,
        Icon: RadioIcon,
        label: 'API Proxies',
        description: 'Browse and manage your APIs',
    },
    {
        key: 'api-products' as const,
        Icon: ArchiveIcon,
        label: 'API Products',
        description: 'Manage product bundles',
    },
    {
        key: 'analytics' as const,
        Icon: BarChart2Icon,
        label: 'Analytics',
        description: 'Traffic, errors & latency',
    },
] as const;

// ─── Public component ─────────────────────────────────────────────────────────

interface DashboardQuickActionsProps {
    onGoToApis: () => void;
    onGoToApiProducts: () => void;
    onGoToAnalytics: () => void;
}

export function DashboardQuickActions({ onGoToApis, onGoToApiProducts, onGoToAnalytics }: DashboardQuickActionsProps) {
    const handlers: Record<(typeof TILES)[number]['key'], () => void> = {
        apis: onGoToApis,
        'api-products': onGoToApiProducts,
        analytics: onGoToAnalytics,
    };

    return (
        <div className="flex gap-3">
            {TILES.map(({ key, Icon, label, description }) => (
                <ActionTile key={key} Icon={Icon} label={label} description={description} onClick={handlers[key]} />
            ))}
        </div>
    );
}
