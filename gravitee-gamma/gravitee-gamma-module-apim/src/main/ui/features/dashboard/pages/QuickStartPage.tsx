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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { Alert, AlertDescription, Skeleton } from '@gravitee/graphene-core';
import { useLocation, useNavigate } from 'react-router-dom';

import { APIM_OVERVIEW_TOUR_ID, useOnboarding } from '../../../app/onboarding';
import { APIM_ROUTE_CONFIG } from '../../../config/routes';
import { DashboardEmptyLanding, DashboardView } from '../components';
import { useDashboardStats } from '../hooks/useDashboardStats';

function QuickStartPageSkeleton() {
    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-2">
                    <Skeleton className="h-8 w-52 rounded" />
                    <Skeleton className="h-4 w-80 rounded" />
                </div>
                <div className="flex gap-2">
                    <Skeleton className="h-9 w-40 rounded" />
                    <Skeleton className="h-9 w-36 rounded" />
                </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
                <Skeleton className="rounded-xl" style={{ height: '8rem' }} />
                <Skeleton className="rounded-xl" style={{ height: '8rem' }} />
            </div>
            <div className="grid grid-cols-2 gap-4">
                <Skeleton className="rounded-xl" style={{ height: '20rem' }} />
                <Skeleton className="rounded-xl" style={{ height: '20rem' }} />
            </div>
        </div>
    );
}

export function QuickStartPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { navigateToKey, modulePrefix } = useModuleRouting(APIM_ROUTE_CONFIG);
    const { openTour } = useOnboarding();
    const stats = useDashboardStats();

    // Mirror buildModuleNavPath from the SDK: /environments/{env}/{module}/{subPath}
    const goTo = (subPath: string) => {
        if (modulePrefix) {
            const idx = location.pathname.startsWith('/environments/') ? location.pathname.indexOf('/', 14) : -1;
            const envBase = idx > 0 ? location.pathname.slice(0, idx) : '';
            navigate(`${envBase}/${modulePrefix}/${subPath}`);
        } else {
            navigate(`/${subPath}`);
        }
    };

    // Show skeleton while env is resolving (queries idle → isLoading=false in TanStack v5)
    // or while the first fetch is in-flight. Only drop it once we have a definitive answer.
    if (stats.hasContent === null && !stats.isError) {
        return <QuickStartPageSkeleton />;
    }

    if (stats.isError) {
        return (
            <Alert variant="destructive" className="max-w-lg">
                <AlertDescription>Failed to load dashboard data. Please refresh the page.</AlertDescription>
            </Alert>
        );
    }

    if (stats.hasContent) {
        return (
            <DashboardView
                totalApis={stats.totalApis}
                totalProducts={stats.totalProducts}
                onCreateProxy={() => goTo('apis/new')}
                onCreateProduct={() => goTo('api-products/new')}
                onGoToApis={() => navigateToKey('apis')}
                onGoToApiProducts={() => navigateToKey('api-products')}
                onStartTour={() => openTour(APIM_OVERVIEW_TOUR_ID)}
            />
        );
    }

    return <DashboardEmptyLanding onCreateProxy={() => goTo('apis/new')} />;
}
