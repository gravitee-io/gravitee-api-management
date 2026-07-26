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
import { ArchiveIcon, RadioIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';

import { UpgradeToAccessButton } from '../../../shared/components/UpgradeToAccessButton';

const NOOP = () => undefined;

// ─── Single tile ──────────────────────────────────────────────────────────────

interface ActionTileProps {
    Icon: LucideIcon;
    label: string;
    description: string;
    onClick: () => void;
    locked?: boolean;
}

function ActionTile({ Icon, label, description, onClick, locked }: ActionTileProps) {
    return (
        <div className="flex flex-1 flex-col items-start gap-2 rounded-xl border bg-card p-4 text-left transition-colors hover:bg-muted">
            <button type="button" onClick={onClick} className="flex w-full flex-col items-start gap-2 text-left">
                <div className="rounded-lg bg-primary/10 p-2">
                    <Icon className="size-4 text-primary" aria-hidden />
                </div>
                <div>
                    <p className="text-sm font-semibold">{label}</p>
                    <p className="text-xs text-muted-foreground mt-0.5">{description}</p>
                </div>
            </button>
            {locked ? <UpgradeToAccessButton onClick={onClick} className="mt-auto" /> : null}
        </div>
    );
}

// ─── Public component ─────────────────────────────────────────────────────────

interface DashboardQuickActionsProps {
    onGoToApis: () => void;
    onGoToApiProducts: () => void;
    apiProductsLocked?: boolean;
    onUpgradeApiProducts?: () => void;
}

export function DashboardQuickActions({
    onGoToApis,
    onGoToApiProducts,
    apiProductsLocked = false,
    onUpgradeApiProducts,
}: DashboardQuickActionsProps) {
    // When locked, never fall through to navigation — require the upgrade handler (or no-op).
    const onApiProductsClick = apiProductsLocked ? (onUpgradeApiProducts ?? NOOP) : onGoToApiProducts;

    return (
        <div className="flex gap-3">
            <ActionTile Icon={RadioIcon} label="API Proxies" description="Browse and manage your APIs" onClick={onGoToApis} />
            <ActionTile
                Icon={ArchiveIcon}
                label="API Products"
                description="Manage product bundles"
                onClick={onApiProductsClick}
                locked={apiProductsLocked}
            />
        </div>
    );
}
