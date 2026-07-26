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
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import {
    ChevronDownIcon,
    ChevronRightIcon,
    PlusIcon,
    Trash2Icon,
} from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useMemo, useState } from 'react';

import { AddScopeDialog, type AddScopeValues } from './AddScopeDialog';
import { AutoProvisionDialog } from './AutoProvisionDialog';
import { GrantScopeSubtree } from './GrantScopeSubtree';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify/notify';
import type { PortalNavigationItem } from '../../portals/types/navigation-item.types';
import type { OverrideSelection } from '../hooks/useGroupGrants';
import type { ScopeOption } from '../hooks/useScopeCatalog';
import {
    PORTAL_ACCESS_LEVEL_LABELS,
    PORTAL_GRANT_SCOPE_TYPE_LABELS,
    type PortalAccessGrant,
    type PortalAccessLevel,
    type PortalGrantScopeType,
} from '../types/permissions.types';
import { loadPlanOptions, planOptionName } from '../utils/plan-options';

const SCOPE_TYPE_FILTERS: readonly PortalGrantScopeType[] = [
    'PORTAL',
    'API',
    'API_PRODUCT',
    'AI_WORKSPACE',
];

interface GroupPermissionsTabProps {
    readonly groupName: string;
    readonly grants: readonly PortalAccessGrant[];
    readonly scopeOptions: readonly ScopeOption[];
    readonly navigationItems: readonly PortalNavigationItem[];
    readonly portalNameById: ReadonlyMap<string, string>;
    readonly readOnly: boolean;
    readonly scopeLabelFor: (scopeType: PortalGrantScopeType, scopeId: string) => string;
    readonly onAddScope: (values: AddScopeValues) => Promise<void>;
    readonly onAccessChange: (grant: PortalAccessGrant, access: PortalAccessLevel) => Promise<void>;
    readonly onProvisioningChange: (
        grant: PortalAccessGrant,
        provisioning: 'CLASSIC' | 'AUTO',
        defaultPlanId?: string,
    ) => Promise<void>;
    readonly onRemoveScope: (grant: PortalAccessGrant) => Promise<void>;
    readonly onOverrideChange: (
        grantId: string,
        navigationItemId: string,
        portalId: string,
        access: OverrideSelection,
    ) => Promise<void>;
}

export function GroupPermissionsTab({
    groupName,
    grants,
    scopeOptions,
    navigationItems,
    portalNameById,
    readOnly,
    scopeLabelFor,
    onAddScope,
    onAccessChange,
    onProvisioningChange,
    onRemoveScope,
    onOverrideChange,
}: GroupPermissionsTabProps) {
    const [typeFilter, setTypeFilter] = useState<PortalGrantScopeType | 'ALL'>('ALL');
    const [expandedGrantId, setExpandedGrantId] = useState<string | null>(null);
    const [addOpen, setAddOpen] = useState(false);
    const [noticeDismissed, setNoticeDismissed] = useState(false);
    const [planNameByGrantId, setPlanNameByGrantId] = useState<Record<string, string>>({});
    const [autoProvisionGrant, setAutoProvisionGrant] = useState<PortalAccessGrant | null>(null);
    const [downgradeGrant, setDowngradeGrant] = useState<PortalAccessGrant | null>(null);

    const visibleGrants = useMemo(
        () => (typeFilter === 'ALL' ? grants : grants.filter(grant => grant.scopeType === typeFilter)),
        [grants, typeFilter],
    );

    const existingScopeKeys = useMemo(
        () => new Set(grants.map(grant => `${grant.scopeType}:${grant.scopeId}`)),
        [grants],
    );

    // Plan names are resolved lazily so auto-provisioned rows can show which plan is used.
    useEffect(() => {
        const autoGrants = grants.filter(grant => grant.provisioning === 'AUTO' && grant.defaultPlanId);
        if (autoGrants.length === 0) {
            setPlanNameByGrantId({});
            return;
        }

        let cancelled = false;
        void Promise.all(
            autoGrants.map(async grant => {
                const options = await loadPlanOptions(grant.scopeType, grant.scopeId);
                return [grant.id, planOptionName(options, grant.defaultPlanId) ?? ''] as const;
            }),
        ).then(entries => {
            if (!cancelled) {
                setPlanNameByGrantId(Object.fromEntries(entries));
            }
        });

        return () => {
            cancelled = true;
        };
    }, [grants]);

    const toggleExpanded = useCallback((grantId: string) => {
        setExpandedGrantId(current => (current === grantId ? null : grantId));
    }, []);

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <Select
                    value={typeFilter}
                    onValueChange={value => setTypeFilter(value as PortalGrantScopeType | 'ALL')}
                >
                    <SelectTrigger className="w-44" aria-label="Filter by asset type">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="ALL">Asset types</SelectItem>
                        {SCOPE_TYPE_FILTERS.map(type => (
                            <SelectItem key={type} value={type}>
                                {PORTAL_GRANT_SCOPE_TYPE_LABELS[type]}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>

                <Button type="button" disabled={readOnly} onClick={() => setAddOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add asset
                </Button>
            </div>

            <div className="overflow-hidden rounded-lg border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Name</TableHead>
                            <TableHead className="w-24">Type</TableHead>
                            <TableHead className="w-44">Access</TableHead>
                            <TableHead className="w-56">Provisioning</TableHead>
                            <TableHead className="w-12" />
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {visibleGrants.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={5} className="py-8 text-center text-muted-foreground">
                                    No assets granted to this group yet.
                                </TableCell>
                            </TableRow>
                        ) : (
                            visibleGrants.map(grant => {
                                const isExpanded = expandedGrantId === grant.id;
                                const overrideCount = grant.overrides.length;

                                return [
                                    <TableRow key={grant.id}>
                                        <TableCell>
                                            <button
                                                type="button"
                                                className="flex items-center gap-1 text-left font-medium hover:underline"
                                                aria-expanded={isExpanded}
                                                onClick={() => toggleExpanded(grant.id)}
                                            >
                                                {isExpanded ? (
                                                    <ChevronDownIcon className="size-4" aria-hidden />
                                                ) : (
                                                    <ChevronRightIcon className="size-4" aria-hidden />
                                                )}
                                                {scopeLabelFor(grant.scopeType, grant.scopeId)}
                                            </button>
                                            {overrideCount > 0 && (
                                                <span className="ml-5 text-xs text-muted-foreground">
                                                    {overrideCount} navigation override
                                                    {overrideCount === 1 ? '' : 's'}
                                                </span>
                                            )}
                                        </TableCell>
                                        <TableCell className="text-xs text-muted-foreground">
                                            {PORTAL_GRANT_SCOPE_TYPE_LABELS[grant.scopeType]}
                                        </TableCell>
                                        <TableCell>
                                            <ToggleGroup
                                                type="single"
                                                size="sm"
                                                value={grant.access}
                                                disabled={readOnly}
                                                onValueChange={value => {
                                                    if (!value) {
                                                        return;
                                                    }
                                                    const nextAccess = value as PortalAccessLevel;
                                                    if (
                                                        grant.access === 'CONSUME' &&
                                                        nextAccess === 'VIEW'
                                                    ) {
                                                        setDowngradeGrant(grant);
                                                        return;
                                                    }
                                                    void onAccessChange(grant, nextAccess).then(() =>
                                                        notify.success('Access updated.'),
                                                    );
                                                }}
                                            >
                                                <ToggleGroupItem value="VIEW">
                                                    {PORTAL_ACCESS_LEVEL_LABELS.VIEW}
                                                </ToggleGroupItem>
                                                <ToggleGroupItem
                                                    value="CONSUME"
                                                    disabled={grant.scopeType === 'PORTAL'}
                                                >
                                                    {PORTAL_ACCESS_LEVEL_LABELS.CONSUME}
                                                </ToggleGroupItem>
                                            </ToggleGroup>
                                        </TableCell>
                                        <TableCell>
                                            {grant.access !== 'CONSUME' ? (
                                                <span className="text-muted-foreground">—</span>
                                            ) : grant.provisioning === 'AUTO' ? (
                                                <div className="flex flex-wrap items-center gap-2">
                                                    <Badge variant="success">Auto</Badge>
                                                    <span className="truncate text-xs text-muted-foreground">
                                                        {planNameByGrantId[grant.id] ?? 'Default plan'}
                                                    </span>
                                                    <Button
                                                        type="button"
                                                        variant="link"
                                                        size="xs"
                                                        className="h-auto p-0"
                                                        disabled={readOnly}
                                                        onClick={() => {
                                                            void onProvisioningChange(
                                                                grant,
                                                                'CLASSIC',
                                                            ).then(() =>
                                                                notify.success(
                                                                    'Switched to the classic subscription workflow.',
                                                                ),
                                                            );
                                                        }}
                                                    >
                                                        Switch to classic
                                                    </Button>
                                                </div>
                                            ) : (
                                                <div className="flex flex-wrap items-center gap-2">
                                                    <Badge variant="outline">Classic</Badge>
                                                    <Button
                                                        type="button"
                                                        variant="link"
                                                        size="xs"
                                                        className="h-auto p-0"
                                                        disabled={readOnly}
                                                        onClick={() => setAutoProvisionGrant(grant)}
                                                    >
                                                        Auto-provision
                                                    </Button>
                                                </div>
                                            )}
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon-sm"
                                                aria-label={`Remove ${scopeLabelFor(grant.scopeType, grant.scopeId)}`}
                                                disabled={readOnly}
                                                onClick={() => {
                                                    void onRemoveScope(grant).then(() =>
                                                        notify.success('Asset removed.'),
                                                    );
                                                }}
                                            >
                                                <Trash2Icon className="size-4" aria-hidden />
                                            </Button>
                                        </TableCell>
                                    </TableRow>,
                                    isExpanded ? (
                                        <TableRow key={`${grant.id}-subtree`}>
                                            <TableCell colSpan={5} className="bg-muted/30 p-0">
                                                <GrantScopeSubtree
                                                    grant={grant}
                                                    grants={grants}
                                                    navigationItems={navigationItems}
                                                    portalNameById={portalNameById}
                                                    readOnly={readOnly}
                                                    onOverrideChange={(
                                                        navigationItemId,
                                                        portalId,
                                                        access,
                                                    ) =>
                                                        onOverrideChange(
                                                            grant.id,
                                                            navigationItemId,
                                                            portalId,
                                                            access,
                                                        )
                                                    }
                                                />
                                            </TableCell>
                                        </TableRow>
                                    ) : null,
                                ];
                            })
                        )}
                    </TableBody>
                </Table>
            </div>

            {!noticeDismissed && (
                <Alert>
                    <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
                        <span>
                            You can grant <strong>View</strong> and <strong>Consume</strong> access to
                            assets and portal content for this group.
                        </span>
                        <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setNoticeDismissed(true)}
                        >
                            Close
                        </Button>
                    </AlertDescription>
                </Alert>
            )}

            <AddScopeDialog
                open={addOpen}
                groupName={groupName}
                options={scopeOptions}
                existingScopeKeys={existingScopeKeys}
                onOpenChange={setAddOpen}
                onSubmit={values => {
                    void onAddScope(values).then(() => notify.success('Asset added.'));
                }}
            />

            <AutoProvisionDialog
                open={autoProvisionGrant !== null}
                grant={autoProvisionGrant}
                groupName={groupName}
                assetLabel={
                    autoProvisionGrant
                        ? scopeLabelFor(autoProvisionGrant.scopeType, autoProvisionGrant.scopeId)
                        : ''
                }
                onOpenChange={open => {
                    if (!open) {
                        setAutoProvisionGrant(null);
                    }
                }}
                onConfirm={defaultPlanId => {
                    if (!autoProvisionGrant) {
                        return;
                    }
                    void onProvisioningChange(autoProvisionGrant, 'AUTO', defaultPlanId).then(() =>
                        notify.success('Access is now auto-provisioned.'),
                    );
                }}
            />

            <ConfirmDialog
                open={downgradeGrant !== null}
                onOpenChange={open => {
                    if (!open) {
                        setDowngradeGrant(null);
                    }
                }}
                title="Switch to View access?"
                description={
                    <>
                        Changing from <strong>Consume</strong> to <strong>View</strong> closes all
                        existing subscriptions for this group on{' '}
                        {downgradeGrant
                            ? scopeLabelFor(downgradeGrant.scopeType, downgradeGrant.scopeId)
                            : 'this asset'}
                        . Members will keep documentation access but lose the ability to call the API
                        until Consume is granted again.
                    </>
                }
                confirmLabel="Switch to View"
                destructive
                onConfirm={() => {
                    if (!downgradeGrant) {
                        return;
                    }
                    const grant = downgradeGrant;
                    setDowngradeGrant(null);
                    void onAccessChange(grant, 'VIEW').then(() =>
                        notify.success('Access updated. Existing subscriptions will be closed.'),
                    );
                }}
            />
        </div>
    );
}
