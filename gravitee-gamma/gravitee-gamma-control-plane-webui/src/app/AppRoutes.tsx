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
import { Navigate, Route, Routes } from 'react-router-dom';

import {
    ActivationPage,
    LoginPage,
    ProtectedRoute,
    PublicOnlyRoute,
    RegistrationEnabledRoute,
    ResetPasswordPage,
    SignUpPage,
} from '../features/auth';
import { EnvironmentGuard, RootRedirect } from '../features/environment';
import { type GammaModule, RemoteModuleRoute, useGammaModules } from '../features/modules';
import {
    CloudAccountTokensPage,
    CloudCustomReportersPage,
    CloudGeneralSettingsPage,
    CloudInviteMemberPage,
    CloudLayout,
    CloudMembersPage,
    CloudOverviewPage,
    CloudSettingsLayout,
    CloudSettingsSectionPage,
    CloudSsoPage,
    CloudTokensPage,
} from '../pages/cloud';
import { HomePage } from '../pages/home';
import { MyAccountPage } from '../pages/my-account';
import { TasksPage } from '../pages/tasks';
import { ContentSkeleton } from '../shared/components/ContentSkeleton';
import { RouteLayout } from '../shared/components/RouteLayout';
import { ShellLayout } from '../shared/components/ShellLayout';

export function AppRoutes() {
    const { modules, loading, error, retry } = useGammaModules();

    if (loading && modules.length === 0) {
        return (
            <Routes>
                <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
                <Route path="/registration/:token" element={<ActivationPage />} />
                <Route element={<PublicOnlyRoute />}>
                    <Route path="/login" element={<LoginPage />} />
                    <Route element={<RegistrationEnabledRoute />}>
                        <Route path="/sign-up" element={<SignUpPage />} />
                    </Route>
                </Route>
                <Route element={<ProtectedRoute />}>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={[]} />}>
                        <Route element={<EnvironmentGuard />}>
                            <Route path="*" element={<ContentSkeleton />} />
                        </Route>
                    </Route>
                    <Route path="/" element={<RootRedirect />} />
                    <Route path="*" element={<RootRedirect />} />
                </Route>
            </Routes>
        );
    }

    return (
        <Routes>
            <Route path="/reset-password/:token" element={<ResetPasswordPage />} />
            {/* Outside every guard: the link is the only way in, whether or not someone is signed in or
                registration is still on, and the server has the only explanation worth giving. */}
            <Route path="/registration/:token" element={<ActivationPage />} />
            <Route element={<PublicOnlyRoute />}>
                <Route path="/login" element={<LoginPage />} />
                <Route element={<RegistrationEnabledRoute />}>
                    <Route path="/sign-up" element={<SignUpPage />} />
                </Route>
            </Route>
            <Route element={<ProtectedRoute />}>
                <Route path="/environments/:envHrid" element={<ShellLayout modules={modules} />}>
                    <Route element={<EnvironmentGuard />}>
                        <Route element={<RouteLayout />}>
                            <Route path="home" element={<HomePage modules={modules} loading={loading} error={error} onRetry={retry} />} />
                            <Route path="tasks" element={<TasksPage />} />
                            <Route path="my-account" element={<MyAccountPage />} />
                        </Route>
                        {!modules.some((m: GammaModule) => m.id === 'cloud') && (
                            <Route path="cloud/*" element={<CloudLayout />}>
                                <Route path="dashboard" element={<CloudOverviewPage />} />
                                <Route path="settings/*" element={<CloudSettingsLayout />}>
                                    <Route path="general" element={<CloudGeneralSettingsPage />} />
                                    <Route path="custom-reporters" element={<CloudCustomReportersPage />} />
                                    <Route path="account-tokens" element={<CloudAccountTokensPage />} />
                                    <Route path="cloud-tokens" element={<CloudTokensPage />} />
                                    <Route path="sso" element={<CloudSsoPage />} />
                                    <Route path="private-networks" element={<CloudSettingsSectionPage />} />
                                    <Route path="members" element={<CloudMembersPage />} />
                                    <Route path="invite-member" element={<CloudInviteMemberPage />} />
                                    <Route index element={<Navigate to="general" replace />} />
                                </Route>
                                <Route index element={<Navigate to="dashboard" replace />} />
                            </Route>
                        )}
                        {modules.map((m: GammaModule) => (
                            <Route key={m.id} path={`${m.id}/*`} element={<RemoteModuleRoute module={m} />} />
                        ))}
                        <Route index element={<Navigate to="home" replace />} />
                    </Route>
                </Route>
                <Route path="/" element={<RootRedirect />} />
                <Route path="*" element={<RootRedirect />} />
            </Route>
        </Routes>
    );
}
