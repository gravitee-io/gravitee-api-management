import { buildLinearBreadcrumbs, SidebarNavigation, useLayoutConfig } from '@gravitee/graphene-core';
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { useMemo } from 'react';
import { Outlet, Route, Routes, useNavigate } from 'react-router-dom';

import { NAV_GROUPS } from '../config/navigation';
import { AI_FLEET_ROUTE_CONFIG } from '../config/routes';
import { FleetPage } from '../pages/FleetPage';
import { EventsPage } from '../pages/EventsPage';

function ModuleLayout() {
    const navigate = useNavigate();
    const { activeNavKey, navigateToKey, rootPath } = useModuleRouting(AI_FLEET_ROUTE_CONFIG);

    const breadcrumbs = useMemo(
        () =>
            buildLinearBreadcrumbs(navigate, [
                { label: 'AI Fleet', to: rootPath },
                { label: AI_FLEET_ROUTE_CONFIG.routes[activeNavKey].label },
            ]),
        [activeNavKey, navigate, rootPath],
    );

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={navigateToKey} />,
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, navigateToKey],
    );

    return <Outlet />;
}

export function AppRoutes() {
    return (
        <Routes>
            <Route element={<ModuleLayout />}>
                <Route index element={<FleetPage />} />
                <Route path="fleet" element={<FleetPage />} />
                <Route path="events" element={<EventsPage />} />
            </Route>
        </Routes>
    );
}
