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
import { Alert, AlertDescription, Button, Skeleton } from '@gravitee/graphene-core';
import { ExternalLinkIcon } from '@gravitee/graphene-core/icons';
import { AssetConsumerAccessPanel } from '@portal-gamma/features/permissions/components/AssetConsumerAccessPanel';
import { ConsoleAuthoringGrantsTable } from '@portal-gamma/features/permissions/components/ConsoleAuthoringGrantsTable';
import { useAssetConsumerGrants } from '@portal-gamma/features/permissions/hooks/useAssetConsumerGrants';
import { useConsoleDocGrants } from '@portal-gamma/features/permissions/hooks/useConsoleDocGrants';
import { Link, useLocation, useParams } from 'react-router-dom';

import { useApiDetailContext } from '../../../context/ApiDetailContext';

const ENVIRONMENTS_PREFIX = '/environments/';

/** Mirrors buildModuleNavPath from the SDK so the link keeps the current environment. */
function portalsPermissionsPath(pathname: string): string {
    if (!pathname.startsWith(ENVIRONMENTS_PREFIX)) {
        return '/portals/permissions';
    }

    const separatorIndex = pathname.indexOf('/', ENVIRONMENTS_PREFIX.length);
    const environmentBase = separatorIndex > 0 ? pathname.slice(0, separatorIndex) : pathname;
    return `${environmentBase}/portals/permissions`;
}

export function ApiDocumentationAccessPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const { pathname } = useLocation();
    const { api, isLoading } = useApiDetailContext();

    const consoleGrants = useConsoleDocGrants(apiId);
    const consumerGrants = useAssetConsumerGrants(apiId);

    if (isLoading || !apiId) {
        return (
            <div className="flex flex-col gap-4">
                <Skeleton className="h-8 w-64" />
                <Skeleton className="h-64 w-full" />
            </div>
        );
    }

    const apiName = api?.name ?? 'this API';

    return (
        <div className="space-y-8">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Documentation Access</h1>
                <p className="text-sm text-muted-foreground">
                    Decide who authors the documentation of {apiName}, and review which portal groups can read or
                    subscribe to it.
                </p>
            </div>

            <Alert>
                <AlertDescription>
                    Either this API&apos;s own team owns its documentation, or the portal managers do. Give the
                    owning team the <strong>Owner</strong> role and everyone else <strong>Reader</strong>.
                </AlertDescription>
            </Alert>

            <section className="space-y-3">
                <h2 className="text-lg font-semibold tracking-tight">Authoring</h2>
                <ConsoleAuthoringGrantsTable
                    grants={consoleGrants.grants}
                    fixedScope={{ scopeType: 'API', scopeId: apiId, scopeName: apiName }}
                    scopeLabelFor={() => apiName}
                    onAdd={async input => {
                        await consoleGrants.addGrant(input);
                    }}
                    onRoleChange={consoleGrants.setRole}
                    onRemove={consoleGrants.removeGrant}
                />
            </section>

            <section className="space-y-3">
                <h2 className="text-lg font-semibold tracking-tight">Consumer access</h2>
                <AssetConsumerAccessPanel
                    rows={consumerGrants.rows}
                    action={
                        <Button variant="outline" size="sm" asChild>
                            <Link to={portalsPermissionsPath(pathname)}>
                                Manage in Developer Portals
                                <ExternalLinkIcon className="size-4" aria-hidden />
                            </Link>
                        </Button>
                    }
                />
            </section>
        </div>
    );
}
