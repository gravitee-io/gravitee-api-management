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
import { useEnvironment, useHasFeature } from '@gravitee/gamma-modules-sdk';
import { useQuery } from '@tanstack/react-query';

import { searchApiProducts } from '../../api-products/services/apiProduct';
import { apiProductKeys } from '../../api-products/utils/queryKeys';
import { searchApis } from '../../apis/services/apiList';
import { apiListKeys } from '../../apis/utils/queryKeys';
import { ApimLicenseFeature } from '../../license/apimFeatures';

const STATS_PAGE = 1;
const STATS_PER_PAGE = 1;

export interface DashboardStats {
    totalApis: number | null;
    totalProducts: number | null;
    /** null = data not yet available; true = at least one API or product; false = nothing yet */
    hasContent: boolean | null;
    isLoading: boolean;
    isError: boolean;
}

export function useDashboardStats(): DashboardStats {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const hasApiProducts = useHasFeature(ApimLicenseFeature.API_PRODUCTS);
    // Guard on envId too — env may be truthy but id not yet populated
    const enabled = Boolean(envId);

    const totalApisQuery = useQuery({
        queryKey: apiListKeys.count(envId, {}),
        queryFn: () => searchApis(envId, {}, STATS_PAGE, STATS_PER_PAGE),
        enabled,
        staleTime: 60_000,
    });

    // Skip products search when unlicensed — the call can fail or flake and would
    // otherwise mark the whole Quick Start page as an error. Show 0 products instead.
    const totalProductsQuery = useQuery({
        queryKey: apiProductKeys.count(envId, {}),
        queryFn: () => searchApiProducts(envId, {}, STATS_PAGE, STATS_PER_PAGE),
        enabled: enabled && hasApiProducts,
        staleTime: 60_000,
    });

    const totalApis = totalApisQuery.data?.pagination?.totalCount ?? null;
    const totalProducts = hasApiProducts ? (totalProductsQuery.data?.pagination?.totalCount ?? null) : 0;

    const hasContent = totalApis !== null && (!hasApiProducts || totalProducts !== null) ? totalApis > 0 || (totalProducts ?? 0) > 0 : null;

    return {
        totalApis,
        totalProducts,
        hasContent,
        isLoading: totalApisQuery.isLoading || (hasApiProducts && totalProductsQuery.isLoading),
        isError: totalApisQuery.isError || (hasApiProducts && totalProductsQuery.isError),
    };
}
