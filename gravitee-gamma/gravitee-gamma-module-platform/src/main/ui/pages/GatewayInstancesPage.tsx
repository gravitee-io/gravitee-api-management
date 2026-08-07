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

import { useState } from 'react';

import { GatewayInstancesTable } from '../features/gateway-instances/components/GatewayInstancesTable';
import { useGatewayInstanceList } from '../features/gateway-instances/hooks/useGatewayInstanceList';
import { useForbiddenResourceRedirect } from '../shared/hooks/useForbiddenResourceRedirect';
import { isForbiddenApiError } from '../shared/utils/apiErrors';

const DEFAULT_PAGE = 1;
const DEFAULT_PAGE_SIZE = 10;

export function GatewayInstancesPage() {
    const [page, setPage] = useState(DEFAULT_PAGE);
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

    const { rows, totalCount, isLoading, isError, error } = useGatewayInstanceList({ page, pageSize });

    const isForbidden = isForbiddenApiError(isError, error);
    useForbiddenResourceRedirect({
        isForbidden,
        permissionPrefix: 'environment-instance-',
        redirectTo: '../applications',
    });

    function handlePageSizeChange(size: number) {
        setPageSize(size);
        setPage(DEFAULT_PAGE);
    }

    if (isForbidden) {
        return null;
    }

    if (isError) {
        return (
            <div className="space-y-6">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">Gateways</h1>
                </div>
                <div className="flex items-center justify-center p-8">
                    <p className="text-sm text-muted-foreground">Failed to load gateway instances. Please refresh and try again.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Gateways</h1>
            </div>
            <GatewayInstancesTable
                rows={rows}
                isLoading={isLoading}
                page={page}
                pageSize={pageSize}
                totalCount={totalCount}
                onPageChange={setPage}
                onPageSizeChange={handlePageSizeChange}
            />
        </div>
    );
}
