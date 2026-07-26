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
import { Badge, Button, Card, CardContent } from '@gravitee/graphene-core';
import { useState } from 'react';

import { PORTAL_TENANT_MANAGEMENT_MODE_LABELS, type PortalTenant } from '../../tenants/types/portal-tenant.types';
import type { PermissionsGroupSummary } from '../hooks/usePermissionsDirectory';
import { PORTAL_GROUP_MANAGEMENT_MODE_LABELS } from '../types/permissions.types';

export type GroupDetailTab = 'members' | 'permissions' | 'features';

const TABS: readonly { id: GroupDetailTab; label: string }[] = [
    { id: 'members', label: 'Members' },
    { id: 'permissions', label: 'Permissions' },
    { id: 'features', label: 'Features' },
];

interface GroupDetailPaneProps {
    readonly group: PermissionsGroupSummary;
    readonly tenant: PortalTenant;
    readonly activeTab: GroupDetailTab;
    readonly readOnly: boolean;
    readonly onTabChange: (tab: GroupDetailTab) => void;
    readonly onRename: (name: string) => Promise<void>;
    readonly onDelete: () => void;
    readonly children: React.ReactNode;
}

export function GroupDetailPane({
    group,
    tenant,
    activeTab,
    readOnly,
    onTabChange,
    onRename,
    onDelete,
    children,
}: GroupDetailPaneProps) {
    const [renaming, setRenaming] = useState(false);
    const [draftName, setDraftName] = useState(group.name);

    const tenantMode = tenant.managementMode
        ? PORTAL_TENANT_MANAGEMENT_MODE_LABELS[tenant.managementMode]
        : undefined;
    const groupMode = PORTAL_GROUP_MANAGEMENT_MODE_LABELS[group.managementMode];

    const submitRename = () => {
        const trimmed = draftName.trim();
        setRenaming(false);
        if (trimmed.length < 2 || trimmed === group.name) {
            setDraftName(group.name);
            return;
        }
        void onRename(trimmed);
    };

    return (
        <div className="flex h-full min-h-0 flex-col gap-5 overflow-hidden rounded-lg border bg-card p-5">
            <div className="flex shrink-0 flex-wrap items-start justify-between gap-3">
                <div className="flex min-w-0 flex-wrap items-center gap-2">
                    {renaming ? (
                        <input
                            autoFocus
                            aria-label="Group name"
                            value={draftName}
                            onChange={event => setDraftName(event.target.value)}
                            onBlur={submitRename}
                            onKeyDown={event => {
                                if (event.key === 'Enter') {
                                    submitRename();
                                }
                                if (event.key === 'Escape') {
                                    setDraftName(group.name);
                                    setRenaming(false);
                                }
                            }}
                            className="h-9 rounded-md border border-input bg-background px-2 text-xl font-semibold tracking-tight"
                        />
                    ) : (
                        <h2 className="truncate text-xl font-semibold tracking-tight">{group.name}</h2>
                    )}
                    <Badge variant="secondary">{groupMode.long}</Badge>
                    {tenantMode && <Badge variant="outline">{tenantMode.long}</Badge>}
                </div>

                <div className="flex gap-2">
                    <Button type="button" size="sm" variant="outline" disabled={readOnly} onClick={onDelete}>
                        Delete
                    </Button>
                    <Button
                        type="button"
                        size="sm"
                        disabled={readOnly}
                        onClick={() => {
                            setDraftName(group.name);
                            setRenaming(true);
                        }}
                    >
                        Rename
                    </Button>
                </div>
            </div>

            <div className="grid shrink-0 gap-3 sm:grid-cols-3">
                <StatCard label="Members" value={group.memberCount} />
                <StatCard label="Permissions" value={group.grantCount} />
                <StatCard label="Admins" value={group.adminCount} />
            </div>

            <div className="flex shrink-0 flex-wrap gap-1 border-b">
                {TABS.map(tab => (
                    <button
                        key={tab.id}
                        type="button"
                        aria-current={activeTab === tab.id}
                        className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
                            activeTab === tab.id
                                ? 'border-primary text-primary'
                                : 'border-transparent text-muted-foreground hover:text-foreground'
                        }`}
                        onClick={() => onTabChange(tab.id)}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto">{children}</div>
        </div>
    );
}

function StatCard({ label, value }: { readonly label: string; readonly value: number }) {
    return (
        <Card>
            <CardContent className="flex items-baseline gap-2 p-3">
                <span className="text-xl font-semibold tracking-tight">{value}</span>
                <span className="text-sm text-muted-foreground">{label}</span>
            </CardContent>
        </Card>
    );
}
