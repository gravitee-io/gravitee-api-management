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
import { AppContextBar, AppLayout, AppSidebar, ContentHeader, LayoutSlotsProvider, useLayoutSlots } from '@gravitee/graphene-core';
import { Globe } from 'lucide-react';
import type { ReactNode } from 'react';
import { Suspense, useCallback, useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

import { ContentSkeleton } from './ContentSkeleton';
import { UserMenu } from './UserMenu';
import { useAvatarCacheBust, useLogout, useUser } from '../../features/auth';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import { getPrimaryHrid, useEnvHrid } from '../../features/environment/environment.utils';
import {
    PORTALS_MODULE_ID,
    HOME_ICON,
    MODULE_ICONS,
    buildPortalNextEditorUrl,
    findModuleProduct,
    orderByCatalog,
    type GammaModule,
} from '../../features/modules';
import { currentUserAvatarUrl } from '../../pages/my-account/myAccount.mapping';
import { PendingTasksBadge } from '../../pages/tasks';
import { useBootstrapStore } from '../config/bootstrap.store';
import { buildPathnameAfterEnvironmentChange, pathSegmentsAfterEnvironment } from '../config/routes';

const GAMMA_APP_KEY = 'gamma-console';

const hostAppDefinition = {
    key: GAMMA_APP_KEY,
    label: 'Home',
    icon: <HOME_ICON className="size-5" />,
    pinned: true,
};

function moduleIcon(moduleId: string): ReactNode {
    const Icon = MODULE_ICONS[moduleId];
    return Icon ? <Icon className="size-5" /> : <Globe size={20} />;
}

function buildAppDefinitions(modules: readonly GammaModule[]) {
    return [
        hostAppDefinition,
        ...orderByCatalog(modules).map(m => {
            const product = findModuleProduct(m.id);
            return {
                key: m.id,
                label: product?.label ?? m.name,
                description: product?.tagline ?? m.name,
                icon: moduleIcon(m.id),
            };
        }),
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
    const { pathname } = useLocation();
    const { slots } = useLayoutSlots();
    const cacheBust = useAvatarCacheBust();
    const config = useBootstrapStore(s => s.config);

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
            if (key === PORTALS_MODULE_ID) {
                if (config?.consoleUrl) {
                    window.open(buildPortalNextEditorUrl(config.consoleUrl, envHrid), '_blank', 'noopener,noreferrer');
                }
                return;
            }
            navigate(`/environments/${envHrid}/${key}`);
        },
        [config?.consoleUrl, envHrid, navigate],
    );

    // `search` and `hash` are dropped along with the sub-path -- they carry filters and tabs that
    // are just as environment-scoped. EnvironmentGuard owns adopting the new environment.
    const handleEnvironmentChange = useCallback(
        (newEnvHrid: string) => {
            navigate({ pathname: buildPathnameAfterEnvironmentChange(pathname, envHrid, newEnvHrid) });
        },
        [envHrid, navigate, pathname],
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
            banner={slots.banner}
            bannerSticky={slots.bannerSticky}
            sidebar={
                <AppSidebar
                    onLogoClick={() => navigate('/')}
                    renderNavigation={() => slots.navigation}
                    renderFooter={slots.footer ? () => slots.footer : undefined}
                />
            }
            subheader={
                <ContentHeader
                    appContext={
                        <AppContextBar
                            apps={apps}
                            activeAppKey={activeAppKey}
                            onAppChange={handleAppChange}
                            environments={envItems}
                            activeEnvironmentKey={envHrid}
                            onEnvironmentChange={handleEnvironmentChange}
                        />
                    }
                    leading={slots.leading}
                    breadcrumbs={slots.breadcrumbs}
                    trailing={
                        user ? (
                            <div className="flex items-center gap-3">
                                <PendingTasksBadge />
                                <UserMenu
                                    name={user.displayName}
                                    email={user.email}
                                    avatarSrc={
                                        user.id && config
                                            ? currentUserAvatarUrl(config.managementBaseURL, config.organizationId, user.id, cacheBust)
                                            : undefined
                                    }
                                    onMyAccount={() => navigate(`/environments/${envHrid}/my-account`)}
                                    onSignOut={handleSignOut}
                                />
                            </div>
                        ) : undefined
                    }
                />
            }
        >
            <Suspense fallback={<ContentSkeleton />}>
                <Outlet />
            </Suspense>
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
