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
import { Navigate, Outlet, useLocation, useSearchParams } from 'react-router-dom';

import { useUser } from '../auth.selectors';

export function ProtectedRoute() {
    const user = useUser();
    const location = useLocation();

    if (!user) {
        const redirect = location.pathname !== '/' ? `?redirect=${encodeURIComponent(location.pathname)}` : '';
        return <Navigate to={`/login${redirect}`} replace />;
    }

    return <Outlet />;
}

/**
 * The page asked before the login, when it is one of the console's, Home otherwise. The browser decides:
 * an address it resolves outside the console (a full URL, `/\host`, a tab between two slashes) would
 * make the navigation throw.
 */
function consolePath(redirect: string | null): string {
    if (!redirect) return '/';
    try {
        const url = new URL(redirect, window.location.origin);
        return url.origin === window.location.origin ? `${url.pathname}${url.search}${url.hash}` : '/';
    } catch {
        return '/';
    }
}

export function PublicOnlyRoute() {
    const user = useUser();
    const [searchParams] = useSearchParams();

    if (user) {
        return <Navigate to={consolePath(searchParams.get('redirect'))} replace />;
    }

    return <Outlet />;
}
