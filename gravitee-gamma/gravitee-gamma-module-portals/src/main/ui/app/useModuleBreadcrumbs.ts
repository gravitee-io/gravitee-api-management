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
import { type ModuleRoutingResult, useToHref } from '@gravitee/gamma-modules-sdk/routing';
import { buildLinearBreadcrumbs, type LinearBreadcrumbSegment } from '@gravitee/graphene-core';
import { useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ROUTES, type RouteKey } from '../config/routes';

export function useModuleBreadcrumbs(
    activeNavKey: RouteKey,
    { pathForKey }: Pick<ModuleRoutingResult<RouteKey>, 'pathForKey'>,
): ReturnType<typeof buildLinearBreadcrumbs> {
    const navigate = useNavigate();
    const toHref = useToHref();
    const { portalId } = useParams<{ portalId: string }>();

    return useMemo(() => {
        const segments: LinearBreadcrumbSegment[] = [
            { label: 'Developer Portals', to: pathForKey('dashboard') },
            { label: ROUTES[activeNavKey].label, to: pathForKey(activeNavKey) },
        ];
        if (portalId) segments.push({ label: portalId });
        return buildLinearBreadcrumbs(navigate, segments, { toHref });
    }, [activeNavKey, navigate, pathForKey, portalId, toHref]);
}
