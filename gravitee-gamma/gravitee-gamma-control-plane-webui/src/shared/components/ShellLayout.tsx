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
import { AppLayout, AppSidebar, ContentHeader, LayoutSlotsProvider, TopNavUser, useLayoutSlots } from '@gravitee/graphene-core';
import { Globe } from 'lucide-react';
import type { ReactNode } from 'react';
import { useCallback, useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useLogout, useUser } from '../../features/auth';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { useEnvHrid, getPrimaryHrid } from '../../features/environment/environment.utils';
import type { GammaModule } from '../../features/modules';
import { HOME_ICON, MODULE_ICONS } from '../../features/modules';
import { buildPathnameAfterEnvironmentChange, pathSegmentsAfterEnvironment } from '../config/routes';

const GAMMA_APP_KEY = 'gamma-console';

const hostAppDefinition = {
    key: GAMMA_APP_KEY,
    label: 'Home',
    description: 'Gamma control plane',
    icon: <HOME_ICON className="size-5" />,
};

function moduleIcon(moduleId: string): ReactNode {
    const Icon = MODULE_ICONS[moduleId];
    return Icon ? <Icon className="size-5" /> : <Globe size={20} />;
}

function buildAppDefinitions(modules: readonly GammaModule[]) {
    return [
        hostAppDefinition,
        ...modules.map(m => ({
            key: m.id,
            label: m.name,
            description: `v${m.version}`,
            icon: moduleIcon(m.id),
        })),
    ];
}

function resolveActiveAppKey(pathname: string, envHrid: string, modules: readonly GammaModule[]): string {
    const rest = pathSegmentsAfterEnvironment(pathname, envHrid);
    if (rest.length === 0) {
        return GAMMA_APP_KEY;
    }
    if (rest[0] === 'home' || rest[0] === 'about') {
        return GAMMA_APP_KEY;
    }
    for (const m of modules) {
        if (rest[0] === m.id) {
            return m.id;
        }
    }
    return GAMMA_APP_KEY;
}

function ShellLayoutInner({ modules }: { readonly modules: readonly GammaModule[] }) {
    const user = useUser();
    const logout = useLogout();
    const navigate = useNavigate();
    const envHrid = useEnvHrid();
    const location = useLocation();
    const pathname = location.pathname;
    const { slots } = useLayoutSlots();

    const environments = useEnvironmentStore(s => s.environments);

    const apps = useMemo(() => buildAppDefinitions(modules), [modules]);
    const activeAppKey = useMemo(() => resolveActiveAppKey(pathname, envHrid, modules), [pathname, envHrid, modules]);

    const envItems = useMemo(
        () => environments.map(env => ({ key: getPrimaryHrid(env), label: env.name ?? getPrimaryHrid(env) })),
        [environments],
    );

    const handleAppChange = useCallback(
        (key: string) => {
            if (key === GAMMA_APP_KEY) {
                navigate(`/environments/${envHrid}/home`);
                return;
            }
            navigate(`/environments/${envHrid}/${key}`);
        },
        [envHrid, navigate],
    );

    const handleEnvironmentChange = useCallback(
        (newEnvHrid: string) => {
            const newPathname = buildPathnameAfterEnvironmentChange(pathname, envHrid, newEnvHrid);
            navigate({ pathname: newPathname, search: location.search, hash: location.hash });
        },
        [envHrid, location.hash, location.search, navigate, pathname],
    );

    const handleSignOut = useCallback(() => {
        void logout();
    }, [logout]);

    return (
        <AppLayout
            defaultSidebarMode="hover-expand"
            defaultTheme="system"
            fullHeight
            viewMode={slots.viewMode}
            contextExpanded={slots.contextExpanded}
            contextSidebar={slots.contextSidebar}
            contentVariant={slots.contentVariant}
            sidebar={
                <AppSidebar
                    apps={apps}
                    activeAppKey={activeAppKey}
                    onAppChange={handleAppChange}
                    renderNavigation={() => slots.navigation}
                    environments={envItems}
                    activeEnvironmentKey={envHrid}
                    onEnvironmentChange={handleEnvironmentChange}
                />
            }
            subheader={
                <ContentHeader
                    leading={slots.leading}
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
