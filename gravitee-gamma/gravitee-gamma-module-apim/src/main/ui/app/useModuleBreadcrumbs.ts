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
import { useModuleRouting, useToHref } from '@gravitee/gamma-modules-sdk/routing';
import { buildLinearBreadcrumbs } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

import { observability } from '../config/observability';
import { APIM_ROUTE_CONFIG, isRouteKey, ROUTES, type RouteKey } from '../config/routes';

export function useModuleBreadcrumbs(activeNavKey: RouteKey): ReturnType<typeof buildLinearBreadcrumbs> {
    const navigate = useNavigate();
    const toHref = useToHref();
    const { pathForKey } = useModuleRouting(APIM_ROUTE_CONFIG);

    return useMemo(() => {
        const observeSegments = observability.breadcrumbSegments(activeNavKey);
        if (observeSegments) {
            return buildLinearBreadcrumbs(
                navigate,
                observeSegments.map(({ label, routeKey }) => ({
                    label,
                    to: routeKey && isRouteKey(routeKey) ? pathForKey(routeKey) : undefined,
                })),
                { toHref },
            );
        }
        return buildLinearBreadcrumbs(navigate, [{ label: ROUTES[activeNavKey].label }]);
    }, [activeNavKey, navigate, pathForKey, toHref]);
}
