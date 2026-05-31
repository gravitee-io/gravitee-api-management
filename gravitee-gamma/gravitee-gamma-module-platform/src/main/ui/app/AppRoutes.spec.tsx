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
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import { AppRoutes } from './AppRoutes';

jest.mock('./PlatformToaster', () => ({
    PlatformToaster: () => <div data-testid="platform-toaster" />,
}));

jest.mock('@gravitee/gamma-modules-sdk/routing', () => ({
    useModuleRouting: () => ({
        activeNavKey: 'applications',
        navigateToKey: jest.fn(),
        rootPath: '/platform',
    }),
}));

jest.mock('@gravitee/graphene-core', () => {
    const actual = jest.requireActual('@gravitee/graphene-core') as object;
    return {
        ...actual,
        useLayoutConfig: jest.fn(),
        SidebarNavigation: () => null,
        buildLinearBreadcrumbs: () => [],
    };
});

jest.mock('../shared/console-settings', () => ({
    ConsoleSettingsProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

jest.mock('../shared/hooks/useEnvironmentPermissions', () => ({
    useEnvironmentPermissions: jest.fn(),
}));

jest.mock('../pages/ApplicationsPage', () => ({
    ApplicationsPage: () => <div data-testid="applications-page" />,
}));

jest.mock('../pages/RegisterApplicationPage', () => ({
    RegisterApplicationPage: () => <div data-testid="register-application-page" />,
}));

jest.mock('../features/applications/components/detail', () => ({
    ApplicationDetailLayout: () => <div data-testid="application-detail-layout" />,
    ApplicationDetailIndexRedirect: () => null,
}));

jest.mock('../pages/ApplicationDetailSubscriptionPage', () => ({
    ApplicationDetailSubscriptionPage: () => null,
}));

describe('AppRoutes', () => {
    it('mounts PlatformToaster for module-wide toast feedback', () => {
        render(
            <MemoryRouter initialEntries={['/applications']}>
                <AppRoutes />
            </MemoryRouter>,
        );

        expect(screen.getByTestId('platform-toaster')).not.toBeNull();
        expect(screen.getByTestId('applications-page')).not.toBeNull();
    });
});
