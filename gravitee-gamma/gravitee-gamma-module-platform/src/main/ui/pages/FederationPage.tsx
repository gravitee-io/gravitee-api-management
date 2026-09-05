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

import { useEffect, useState } from 'react';

import { IntegrationsEmptyState } from '../features/integrations/components/IntegrationsEmptyState';
import { IntegrationsTable } from '../features/integrations/components/IntegrationsTable';
import { useIntegrations } from '../features/integrations/hooks/useIntegrations';
import { DEFAULT_INTEGRATION_LIST_PAGE_SIZE } from '../features/integrations/utils/paginationConstants';
import { notify } from '../shared/notify';

export function FederationPage() {
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(DEFAULT_INTEGRATION_LIST_PAGE_SIZE);

    const { data, isLoading, isError, error } = useIntegrations({ page, perPage });

    const integrations = data?.data ?? [];
    const totalCount = data?.pagination.totalCount ?? 0;

    useEffect(() => {
        if (!isError) return;
        notify.error(error, 'Integrations could not be loaded. Please refresh and try again.');
    }, [error, isError]);

    function renderContent() {
        if (isError) {
            return (
                <div className="flex items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">Integrations could not be loaded. Please refresh and try again.</p>
                </div>
            );
        }

        if (!isLoading && totalCount === 0) {
            return <IntegrationsEmptyState />;
        }

        return (
            <IntegrationsTable
                integrations={integrations}
                totalCount={totalCount}
                page={page}
                pageSize={perPage}
                loading={isLoading}
                onPageChange={setPage}
                onPageSizeChange={setPerPage}
            />
        );
    }

    return (
        <div className="space-y-6" data-testid="federation-page">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Integrations</h1>
                <p className="text-sm text-muted-foreground">
                    Connect to third-party API gateways and event brokers to create a unified control plane and API portal with Gravitee.
                </p>
            </div>

            {renderContent()}
        </div>
    );
}
