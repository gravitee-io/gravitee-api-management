import { SidebarNavigation, Toaster, useLayoutConfig } from '@gravitee/graphene-core';
import { useCallback, useMemo } from 'react';
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { NAV_GROUPS } from '../config/navigation';
import { ROUTES, navigateToNavKey, resolveModulePath } from '../config/routes';
import { buildLinearBreadcrumbs } from '../lib/buildLinearBreadcrumbs';
import { ModuleErrorBoundary } from './ModuleErrorBoundary';
import { AgentsPage } from './features/policy-management/pages/AgentsPage';
import { ApisPage } from './features/policy-management/pages/ApisPage';
import { CustomPoliciesPage } from './features/policy-management/pages/CustomPoliciesPage';
import { EventsPage } from './features/policy-management/pages/EventsPage';
import { LlmsPage } from './features/policy-management/pages/LlmsPage';
import { McpsPage } from './features/policy-management/pages/McpsPage';
import { ActionsPage } from './features/policy-structure/ActionsPage';
import { EntitiesPage } from './features/policy-structure/EntitiesPage';
import { SchemaPage } from './features/policy-structure/SchemaPage';

function ModuleLayout() {
    const location = useLocation();
    const navigate = useNavigate();

    const { modulePrefix, activeNavKey } = useMemo(() => resolveModulePath(location.pathname), [location.pathname]);

    const handleNavSelect = useCallback(
        (key: string) => {
            navigateToNavKey(navigate, modulePrefix, key, location.pathname);
        },
        [navigate, modulePrefix, location.pathname],
    );

    const breadcrumbs = useMemo(() => {
        const pageLabel = ROUTES[activeNavKey].label;
        const segments = location.pathname.split('/').filter(Boolean);
        let hostPrefix = '';
        if (segments[0] === 'environments' && segments.length >= 3) {
            hostPrefix = `/${segments[0]}/${segments[1]}/${segments[2]}`;
        } else if (modulePrefix) {
            hostPrefix = `/${modulePrefix}`;
        }
        const rootPath = hostPrefix ? `${hostPrefix}/mcps` : '/mcps';
        return buildLinearBreadcrumbs(navigate, [{ label: 'Authorization', to: rootPath }, { label: pageLabel }]);
    }, [activeNavKey, navigate, modulePrefix, location.pathname]);

    useLayoutConfig(
        {
            navigation: <SidebarNavigation groups={NAV_GROUPS} activeItemKey={activeNavKey} onItemSelect={handleNavSelect} />,
            breadcrumbs,
        },
        [activeNavKey, breadcrumbs, handleNavSelect],
    );

    return <Outlet />;
}

export function AppRoutes() {
    return (
        <ModuleErrorBoundary>
            <Routes>
                <Route element={<ModuleLayout />}>
                    <Route index element={<Navigate to="mcps" replace />} />
                    <Route path="dashboard" element={<Navigate to="../mcps" replace />} />
                    <Route path="mcps" element={<McpsPage />} />
                    <Route path="agents" element={<AgentsPage />} />
                    <Route path="llms" element={<LlmsPage />} />
                    <Route path="apis" element={<ApisPage />} />
                    <Route path="events" element={<EventsPage />} />
                    <Route path="custom-policies" element={<CustomPoliciesPage />} />
                    <Route path="schema" element={<SchemaPage />} />
                    <Route path="entities" element={<EntitiesPage />} />
                    <Route path="actions" element={<ActionsPage />} />
                </Route>
            </Routes>
            <Toaster position="top-right" richColors closeButton />
        </ModuleErrorBoundary>
    );
}
