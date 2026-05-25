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

import { PlansTable } from './PlansTable';
import { PlanStatusCards } from './PlanStatusCards';
import { usePlanList } from '../../../hooks/usePlans';
import type { PlanStatusCounts } from '../../../hooks/usePlans';
import type { PlanContext, PlanStatus } from '../../../types/plan';

interface PlansListPageProps {
    ctx: PlanContext;
    counts: PlanStatusCounts;
    canUpdate: boolean;
}

export function PlansListPage({ ctx, counts, canUpdate }: Readonly<PlansListPageProps>) {
    const [selectedStatus, setSelectedStatus] = useState<PlanStatus>('STAGING');
    const [page, setPage] = useState(1);
    const [perPage, setPerPage] = useState(10);

    const { data, isLoading } = usePlanList(ctx, [selectedStatus], page, perPage);

    const handleStatusSelect = (status: PlanStatus) => {
        setSelectedStatus(status);
        setPage(1);
    };

    const handlePerPageChange = (newPerPage: number) => {
        setPerPage(newPerPage);
        setPage(1);
    };

    return (
        <div className="space-y-4">
            <PlanStatusCards
                staging={counts.staging}
                published={counts.published}
                deprecated={counts.deprecated}
                closed={counts.closed}
                isLoading={counts.isLoading}
                selectedStatus={selectedStatus}
                onStatusSelect={handleStatusSelect}
            />

            <PlansTable
                ctx={ctx}
                plans={data?.data ?? []}
                totalCount={data?.pagination.totalCount ?? 0}
                page={page}
                perPage={perPage}
                isLoading={isLoading}
                canUpdate={canUpdate}
                onPage={setPage}
                onPerPage={handlePerPageChange}
            />
        </div>
    );
}
