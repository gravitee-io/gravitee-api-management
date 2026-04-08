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
import { AppLayout, AppSidebar, ContentHeader, SidebarNavigation, TopNavUser } from '@gravitee/graphene';
import type { NavGroup } from '@gravitee/graphene';
import { Globe, Home, Info } from 'lucide-react';
import { useCallback, useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useLogout, useUser } from '../../features/auth';
import type { GammaModule } from '../../features/modules';

const GAMMA_APP_KEY = 'gamma-console';

const gammaApp = {
    key: GAMMA_APP_KEY,
    label: 'Gamma',
    description: 'Control plane',
    icon: <Globe size={20} />,
};

function buildNavGroups(modules: readonly GammaModule[]): NavGroup[] {
    const moduleItems = modules.map(m => ({
        key: m.id,
        title: m.name,
        icon: Globe,
    }));

    return [
        {
            label: 'Overview',
            items: [
                { key: 'home', title: 'Home', icon: Home },
                { key: 'about', title: 'About', icon: Info },
            ],
        },
        ...(moduleItems.length > 0
            ? [
                  {
                      label: 'Modules',
                      items: moduleItems,
                  } as NavGroup,
              ]
            : []),
    ];
}

function resolveActiveNavKey(pathname: string, modules: readonly GammaModule[]): string {
    if (pathname === '/' || pathname === '') {
        return 'home';
    }
    if (pathname === '/about' || pathname.startsWith('/about')) {
        return 'about';
    }
    for (const m of modules) {
        if (pathname === `/${m.id}` || pathname.startsWith(`/${m.id}/`)) {
            return m.id;
        }
    }
    return 'home';
}

export function ShellLayout({ modules }: { readonly modules: readonly GammaModule[] }) {
    const user = useUser();
    const logout = useLogout();
    const navigate = useNavigate();
    const location = useLocation();
    const pathname = location.pathname;

    const navGroups = useMemo(() => buildNavGroups(modules), [modules]);
    const activeItemKey = useMemo(() => resolveActiveNavKey(pathname, modules), [pathname, modules]);

    const handleNavSelect = useCallback(
        (key: string) => {
            if (key === 'home') {
                navigate('/');
                return;
            }
            if (key === 'about') {
                navigate('/about');
                return;
            }
            navigate(`/${key}`);
        },
        [navigate],
    );

    const breadcrumbs = useMemo(() => {
        if (pathname === '/' || pathname === '') {
            return [{ label: 'Home' }];
        }
        if (pathname === '/about' || pathname.startsWith('/about')) {
            return [{ label: 'Home', onClick: () => navigate('/') }, { label: 'About' }];
        }
        for (const m of modules) {
            if (pathname === `/${m.id}` || pathname.startsWith(`/${m.id}/`)) {
                const rest = pathname.slice(`/${m.id}`.length).replace(/^\//, '');
                const segments = rest ? rest.split('/').filter(Boolean) : [];
                const crumbs: { label: string; onClick?: () => void }[] = [{ label: 'Home', onClick: () => navigate('/') }];
                if (segments.length === 0) {
                    crumbs.push({ label: m.name });
                    return crumbs;
                }
                crumbs.push({ label: m.name, onClick: () => navigate(`/${m.id}`) });
                for (let i = 0; i < segments.length; i++) {
                    const label = segments[i];
                    const pathUpTo = `/${m.id}/${segments.slice(0, i + 1).join('/')}`;
                    const isLast = i === segments.length - 1;
                    crumbs.push(
                        isLast
                            ? { label }
                            : {
                                  label,
                                  onClick: () => navigate(pathUpTo),
                              },
                    );
                }
                return crumbs;
            }
        }
        return [{ label: 'Home' }];
    }, [pathname, modules, navigate]);

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
                    apps={[gammaApp]}
                    activeAppKey={GAMMA_APP_KEY}
                    renderNavigation={() => (
                        <SidebarNavigation groups={navGroups} activeItemKey={activeItemKey} onItemSelect={handleNavSelect} />
                    )}
                />
            }
            subheader={
                <ContentHeader
                    breadcrumbs={breadcrumbs}
                    trailing={user ? <TopNavUser name={user.displayName} email={user.email} onSignOut={handleSignOut} /> : undefined}
                />
            }
        >
            <Outlet />
        </AppLayout>
    );
}
