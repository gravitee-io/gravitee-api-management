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
import React from 'react';
import { Route, Routes } from 'react-router-dom';

import { LoginPage, ProtectedRoute, PublicOnlyRoute } from '../features/auth';
import { type GammaModule, RemoteModuleRoute, useGammaModules } from '../features/modules';
import { AboutPage } from '../pages/AboutPage';
import { HomePage } from '../pages/HomePage';

export function App() {
    const { modules, loading, error } = useGammaModules();

    return (
        <React.Suspense fallback={<p>Loading…</p>}>
            <Routes>
                <Route element={<PublicOnlyRoute />}>
                    <Route path="/login" element={<LoginPage />} />
                </Route>
                <Route element={<ProtectedRoute />}>
                    <Route path="/" element={<HomePage modules={modules} loading={loading} error={error} />} />
                    <Route path="/about" element={<AboutPage />} />
                    {modules.map((m: GammaModule) => (
                        <Route key={m.id} path={`/${m.id}/*`} element={<RemoteModuleRoute module={m} />} />
                    ))}
                </Route>
            </Routes>
        </React.Suspense>
    );
}

export default App;
