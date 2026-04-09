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
import { AppLayout, AppSidebar, ContentHeader, LayoutSlotsProvider, TopNavUser, useLayoutSlots } from '@gravitee/graphene';
import { Globe, Home } from 'lucide-react';
import { useCallback, useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useLogout, useUser } from '../../features/auth';
import type { GammaModule } from '../../features/modules';

const GAMMA_APP_KEY = 'gamma-console';

const hostAppDefinition = {
    key: GAMMA_APP_KEY,
    label: 'Home',
    description: 'Gamma control plane',
    icon: <Home size={20} />,
};

function buildAppDefinitions(modules: readonly GammaModule[]) {
    return [
        hostAppDefinition,
        ...modules.map(m => ({
            key: m.id,
            label: m.name,
            description: `v${m.version}`,
            icon: <Globe size={20} />,
        })),
    ];
}

function resolveActiveAppKey(pathname: string, modules: readonly GammaModule[]): string {
    for (const m of modules) {
        if (pathname === `/${m.id}` || pathname.startsWith(`/${m.id}/`)) {
            return m.id;
        }
    }
    return GAMMA_APP_KEY;
}

function ShellLayoutInner({ modules }: { readonly modules: readonly GammaModule[] }) {
    const user = useUser();
    const logout = useLogout();
    const navigate = useNavigate();
    const location = useLocation();
    const pathname = location.pathname;
    const { slots } = useLayoutSlots();

    const apps = useMemo(() => buildAppDefinitions(modules), [modules]);
    const activeAppKey = useMemo(() => resolveActiveAppKey(pathname, modules), [pathname, modules]);

    const handleAppChange = useCallback(
        (key: string) => {
            if (key === GAMMA_APP_KEY) {
                navigate('/');
                return;
            }
            navigate(`/${key}`);
        },
        [navigate],
    );

    const handleSignOut = useCallback(() => {
        void logout();
    }, [logout]);

    return (
        <AppLayout
            defaultSidebarMode="hover-expand"
            defaultTheme="system"
            fullHeight
            sidebar={
                <AppSidebar
                    apps={apps}
                    activeAppKey={activeAppKey}
                    onAppChange={handleAppChange}
                    renderNavigation={() => slots.navigation}
                />
            }
            subheader={
                <ContentHeader
                    breadcrumbs={slots.breadcrumbs}
                    trailing={user ? <TopNavUser name={user.displayName} email={user.email} onSignOut={handleSignOut} /> : undefined}
                />
            }
        >
            <Outlet />
        </AppLayout>
    );
}

export function ShellLayout({ modules }: { readonly modules: readonly GammaModule[] }) {
    return (
        <LayoutSlotsProvider>
            <ShellLayoutInner modules={modules} />
        </LayoutSlotsProvider>
    );
}
