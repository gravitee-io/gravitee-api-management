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
import { Skeleton } from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';

import { CreateGroupDialog } from './CreateGroupDialog';
import { CreateTenantDialog } from './CreateTenantDialog';
import { GroupDetailPane, type GroupDetailTab } from './GroupDetailPane';
import { GroupFeaturesTab } from './GroupFeaturesTab';
import { GroupListPane } from './GroupListPane';
import { GroupMembersTab } from './GroupMembersTab';
import { GroupPermissionsTab } from './GroupPermissionsTab';
import { TenantListPane } from './TenantListPane';
import { ConfirmDialog } from '../../../shared/components/ConfirmDialog';
import { notify } from '../../../shared/notify/notify';
import { useGroupGrants } from '../hooks/useGroupGrants';
import { useGroupMembers } from '../hooks/useGroupMembers';
import { usePermissionsDirectory } from '../hooks/usePermissionsDirectory';
import { usePortalNavigationIndex } from '../hooks/usePortalNavigationIndex';
import { useScopeCatalog } from '../hooks/useScopeCatalog';

interface PortalConsumerPermissionsProps {
    readonly className?: string;
}

/**
 * Consumer-side permissions: tenants, the groups inside them, and what each group may see or consume.
 * Rendered both by the portals module Permissions screen and by the unified platform screen.
 */
export function PortalConsumerPermissions({ className }: PortalConsumerPermissionsProps) {
    const {
        tenants,
        groupsByTenantId,
        loading,
        createTenant,
        removeTenant,
        createGroup,
        renameGroup,
        updateGroupFeatures,
        removeGroup,
        refresh,
    } = usePermissionsDirectory();
    const { options: scopeOptions, labelFor } = useScopeCatalog();
    const { items: navigationItems, portalNameById } = usePortalNavigationIndex();

    const [tenantQuery, setTenantQuery] = useState('');
    const [groupQuery, setGroupQuery] = useState('');
    const [selectedTenantId, setSelectedTenantId] = useState<string | null>(null);
    const [selectedGroupId, setSelectedGroupId] = useState<string | null>(null);
    const [activeTab, setActiveTab] = useState<GroupDetailTab>('members');
    const [createTenantOpen, setCreateTenantOpen] = useState(false);
    const [createGroupOpen, setCreateGroupOpen] = useState(false);
    const [groupPendingDeletion, setGroupPendingDeletion] = useState<string | null>(null);
    const [tenantPendingDeletion, setTenantPendingDeletion] = useState<string | null>(null);

    const filteredTenants = useMemo(() => {
        const normalized = tenantQuery.trim().toLowerCase();
        if (!normalized) {
            return tenants;
        }

        return tenants.filter(
            tenant =>
                tenant.name.toLowerCase().includes(normalized)
                || tenant.hrid.toLowerCase().includes(normalized),
        );
    }, [tenantQuery, tenants]);

    const selectedTenant = tenants.find(tenant => tenant.id === selectedTenantId);
    const tenantGroups = useMemo(
        () => (selectedTenantId ? (groupsByTenantId.get(selectedTenantId) ?? []) : []),
        [groupsByTenantId, selectedTenantId],
    );
    const filteredGroups = useMemo(() => {
        const normalized = groupQuery.trim().toLowerCase();
        if (!normalized) {
            return tenantGroups;
        }

        return tenantGroups.filter(
            group =>
                group.name.toLowerCase().includes(normalized)
                || group.hrid.toLowerCase().includes(normalized),
        );
    }, [groupQuery, tenantGroups]);
    const selectedGroup = filteredGroups.find(group => group.id === selectedGroupId)
        ?? tenantGroups.find(group => group.id === selectedGroupId);

    useEffect(() => {
        if (filteredTenants.length === 0) {
            setSelectedTenantId(null);
            return;
        }

        if (!filteredTenants.some(tenant => tenant.id === selectedTenantId)) {
            // Land on a tenant that actually has groups, so the other two panes are populated.
            const populated = filteredTenants.find(tenant => tenant.groupCount > 0);
            setSelectedTenantId((populated ?? filteredTenants[0]).id);
        }
    }, [filteredTenants, selectedTenantId]);

    useEffect(() => {
        setGroupQuery('');
    }, [selectedTenantId]);

    useEffect(() => {
        if (filteredGroups.length === 0) {
            setSelectedGroupId(null);
            return;
        }

        if (!filteredGroups.some(group => group.id === selectedGroupId)) {
            const richest = filteredGroups.reduce((best, group) =>
                group.grantCount > best.grantCount ? group : best,
            );
            setSelectedGroupId(richest.id);
        }
    }, [filteredGroups, selectedGroupId]);

    const members = useGroupMembers(selectedGroup?.id, selectedGroup?.tenantId);
    const grants = useGroupGrants(selectedGroup?.id, selectedGroup?.tenantId);

    if (loading) {
        return (
            <div className={`space-y-3 ${className ?? ''}`} aria-busy="true">
                <Skeleton className="h-9 w-72" />
                <Skeleton className="h-full min-h-96 w-full" />
            </div>
        );
    }

    return (
        <div className={`flex min-h-0 flex-col gap-4 ${className ?? ''}`}>
            <div className="grid min-h-0 flex-1 gap-4 lg:grid-cols-5">
                <TenantListPane
                    tenants={filteredTenants}
                    selectedTenantId={selectedTenantId}
                    canCreate
                    canDelete
                    query={tenantQuery}
                    onQueryChange={setTenantQuery}
                    onSelect={setSelectedTenantId}
                    onCreate={() => setCreateTenantOpen(true)}
                    onDelete={setTenantPendingDeletion}
                />

                <GroupListPane
                    groups={filteredGroups}
                    selectedGroupId={selectedGroupId}
                    canCreate={selectedTenantId !== null}
                    query={groupQuery}
                    onQueryChange={setGroupQuery}
                    onSelect={setSelectedGroupId}
                    onCreate={() => setCreateGroupOpen(true)}
                />

                <div className="min-h-0 min-w-0 lg:col-span-3">
                    {selectedGroup && selectedTenant ? (
                        <GroupDetailPane
                            group={selectedGroup}
                            tenant={selectedTenant}
                            activeTab={activeTab}
                            readOnly={false}
                            onTabChange={setActiveTab}
                            onRename={async name => {
                                await renameGroup(selectedGroup.id, name);
                                notify.success('Group renamed.');
                            }}
                            onDelete={() => setGroupPendingDeletion(selectedGroup.id)}
                        >
                            {activeTab === 'members' ? (
                                <GroupMembersTab
                                    groupName={selectedGroup.name}
                                    tenantName={selectedTenant.name}
                                    members={members.members}
                                    tenantMembers={members.tenantMembers}
                                    readOnly={false}
                                    onAddMembers={async users => {
                                        await members.addMembers(users);
                                        await refresh();
                                    }}
                                    onRoleChange={async (memberId, role) => {
                                        await members.setRole(memberId, role);
                                        await refresh();
                                    }}
                                    onRemove={async memberId => {
                                        await members.removeMember(memberId);
                                        await refresh();
                                    }}
                                />
                            ) : activeTab === 'features' ? (
                                <GroupFeaturesTab
                                    group={selectedGroup}
                                    onSave={async features => {
                                        await updateGroupFeatures(selectedGroup.id, features);
                                    }}
                                />
                            ) : (
                                <GroupPermissionsTab
                                    groupName={selectedGroup.name}
                                    grants={grants.grants}
                                    scopeOptions={scopeOptions}
                                    navigationItems={navigationItems}
                                    portalNameById={portalNameById}
                                    readOnly={false}
                                    scopeLabelFor={labelFor}
                                    onAddScope={async values => {
                                        await grants.addGrant(values);
                                        await refresh();
                                    }}
                                    onAccessChange={async (grant, access) => {
                                        await grants.updateGrant(grant.id, {
                                            access,
                                            provisioning:
                                                access === 'CONSUME' ? (grant.provisioning ?? 'CLASSIC') : undefined,
                                        });
                                    }}
                                    onProvisioningChange={async (grant, provisioning, defaultPlanId) => {
                                        await grants.updateGrant(grant.id, { provisioning, defaultPlanId });
                                    }}
                                    onRemoveScope={async grant => {
                                        await grants.removeGrant(grant.id);
                                        await refresh();
                                    }}
                                    onOverrideChange={grants.setOverride}
                                />
                            )}
                        </GroupDetailPane>
                    ) : (
                        <div className="flex h-full min-h-0 items-center justify-center rounded-lg border border-dashed bg-card p-6 text-center">
                            <p className="max-w-sm text-sm text-muted-foreground">
                                {selectedTenant
                                    ? 'Create a group in this tenant to start granting access to assets and portal content.'
                                    : 'Select a tenant to manage its groups and permissions.'}
                            </p>
                        </div>
                    )}
                </div>
            </div>

            <CreateTenantDialog
                open={createTenantOpen}
                onOpenChange={setCreateTenantOpen}
                onSubmit={values => {
                    void createTenant(values).then(tenant => {
                        setSelectedTenantId(tenant.id);
                        notify.success('Tenant created.');
                    });
                }}
            />

            {selectedTenant && (
                <CreateGroupDialog
                    open={createGroupOpen}
                    tenantName={selectedTenant.name}
                    onOpenChange={setCreateGroupOpen}
                    onSubmit={values => {
                        void createGroup(selectedTenant.id, values).then(group => {
                            setSelectedGroupId(group.id);
                            notify.success('Group created.');
                        });
                    }}
                />
            )}

            <ConfirmDialog
                open={groupPendingDeletion !== null}
                onOpenChange={open => {
                    if (!open) {
                        setGroupPendingDeletion(null);
                    }
                }}
                title="Delete group?"
                description="Members lose every grant this group provided. This cannot be undone."
                confirmLabel="Delete"
                destructive
                onConfirm={() => {
                    if (!groupPendingDeletion) {
                        return;
                    }
                    void removeGroup(groupPendingDeletion).then(() => {
                        notify.success('Group deleted.');
                        setGroupPendingDeletion(null);
                    });
                }}
            />

            <ConfirmDialog
                open={tenantPendingDeletion !== null}
                onOpenChange={open => {
                    if (!open) {
                        setTenantPendingDeletion(null);
                    }
                }}
                title="Delete tenant?"
                description="Every group, membership, and grant in this tenant is removed."
                confirmLabel="Delete"
                destructive
                onConfirm={() => {
                    if (!tenantPendingDeletion) {
                        return;
                    }
                    void removeTenant(tenantPendingDeletion).then(() => {
                        notify.success('Tenant deleted.');
                        setTenantPendingDeletion(null);
                    });
                }}
            />
        </div>
    );
}
