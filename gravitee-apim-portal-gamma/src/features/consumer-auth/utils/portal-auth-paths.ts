/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export interface PortalAuthPaths {
    readonly loginPath: string;
    readonly signupPath: string;
    readonly defaultRedirectPath: string;
    readonly invitePath: (token: string) => string;
}

export function getStandaloneAuthPaths(portalId: string, slug?: string): PortalAuthPaths {
    const defaultRedirectPath = slug ? `/portals/${portalId}/${slug}` : `/portals/${portalId}`;

    return {
        loginPath: `/portals/${portalId}/login`,
        signupPath: `/portals/${portalId}/signup`,
        defaultRedirectPath,
        invitePath: token => `/portals/${portalId}/invite/${token}`,
    };
}

export function getEditorAuthPaths(portalId: string, slug?: string): PortalAuthPaths {
    const defaultRedirectPath = slug ? `/portals/${portalId}/edit/${slug}` : `/portals/${portalId}/edit`;

    return {
        loginPath: `/portals/${portalId}/login`,
        signupPath: `/portals/${portalId}/signup`,
        defaultRedirectPath,
        invitePath: token => `/portals/${portalId}/invite/${token}`,
    };
}

export function isAuthRoutePath(pathname: string, portalId: string): boolean {
    const prefix = `/portals/${portalId}/`;
    return (
        pathname === `${prefix}login`
        || pathname === `${prefix}signup`
        || pathname.startsWith(`${prefix}invite/`)
    );
}
