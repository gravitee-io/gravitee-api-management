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
import { Button } from '@gravitee/graphene-core';

import { countEnabledFeatures } from '../utils/tenant-preview';
import type { PortalTenant, PortalTenantMember } from '../types/portal-tenant.types';

interface TenantOverviewTabProps {
    readonly tenant: PortalTenant;
    readonly members: PortalTenantMember[];
    readonly totalApiCount: number;
    readonly appCount: number;
    readonly onNavigateTab: (tab: 'users' | 'api-access' | 'features') => void;
    readonly onDelete: () => void;
}

export function TenantOverviewTab({
    tenant,
    members,
    totalApiCount,
    appCount,
    onNavigateTab,
    onDelete,
}: TenantOverviewTabProps) {
    const apiCount = tenant.apiAccessMode === 'all' ? totalApiCount : tenant.allowedApiIds.length;
    const enabledFeatures = countEnabledFeatures(tenant.features);

    return (
        <div className="space-y-8">
            <div className="grid gap-4 md:grid-cols-3">
                <SummaryCard
                    title="Users"
                    value={`${members.length} members`}
                    actionLabel="Manage users →"
                    onAction={() => onNavigateTab('users')}
                />
                <SummaryCard
                    title="API access"
                    value={`${apiCount} of ${totalApiCount} APIs`}
                    actionLabel="Configure →"
                    onAction={() => onNavigateTab('api-access')}
                />
                <SummaryCard
                    title="Features"
                    value={`${enabledFeatures} of 6 enabled`}
                    actionLabel="Configure →"
                    onAction={() => onNavigateTab('features')}
                />
            </div>

            <section className="space-y-3">
                <h3 className="text-sm font-semibold">Recent activity (mock)</h3>
                <ul className="space-y-2 text-sm text-muted-foreground">
                    {members[0] && (
                        <li>
                            · {members[0].email} subscribed to Payments API — 2 days ago
                        </li>
                    )}
                    {members[1] && (
                        <li>
                            · {members[1].email} created application &quot;Checkout&quot; — 5 days ago
                        </li>
                    )}
                    {members.length === 0 && <li>· No activity yet</li>}
                </ul>
            </section>

            <section className="space-y-3 rounded-lg border border-destructive/30 p-4">
                <h3 className="text-sm font-semibold text-destructive">Danger zone</h3>
                <p className="text-sm text-muted-foreground">
                    Permanently delete this tenant and remove all member assignments ({appCount} apps keep their data).
                </p>
                <Button variant="destructive" onClick={onDelete}>
                    Delete tenant
                </Button>
            </section>
        </div>
    );
}

function SummaryCard({
    title,
    value,
    actionLabel,
    onAction,
}: {
    readonly title: string;
    readonly value: string;
    readonly actionLabel: string;
    readonly onAction: () => void;
}) {
    return (
        <div className="rounded-lg border p-4">
            <h3 className="text-sm font-medium text-muted-foreground">{title}</h3>
            <p className="mt-2 text-lg font-semibold">{value}</p>
            <Button variant="link" className="mt-2 h-auto p-0" onClick={onAction}>
                {actionLabel}
            </Button>
        </div>
    );
}
