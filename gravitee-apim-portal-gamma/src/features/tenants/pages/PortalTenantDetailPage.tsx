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
import { Button, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { getPublishedApiNavItems } from '../../../blocks/ApiCatalogBlock/catalog-utils';
import { getPortal } from '../../portals/storage/portals.storage';
import { getNavItems } from '../../portals/storage/navigation-items.storage';
import { usePortalsNavigation } from '../../portals/config/navigation';
import { NotFoundPage } from '../../../shared/components/NotFoundPage';
import { DeletePortalTenantDialog } from '../components/DeletePortalTenantDialog';
import { TenantApiAccessTab } from '../components/TenantApiAccessTab';
import { TenantFeaturesTab } from '../components/TenantFeaturesTab';
import { TenantOverviewTab } from '../components/TenantOverviewTab';
import { TenantUsersTab } from '../components/TenantUsersTab';
import { usePortalTenant } from '../hooks/usePortalTenant';
import { deleteMembersForTenant } from '../storage/portal-tenant-members.storage';
import { deletePortalTenant } from '../storage/portal-tenants.storage';
import { countTenantApps } from '../storage/seed-portal-tenants';

type TenantTab = 'overview' | 'users' | 'api-access' | 'features';

const TABS: Array<{ id: TenantTab; label: string }> = [
    { id: 'overview', label: 'Overview' },
    { id: 'users', label: 'Users' },
    { id: 'api-access', label: 'API access' },
    { id: 'features', label: 'Features' },
];

export function PortalTenantDetailPage() {
    const { portalId, tenantId } = useParams<{ portalId: string; tenantId: string }>();
    const navigate = useNavigate();
    const { homePath, portalTenantsPath, portalViewPath } = usePortalsNavigation();
    const [searchParams, setSearchParams] = useSearchParams();
    const { tenant, members, loading, updateTenant, setMembers } = usePortalTenant(tenantId);
    const [portalName, setPortalName] = useState('');
    const [totalApiCount, setTotalApiCount] = useState(0);
    const [appCount, setAppCount] = useState(0);
    const [deleteOpen, setDeleteOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    const activeTab = (searchParams.get('tab') as TenantTab | null) ?? 'overview';

    useEffect(() => {
        if (!portalId) {
            return;
        }

        void (async () => {
            const portal = await getPortal(portalId);
            setPortalName(portal?.name ?? '');

            const navItems = await getNavItems(portalId);
            setTotalApiCount(getPublishedApiNavItems(navItems).length);
        })();
    }, [portalId]);

    useEffect(() => {
        if (!tenantId) {
            return;
        }

        void countTenantApps(tenantId).then(setAppCount);
    }, [tenantId, members]);

    const subtitle = useMemo(() => {
        if (!tenant) {
            return '';
        }

        const created = new Date(tenant.createdAt).toLocaleDateString(undefined, {
            month: 'short',
            year: 'numeric',
        });
        return `${tenant.description ? `${tenant.description} · ` : ''}${members.length} users · Created ${created}`;
    }, [tenant, members.length]);

    const setActiveTab = (tab: TenantTab) => {
        setSearchParams(tab === 'overview' ? {} : { tab });
    };

    const openPreview = () => {
        if (!portalId || !tenantId) {
            return;
        }

        navigate(portalViewPath(portalId, { asTenant: tenantId }));
    };

    const handleDelete = async () => {
        if (!tenantId || !portalId) {
            return;
        }

        setIsDeleting(true);
        try {
            await deleteMembersForTenant(tenantId);
            await deletePortalTenant(tenantId);
            navigate(portalTenantsPath(portalId));
        } finally {
            setIsDeleting(false);
            setDeleteOpen(false);
        }
    };

    if (!loading && !tenant) {
        return (
            <NotFoundPage
                homePath={portalId ? portalTenantsPath(portalId) : homePath}
                homeLabel="Back to tenants"
                title="Tenant not found"
                description="This tenant does not exist or may have been removed."
                className="min-h-screen"
            />
        );
    }

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Link
                    to={portalTenantsPath(portalId ?? '')}
                    className="inline-flex items-center gap-1 hover:text-foreground"
                >
                    <ArrowLeftIcon className="size-4" aria-hidden="true" />
                    Tenants
                </Link>
                <span aria-hidden="true">/</span>
                <span className="text-foreground">{portalName}</span>
            </div>

            {loading || !tenant ? (
                <div className="space-y-4" aria-busy="true">
                    <Skeleton className="h-10 w-64" />
                    <Skeleton className="h-8 w-96" />
                    <Skeleton className="h-64 w-full" />
                </div>
            ) : (
                <>
                    <div className="flex flex-wrap items-start justify-between gap-4">
                        <div className="space-y-1">
                            <h1 className="text-2xl font-bold tracking-tight">{tenant.name}</h1>
                            <p className="text-sm text-muted-foreground">{subtitle}</p>
                        </div>
                        <Button variant="outline" onClick={openPreview}>
                            Preview as
                        </Button>
                    </div>

                    <div className="flex flex-wrap gap-2 border-b">
                        {TABS.map(tab => (
                            <button
                                key={tab.id}
                                type="button"
                                className={`border-b-2 px-3 py-2 text-sm font-medium transition-colors ${
                                    activeTab === tab.id
                                        ? 'border-primary text-foreground'
                                        : 'border-transparent text-muted-foreground hover:text-foreground'
                                }`}
                                onClick={() => setActiveTab(tab.id)}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>

                    {activeTab === 'overview' && (
                        <TenantOverviewTab
                            tenant={tenant}
                            members={members}
                            totalApiCount={totalApiCount}
                            appCount={appCount}
                            onNavigateTab={setActiveTab}
                            onDelete={() => setDeleteOpen(true)}
                        />
                    )}

                    {activeTab === 'users' && (
                        <TenantUsersTab tenant={tenant} members={members} onMembersChange={setMembers} />
                    )}

                    {activeTab === 'api-access' && portalId && (
                        <TenantApiAccessTab
                            portalId={portalId}
                            tenant={tenant}
                            onSave={async patch => {
                                await updateTenant(patch);
                            }}
                        />
                    )}

                    {activeTab === 'features' && (
                        <TenantFeaturesTab
                            tenant={tenant}
                            onSave={async features => {
                                await updateTenant({ features });
                            }}
                        />
                    )}
                </>
            )}

            {tenant && (
                <DeletePortalTenantDialog
                    tenant={tenant}
                    open={deleteOpen}
                    isPending={isDeleting}
                    onOpenChange={setDeleteOpen}
                    onConfirm={() => void handleDelete()}
                />
            )}
        </div>
    );
}
