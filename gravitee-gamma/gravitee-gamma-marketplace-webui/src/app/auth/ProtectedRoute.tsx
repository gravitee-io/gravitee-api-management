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
import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { useForceLoginEnabled, useUser } from './auth.selectors';

function loginRedirect(pathname: string): string {
    const redirect = pathname !== '/' ? `?redirect=${encodeURIComponent(pathname)}` : '';
    return `/login${redirect}`;
}

export function ProtectedRoute() {
    const user = useUser();
    const location = useLocation();

    if (!user) {
        return <Navigate to={loginRedirect(location.pathname)} replace />;
    }

    return <Outlet />;
}

export function PublicOnlyRoute() {
    const user = useUser();

    if (user) {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
}

export function ForceLoginRoute() {
    const user = useUser();
    const forceLogin = useForceLoginEnabled();
    const location = useLocation();

    if (forceLogin && !user) {
        return <Navigate to={loginRedirect(location.pathname)} replace />;
    }

    return <Outlet />;
}
