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
import { buildModuleNavPath, type ModuleRoutingResult, useToHref } from '@gravitee/gamma-modules-sdk/routing';
import { buildLinearBreadcrumbs } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import { observability } from '../config/observability';
import { isRouteKey, ROUTES, type RouteKey } from '../config/routes';

export function useModuleBreadcrumbs(
    activeNavKey: RouteKey,
    { modulePrefix, pathForKey }: Pick<ModuleRoutingResult<RouteKey>, 'modulePrefix' | 'pathForKey'>,
): ReturnType<typeof buildLinearBreadcrumbs> {
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const toHref = useToHref();

    return useMemo(() => {
        const observeSegments = observability.breadcrumbSegments(activeNavKey);
        if (observeSegments) {
            // The observability lib's route keys are module-relative paths, and it can name a page this module has not declared.
            const pathForSegment = (routeKey: string) =>
                isRouteKey(routeKey) ? pathForKey(routeKey) : buildModuleNavPath(modulePrefix, routeKey, pathname);
            return buildLinearBreadcrumbs(
                navigate,
                observeSegments.map(({ label, routeKey }) => ({ label, to: routeKey ? pathForSegment(routeKey) : undefined })),
                { toHref },
            );
        }
        return buildLinearBreadcrumbs(navigate, [{ label: ROUTES[activeNavKey].label }]);
    }, [activeNavKey, modulePrefix, navigate, pathForKey, pathname, toHref]);
}
