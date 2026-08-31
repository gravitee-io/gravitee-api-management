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
import {
    AppLayout,
    AppSidebar,
    ContentHeader,
    LayoutSlotsProvider,
    buildLinearBreadcrumbs,
    useLayoutConfig,
    useLayoutSlots,
} from '@gravitee/graphene-core';
import { GioCatalogIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

import { Header } from './Header';
import { breadcrumbSegments } from './nav';
import { Sidebar } from './Sidebar';

function MarketplaceAppContext() {
    return (
        <div className="flex items-center gap-1.5 px-1.5">
            <span className="flex size-5 shrink-0 items-center justify-center [&_svg]:size-5" aria-hidden>
                <GioCatalogIcon className="size-5" />
            </span>
            <span className="max-w-48 truncate text-sm font-semibold">Agent Marketplace</span>
        </div>
    );
}

function PortalLayoutInner() {
    const { slots } = useLayoutSlots();
    const navigate = useNavigate();
    const { pathname } = useLocation();

    const navigation = useMemo(() => <Sidebar />, []);
    useLayoutConfig({ navigation }, [navigation]);

    const defaultBreadcrumbs = useMemo(() => buildLinearBreadcrumbs(navigate, breadcrumbSegments(pathname)), [navigate, pathname]);

    return (
        <AppLayout
            defaultSidebarMode="hover-expand"
            defaultTheme="system"
            fullHeight
            viewMode={slots.viewMode}
            contextExpanded={slots.contextExpanded}
            contextSidebar={slots.contextSidebar}
            contentVariant={slots.contentVariant}
            sidebar={<AppSidebar onLogoClick={() => navigate('/')} renderNavigation={() => slots.navigation} />}
            subheader={
                <ContentHeader
                    leading={slots.leading}
                    appContext={<MarketplaceAppContext />}
                    breadcrumbs={slots.breadcrumbs.length > 0 ? slots.breadcrumbs : defaultBreadcrumbs}
                    trailing={<Header />}
                />
            }
        >
            <Outlet />
        </AppLayout>
    );
}

export function PortalLayout() {
    return (
        <LayoutSlotsProvider>
            <PortalLayoutInner />
        </LayoutSlotsProvider>
    );
}
