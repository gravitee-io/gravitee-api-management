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
import { buildModuleNavPath } from '@gravitee/gamma-modules-sdk/routing';

import type { OrganizationEnvironment } from '../types/user';

export function primaryEnvironmentSegment(environment: { id: string; hrids?: readonly string[] }): string {
    return environment.hrids?.[0] ?? environment.id;
}

export function resolveEnvironmentSegment(environmentId: string, environments: readonly OrganizationEnvironment[]): string {
    const environment = environments.find(item => item.id.toLowerCase() === environmentId.toLowerCase());
    return environment ? primaryEnvironmentSegment(environment) : environmentId;
}

function pathnameForEnvironment(pathname: string, environmentSegment: string): string {
    if (!pathname.startsWith('/environments/')) {
        return pathname;
    }
    return pathname.replace(/^\/environments\/[^/]+/, `/environments/${environmentSegment}`);
}

/**
 * Builds a detail URL for an inherited resource shown on the user detail page.
 * Uses {@link buildModuleNavPath} so federated and standalone mounting stay consistent with the SDK.
 */
export function buildCrossModuleResourcePath(
    moduleId: string,
    routePath: string,
    resourceEnvironmentId: string,
    environments: readonly OrganizationEnvironment[],
    currentPathname: string,
): string {
    const environmentSegment = resolveEnvironmentSegment(resourceEnvironmentId, environments);
    const pathname = pathnameForEnvironment(currentPathname, environmentSegment);
    return buildModuleNavPath(moduleId, routePath, pathname);
}

export function buildInheritedApiDetailPath(
    apiId: string,
    resourceEnvironmentId: string,
    environments: readonly OrganizationEnvironment[],
    currentPathname: string,
): string {
    return buildCrossModuleResourcePath('apim', `apis/${apiId}`, resourceEnvironmentId, environments, currentPathname);
}

export function buildInheritedApiProductDetailPath(
    apiProductId: string,
    resourceEnvironmentId: string,
    environments: readonly OrganizationEnvironment[],
    currentPathname: string,
): string {
    return buildCrossModuleResourcePath(
        'apim',
        `api-products/${apiProductId}/configuration/general`,
        resourceEnvironmentId,
        environments,
        currentPathname,
    );
}

export function buildInheritedApplicationDetailPath(
    applicationId: string,
    resourceEnvironmentId: string,
    environments: readonly OrganizationEnvironment[],
    currentPathname: string,
): string {
    return buildCrossModuleResourcePath('platform', `applications/${applicationId}`, resourceEnvironmentId, environments, currentPathname);
}
