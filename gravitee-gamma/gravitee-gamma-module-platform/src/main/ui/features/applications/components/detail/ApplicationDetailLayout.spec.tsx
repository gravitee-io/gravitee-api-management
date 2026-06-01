/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { permissionService } from '@gravitee/gamma-modules-sdk';
import { render, screen } from '@testing-library/react';
import { isValidElement, type ReactNode } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';

jest.mock('@gravitee/gamma-modules-sdk', () => ({
    ...jest.requireActual<object>('@gravitee/gamma-modules-sdk'),
    permissionService: {
        load: jest.fn(),
        clear: jest.fn(),
        hasAllOf: jest.fn(() => true),
        hasAnyOf: jest.fn(() => true),
        getAllPermissions: jest.fn(() => []),
        subscribe: jest.fn(() => () => {}),
        getSnapshot: jest.fn(() => 0),
    },
}));

jest.mock('../../hooks/useApplicationDetail', () => ({
    useApplicationDetail: jest.fn(() => ({ data: null, isLoading: false, isError: false })),
}));

jest.mock('../../hooks/useApplicationPermissions', () => ({
    useApplicationPermissions: jest.fn(() => ({
        permissionsReady: true,
        isError: false,
        refetch: jest.fn(),
    })),
}));

let capturedLayoutConfig: Record<string, unknown> | null = null;

jest.mock('@gravitee/graphene-core', () => ({
    Badge: ({ children }: { children?: ReactNode }) => <span>{children}</span>,
    Skeleton: () => <div data-testid="skeleton" />,
    useLayoutConfig: jest.fn((config: Record<string, unknown>) => {
        capturedLayoutConfig = config;
    }),
    ContextSidebar: ({ children, header }: { children?: ReactNode; header?: ReactNode }) => (
        <div data-testid="context-sidebar">
            {header}
            {children}
        </div>
    ),
    ContextToggleButton: () => <button type="button" aria-label="Toggle context sidebar" />,
}));

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

jest.mock('./ApplicationDetailSidebarNav', () => ({
    ApplicationDetailSidebarNav: () => <nav data-testid="application-detail-sidebar-nav" />,
}));

jest.mock('./ApplicationDetailProtectedOutlet', () => ({
    ApplicationDetailProtectedOutlet: () => <div data-testid="application-detail-protected-outlet" />,
}));

jest.mock('./ApplicationDetailPermissionsError', () => ({
    ApplicationDetailPermissionsError: ({ onRetry, onBack }: { onRetry: () => void; onBack?: () => void }) => (
        <div data-testid="application-detail-permissions-error">
            <button type="button" onClick={onRetry}>
                Retry
            </button>
            {onBack ? (
                <button type="button" onClick={onBack}>
                    Back
                </button>
            ) : null}
        </div>
    ),
}));

import { ApplicationDetailIndexRedirect, ApplicationDetailLayout } from './ApplicationDetailLayout';
import { ApplicationDetailContext } from '../../context/ApplicationDetailContext';
import { useApplicationDetail } from '../../hooks/useApplicationDetail';
import { useApplicationPermissions } from '../../hooks/useApplicationPermissions';
import type { ApplicationListItem } from '../../types/application';

const mockUseApplicationDetail = useApplicationDetail as jest.Mock;
const mockUseApplicationPermissions = useApplicationPermissions as jest.Mock;

function baseApplication(overrides: Partial<ApplicationListItem> = {}): ApplicationListItem {
    return {
        id: 'app-1',
        name: 'My Application',
        status: 'ACTIVE',
        created_at: 1_000,
        updated_at: 1_000,
        ...overrides,
    } as ApplicationListItem;
}

function renderLayout(applicationId = 'app-1', path = `/applications/${applicationId}/general`) {
    capturedLayoutConfig = null;
    render(
        <MemoryRouter initialEntries={[path]}>
            <Routes>
                <Route path="applications/:applicationId" element={<ApplicationDetailLayout />}>
                    <Route path="general" element={<div data-testid="general-page" />} />
                </Route>
            </Routes>
        </MemoryRouter>,
    );
}

function expectContextSidebarMarkup(...expected: string[]) {
    const sidebar = capturedLayoutConfig?.contextSidebar;
    expect(isValidElement(sidebar)).toBe(true);
    const markup = renderToStaticMarkup(sidebar);
    for (const text of expected) {
        expect(markup).toContain(text);
    }
}

describe('ApplicationDetailLayout', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        capturedLayoutConfig = null;
        mockUseApplicationDetail.mockReturnValue({ data: baseApplication(), isLoading: false, isError: false });
        mockUseApplicationPermissions.mockReturnValue({
            permissionsReady: true,
            isError: false,
            refetch: jest.fn(),
        });
    });

    it('configures context layout with applications breadcrumb and app name', () => {
        renderLayout();
        expect(capturedLayoutConfig?.viewMode).toBe('context');
        const breadcrumbs = capturedLayoutConfig?.breadcrumbs as { label: string; href?: string }[];
        expect(breadcrumbs[0]).toEqual({ label: 'Applications', href: '/applications' });
        expect(breadcrumbs[1]).toEqual({ label: 'My Application' });
        expect(capturedLayoutConfig?.leading).toBeTruthy();
    });

    it('renders protected outlet in the main content area', () => {
        renderLayout();
        expect(screen.queryByTestId('application-detail-protected-outlet')).not.toBeNull();
    });

    it('shows load error when application fetch fails', () => {
        mockUseApplicationDetail.mockReturnValue({ data: null, isLoading: false, isError: true });
        renderLayout();
        expect(screen.queryByText(/failed to load application/i)).not.toBeNull();
        expect(screen.queryByTestId('application-detail-protected-outlet')).toBeNull();
    });

    it('shows permissions error UI when permissions fetch fails', () => {
        mockUseApplicationPermissions.mockReturnValue({
            permissionsReady: false,
            isError: true,
            refetch: jest.fn(),
        });
        renderLayout();
        expect(screen.queryByTestId('application-detail-permissions-error')).not.toBeNull();
    });

    it('renders application name in the context sidebar header', () => {
        renderLayout();
        expectContextSidebarMarkup('My Application', 'data-testid="application-detail-sidebar-nav"');
    });
});

describe('ApplicationDetailIndexRedirect', () => {
    beforeEach(() => {
        jest.spyOn(permissionService, 'hasAnyOf').mockReturnValue(true);
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('redirects the index route to the first accessible tab', () => {
        render(
            <MemoryRouter initialEntries={['/applications/app-1']}>
                <ApplicationDetailContext.Provider
                    value={{
                        application: baseApplication(),
                        isLoading: false,
                        permissionsReady: true,
                        permissionsError: false,
                        refetchPermissions: jest.fn(),
                    }}
                >
                    <Routes>
                        <Route path="applications/:applicationId" element={<Outlet />}>
                            <Route index element={<ApplicationDetailIndexRedirect />} />
                            <Route path="general" element={<div data-testid="general-page" />} />
                            <Route path="overview" element={<div data-testid="overview-page" />} />
                        </Route>
                    </Routes>
                </ApplicationDetailContext.Provider>
            </MemoryRouter>,
        );

        expect(screen.queryByTestId('overview-page')).not.toBeNull();
    });
});
