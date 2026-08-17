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

import { useParams } from 'react-router-dom';

import { SharedPolicyGroupHistoriesTable } from '../features/shared-policy-groups/components/SharedPolicyGroupHistoriesTable';
import { useSharedPolicyGroupHistoryList } from '../features/shared-policy-groups/hooks/useSharedPolicyGroupHistoryList';


export function SharedPolicyGroupHistoryPage() {
    const { sharedPolicyGroupId } = useParams<{ sharedPolicyGroupId: string }>();
    const { page, pageSize, sorting, setPage, setPageSize, setSorting, histories, totalCount, isLoading, isError } =
        useSharedPolicyGroupHistoryList(sharedPolicyGroupId);

    if (isError) {
        return (
            <p className="text-sm text-muted-foreground" data-testid="shared-policy-group-history-error">
                Failed to load version history.
            </p>
        );
    }

    return (
        <div className="space-y-4" data-testid="shared-policy-group-history">
            <SharedPolicyGroupHistoriesTable
                histories={histories}
                totalCount={totalCount}
                loading={isLoading}
                page={page}
                pageSize={pageSize}
                sorting={sorting}
                onPageChange={setPage}
                onPageSizeChange={setPageSize}
                onSortingChange={setSorting}
            />
        </div>
    );
}
