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
import { Route, Routes } from 'react-router-dom';

import { ForceLoginRoute, LoginPage, ProtectedRoute, PublicOnlyRoute } from './auth';
import { CatalogPage } from './catalog/CatalogPage';
import { HomePage } from './home/HomePage';
import { PortalLayout } from './layout/PortalLayout';

function DashboardPlaceholder() {
    return <h1>Dashboard</h1>;
}

export function AppRoutes() {
    return (
        <Routes>
            <Route element={<PublicOnlyRoute />}>
                <Route path="/login" element={<LoginPage />} />
            </Route>
            <Route element={<PortalLayout />}>
                <Route element={<ForceLoginRoute />}>
                    <Route index element={<HomePage />} />
                    <Route path="/catalog" element={<CatalogPage />} />
                    <Route path="/catalog/:apiId" element={<h1>Agent</h1>} />
                </Route>
                <Route element={<ProtectedRoute />}>
                    <Route path="/dashboard" element={<DashboardPlaceholder />} />
                </Route>
            </Route>
        </Routes>
    );
}
