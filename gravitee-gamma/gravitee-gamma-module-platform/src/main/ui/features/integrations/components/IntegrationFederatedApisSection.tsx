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

import { Alert, AlertDescription, Button, Card, CardContent, CardHeader, CardTitle, DataTableEmptyState } from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';

import { IntegrationFederatedApisTable } from './IntegrationFederatedApisTable';
import { useHasEnvironmentPermission } from '../../../shared/hooks/useEnvironmentPermissions';
import { useDetailBasePath } from '../../shared/hooks/useDetailBasePath';
import { useIntegrationFederatedApis } from '../hooks/useIntegrationFederatedApis';
import type { Integration } from '../types/integration';
import { federatedApiDetailPath } from '../utils/federatedApiDetailPath';
import { isIngestionInProgress } from '../utils/ingestion';
import { ENVIRONMENT_INTEGRATION_CREATE_PERMISSION, ENVIRONMENT_INTEGRATION_UPDATE_PERMISSION } from '../utils/integrationPermissions';
import { DEFAULT_INTEGRATION_LIST_PAGE_SIZE } from '../utils/paginationConstants';

export function IntegrationFederatedApisSection({ integration }: Readonly<{ integration: Integration }>) {
    const { pathname } = useLocation();
    const detailBasePath = useDetailBasePath('integrations', integration.id);
    const discoveryHref = `${detailBasePath}/discover`;
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(DEFAULT_INTEGRATION_LIST_PAGE_SIZE);
    const ingesting = isIngestionInProgress(integration);
    const { data, isPending, isError } = useIntegrationFederatedApis(integration.id, page, pageSize, {
        pollWhileIngesting: ingesting,
    });
    const canDiscover = useHasEnvironmentPermission([ENVIRONMENT_INTEGRATION_CREATE_PERMISSION]);
    const canConfigure = useHasEnvironmentPermission([ENVIRONMENT_INTEGRATION_UPDATE_PERMISSION]);
    const apis = data?.data ?? [];
    const totalCount = data?.pagination.totalCount ?? 0;
    const discoverEnabled = canDiscover && integration.agentStatus === 'CONNECTED' && !ingesting;

    return (
        <Card data-testid="integration-federated-apis">
            <CardHeader className="flex flex-row items-start justify-between gap-4 space-y-0">
                <CardTitle className="text-lg font-semibold">APIs</CardTitle>
                {canDiscover ? (
                    discoverEnabled ? (
                        <Button asChild data-testid="discover-button">
                            <Link to={discoveryHref}>Discover</Link>
                        </Button>
                    ) : (
                        <Button type="button" disabled data-testid="discover-button">
                            Discover
                        </Button>
                    )
                ) : null}
            </CardHeader>
            <CardContent className="space-y-4">
                {ingesting && totalCount > 0 ? (
                    <Alert variant="warning" data-testid="integration-apis-ingestion-banner">
                        <AlertDescription>
                            APIs are currently being ingested and will appear below once completed. This may take some time depending on
                            volume.
                        </AlertDescription>
                    </Alert>
                ) : null}
                {isError ? (
                    <p className="text-sm text-muted-foreground">Federated APIs could not be loaded. Please refresh and try again.</p>
                ) : null}
                {!isError && (isPending || apis.length > 0) ? (
                    <IntegrationFederatedApisTable
                        apis={apis}
                        totalCount={totalCount}
                        page={page}
                        pageSize={pageSize}
                        loading={isPending}
                        canConfigure={canConfigure}
                        apiHref={apiId => federatedApiDetailPath(pathname, apiId)}
                        onPageChange={setPage}
                        onPageSizeChange={setPageSize}
                    />
                ) : null}
                {!isError && !isPending && apis.length === 0 ? (
                    <div className="rounded-lg border" data-testid="integration-federated-apis-empty">
                        <DataTableEmptyState
                            variant="first-use"
                            icon={<SearchIcon className="size-8" aria-hidden />}
                            title={ingesting ? 'APIs are being ingested' : 'No APIs created'}
                            description={
                                ingesting
                                    ? 'APIs will appear below once completed. This may take some time depending on volume.'
                                    : 'Create Federated APIs in Gravitee based on APIs or event streams from the external provider.'
                            }
                        />
                    </div>
                ) : null}
            </CardContent>
        </Card>
    );
}
