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
import { Skeleton } from '@gravitee/graphene-core';
import { useState } from 'react';

import { IngestedApisTable } from './IngestedApisTable';
import { IntegrationIngestionInProgress } from './IntegrationIngestionInProgress';
import { useIntegrationIngestedApis } from '../hooks/useIntegrationIngestedApis';
import { DEFAULT_INGESTED_API_LIST_PAGE_SIZE } from '../utils/paginationConstants';

export const INGESTED_APIS_LOAD_ERROR_MESSAGE = 'Ingested APIs could not be loaded. Please refresh and try again.';

// Graphene's DataTableEmptyState renders its title as a div, so the empty states carry their own heading element.
function IngestedApisEmptyState({ isIngesting }: Readonly<{ isIngesting: boolean }>) {
    const { title, description } = isIngesting
        ? {
              title: 'APIs are being ingested',
              description: 'APIs will appear below once completed. This may take some time depending on volume.',
          }
        : {
              title: 'No APIs created',
              description: 'Create Federated APIs in Gravitee based on APIs or event streams from the external provider.',
          };

    return (
        <div className="space-y-1 rounded-lg border p-8 text-center">
            <h2 className="text-base font-semibold">{title}</h2>
            <p className="text-sm text-muted-foreground">{description}</p>
        </div>
    );
}

interface IntegrationIngestedApisSectionProps {
    readonly integrationId: string;
    readonly isIngesting: boolean;
}

export function IntegrationIngestedApisSection({ integrationId, isIngesting }: IntegrationIngestedApisSectionProps) {
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(DEFAULT_INGESTED_API_LIST_PAGE_SIZE);
    const { data, isError, isPlaceholderData } = useIntegrationIngestedApis(integrationId, { page, perPage, isIngesting });

    if (isError) {
        return <p className="text-sm text-muted-foreground">{INGESTED_APIS_LOAD_ERROR_MESSAGE}</p>;
    }

    if (!data) {
        return <Skeleton className="h-32 w-full" />;
    }

    if (data.data.length === 0) {
        return <IngestedApisEmptyState isIngesting={isIngesting} />;
    }

    return (
        <div className="space-y-4">
            {isIngesting && <IntegrationIngestionInProgress />}
            <IngestedApisTable
                apis={data.data}
                totalCount={data.pagination.totalCount}
                page={page}
                pageSize={perPage}
                loading={isPlaceholderData}
                onPageChange={setPage}
                onPageSizeChange={setPerPage}
            />
        </div>
    );
}
