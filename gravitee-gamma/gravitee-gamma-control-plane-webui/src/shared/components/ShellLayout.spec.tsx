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
import { useLayoutConfig } from '@gravitee/graphene-core';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter, Route, Routes, useNavigate, type NavigateFunction } from 'react-router-dom';

import { ShellLayout } from './ShellLayout';
import { EnvironmentGuard } from '../../features/environment';
import { useEnvironmentStore } from '../../features/environment/environment.store';
import type { GammaModule } from '../../features/modules';
import { EnvironmentRenderProbe } from '../../testing/EnvironmentRenderProbe';
import { resetAllStores, seedBootstrap, seedEnvironments, seedUser } from '../../testing/helpers';

const MODULES: GammaModule[] = [{ id: 'apim', name: 'API Management', version: '1.0.0', remoteName: 'apim', exposedModule: 'Module' }];

function NavigateHandle({ onReady }: { readonly onReady: (navigate: NavigateFunction) => void }) {
    onReady(useNavigate());
    return null;
}

describe('ShellLayout environment switching', () => {
    let renders: Array<{ pathname: string; environmentId: string }> = [];
    let navigate: NavigateFunction;

    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        seedEnvironments();
        renders = [];
    });

    /** Mirrors AppRoutes: the guard sits between the shell and the module area. */
    function renderShell(initialPath: string) {
        return render(
            <MemoryRouter initialEntries={[initialPath]}>
                <NavigateHandle onReady={n => (navigate = n)} />
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={MODULES} />}>
                        <Route element={<EnvironmentGuard />}>
                            <Route
                                path="*"
                                element={
                                    <EnvironmentRenderProbe
                                        onRender={(pathname, environmentId) => renders.push({ pathname, environmentId })}
                                    />
                                }
                            />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
    }

    function mismatchedRenders() {
        return renders.filter(r => {
            const hrid = /^\/environments\/([^/]+)/.exec(r.pathname)?.[1];
            return hrid === 'env-1' ? r.environmentId !== 'env-1-id' : hrid === 'env-2' && r.environmentId !== 'env-2-id';
        });
    }

    async function switchToEnvironment2() {
        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: /Switch environment/ }));
        await user.click(await screen.findByText('Environment 2'));
    }

    it('should land on the module root when switching from a deep module page', async () => {
        renderShell('/environments/env-1/apim/apis/api-1/endpoints');

        await switchToEnvironment2();

        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/apim'));
    });

    it('should land on the host area root when switching from a deep host page', async () => {
        renderShell('/environments/env-1/tasks/task-1');

        await switchToEnvironment2();

        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/tasks'));
    });

    it('should expose the theme options in the header account menu', async () => {
        const user = userEvent.setup();
        seedUser();
        renderShell('/environments/env-1/home');

        await user.click(await screen.findByRole('button', { name: 'Account menu' }));

        expect(screen.getByRole('menuitemradio', { name: 'Light' })).toBeTruthy();
        expect(screen.getByRole('menuitemradio', { name: 'Dark' })).toBeTruthy();
        expect(screen.getByRole('menuitemradio', { name: 'System' })).toBeTruthy();
    });

    it('should keep my-account when switching environments', async () => {
        renderShell('/environments/env-1/my-account');

        await switchToEnvironment2();

        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/my-account'));
    });

    it('should never render the new environment path while the store still holds the previous environment', async () => {
        renderShell('/environments/env-1/apim/apis/api-1/endpoints');

        await switchToEnvironment2();

        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/apim'));
        expect(mismatchedRenders()).toEqual([]);
    });

    it('should never pair a restored environment path with the environment left behind, on back navigation', async () => {
        renderShell('/environments/env-1/apim/apis/api-1/endpoints');
        await switchToEnvironment2();
        await waitFor(() => expect(renders.at(-1)?.pathname).toBe('/environments/env-2/apim'));

        await act(async () => {
            navigate(-1);
        });

        await waitFor(() => expect(useEnvironmentStore.getState().environmentId).toBe('env-1-id'));
        expect(mismatchedRenders()).toEqual([]);
    });

    it('should never pair a deep-linked environment path with the environment seeded at startup', async () => {
        renderShell('/environments/env-2/apim/apis/api-1/endpoints');

        await waitFor(() => expect(useEnvironmentStore.getState().environmentId).toBe('env-2-id'));
        expect(mismatchedRenders()).toEqual([]);
    });

    it('should update the environment store when switching', async () => {
        renderShell('/environments/env-1/apim/apis/api-1/endpoints');

        await switchToEnvironment2();

        await waitFor(() => expect(useEnvironmentStore.getState().currentEnvironment?.id).toBe('env-2-id'));
    });
});

describe('ShellLayout app switcher', () => {
    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        seedEnvironments();
    });

    function renderPortalsSwitcher() {
        const modules: GammaModule[] = [
            { id: 'portals', name: 'Developer Portals', version: '1.0.0', remoteName: 'portals', exposedModule: 'Module' },
        ];
        render(
            <MemoryRouter initialEntries={['/environments/env-1/home']}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={modules} />}>
                        <Route path="*" element={null} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
    }

    it('should list the products in catalog order, whatever order the backend returns the modules in', async () => {
        const backendOrder: GammaModule[] = ['edge', 'aim', 'portals', 'apim', 'esm', 'platform', 'authz'].map(id => ({
            id,
            name: `${id} plugin`,
            version: '1.0.0',
            remoteName: id,
            exposedModule: 'Module',
        }));
        render(
            <MemoryRouter initialEntries={['/environments/env-1/home']}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={backendOrder} />}>
                        <Route path="*" element={null} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        await userEvent.setup().click(screen.getByRole('button', { name: 'Home' }));

        const items = await screen.findAllByRole('menuitem');
        expect(items.map(item => item.textContent)).toEqual(
            [
                'Home',
                'Agent Management',
                'API Management',
                'Event Stream Management',
                'Authorization Management',
                'Developer Portals',
                'Edge Management',
                'Platform Management',
            ].map(label => expect.stringMatching(`^${label}`)),
        );
    });

    it('should set Home apart from the products', async () => {
        const modules: GammaModule[] = [{ id: 'apim', name: 'apim plugin', version: '1.0.0', remoteName: 'apim', exposedModule: 'Module' }];
        render(
            <MemoryRouter initialEntries={['/environments/env-1/home']}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={modules} />}>
                        <Route path="*" element={null} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        await userEvent.setup().click(screen.getByRole('button', { name: 'Home' }));

        const menu = await screen.findByRole('menu');
        expect(within(menu).getByRole('separator')).toBeTruthy();
    });

    it('should open Developer Portals in a new tab on the classic console portal editor', async () => {
        const open = jest.spyOn(window, 'open').mockImplementation(() => null);

        const modules: GammaModule[] = [
            { id: 'apim', name: 'API Management', version: '1.0.0', remoteName: 'apim', exposedModule: 'Module' },
            { id: 'portals', name: 'Developer Portals', version: '1.0.0', remoteName: 'portals', exposedModule: 'Module' },
        ];
        render(
            <MemoryRouter initialEntries={['/environments/env-1/home']}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={modules} />}>
                        <Route path="*" element={null} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        const user = userEvent.setup();
        await user.click(screen.getByRole('button', { name: 'Home' }));
        await user.click(await screen.findByRole('menuitem', { name: /Developer Portals/ }));

        expect(open).toHaveBeenCalledWith('http://console.test/#!/env-1/_portal/navigation', '_blank', 'noopener,noreferrer');
        open.mockRestore();
    });

    it('should link Developer Portals to the classic console portal editor its click opens', async () => {
        renderPortalsSwitcher();

        await userEvent.setup().click(screen.getByRole('button', { name: 'Home' }));

        expect((await screen.findByRole('menuitem', { name: /Developer Portals/ })).closest('a')?.getAttribute('href')).toBe(
            'http://console.test/#!/env-1/_portal/navigation',
        );
    });

    it('should not link Developer Portals when the classic console URL is not configured', async () => {
        seedBootstrap({ consoleUrl: undefined });
        renderPortalsSwitcher();

        await userEvent.setup().click(screen.getByRole('button', { name: 'Home' }));

        expect((await screen.findByRole('menuitem', { name: /Developer Portals/ })).closest('a')).toBeNull();
    });

    it('should link each app to its page under the router base path, so it can be opened in a new tab', async () => {
        const user = userEvent.setup();
        render(
            <MemoryRouter basename="/console" initialEntries={['/console/environments/env-1/home']}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={MODULES} />}>
                        <Route path="*" element={null} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        await user.click(await screen.findByRole('button', { name: 'Home' }));

        expect((await screen.findByRole('menuitem', { name: /Home/ })).closest('a')?.getAttribute('href')).toBe(
            '/console/environments/env-1/home',
        );
        expect(
            screen
                .getByRole('menuitem', { name: /API Management/ })
                .closest('a')
                ?.getAttribute('href'),
        ).toBe('/console/environments/env-1/apim');
    });
});

describe('ShellLayout with Account Management', () => {
    const MODULES_WITH_CLOUD: GammaModule[] = [
        { id: 'apim', name: 'API Management', version: '1.0.0', remoteName: 'apim', exposedModule: 'Module' },
        { id: 'account', name: 'Account Management', version: '1.0.0', scope: 'account', remoteName: 'cloud', exposedModule: 'Module' },
    ];

    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        seedEnvironments();
    });

    function renderShellAt(path: string) {
        render(
            <MemoryRouter initialEntries={[path]}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={MODULES_WITH_CLOUD} />}>
                        <Route path="*" element={null} />
                    </Route>
                    <Route path="/account" element={<ShellLayout modules={MODULES_WITH_CLOUD} />}>
                        <Route path="*" element={null} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
    }

    it('should pin Account Management in place of Home, and list Home first among the products', async () => {
        renderShellAt('/environments/env-1/home');

        await userEvent.setup().click(screen.getByRole('button', { name: 'Home' }));

        const items = await screen.findAllByRole('menuitem');
        expect(items.map(item => item.textContent)).toEqual(
            ['Account Management', 'Home', 'API Management'].map(label => expect.stringMatching(`^${label}`)),
        );
        const separator = within(screen.getByRole('menu')).getByRole('separator');
        expect(items[0].compareDocumentPosition(separator) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(items[1].compareDocumentPosition(separator) & Node.DOCUMENT_POSITION_PRECEDING).toBeTruthy();
        expect(items[1].textContent).toContain('Overview and quick actions');
    });

    it('should keep Home pinned when the Cloud module is not deployed', async () => {
        render(
            <MemoryRouter initialEntries={['/environments/env-1/home']}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={MODULES} />}>
                        <Route path="*" element={null} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        await userEvent.setup().click(screen.getByRole('button', { name: 'Home' }));

        const items = await screen.findAllByRole('menuitem');
        expect(items.map(item => item.textContent)).toEqual(['Home', 'API Management'].map(label => expect.stringMatching(`^${label}`)));
        const separator = within(screen.getByRole('menu')).getByRole('separator');
        expect(items[0].compareDocumentPosition(separator) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(screen.queryByRole('menuitem', { name: /Account Management/ })).toBeNull();
    });

    it('should pin any account module in place of Home, whatever its id', async () => {
        const modules: GammaModule[] = [
            { id: 'apim', name: 'API Management', version: '1.0.0', remoteName: 'apim', exposedModule: 'Module' },
            { id: 'billing', name: 'Billing', version: '1.0.0', scope: 'account', remoteName: 'billing', exposedModule: 'Module' },
        ];
        render(
            <MemoryRouter initialEntries={['/environments/env-1/home']}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={modules} />}>
                        <Route path="*" element={null} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );

        await userEvent.setup().click(screen.getByRole('button', { name: 'Home' }));

        const items = await screen.findAllByRole('menuitem');
        expect(items.map(item => item.textContent)).toEqual(
            ['Billing', 'Home', 'API Management'].map(label => expect.stringMatching(`^${label}`)),
        );
        expect(items[0].closest('a')?.getAttribute('href')).toBe('/billing');
    });

    it('should link Account Management to the Cloud module', async () => {
        renderShellAt('/environments/env-1/home');

        await userEvent.setup().click(screen.getByRole('button', { name: 'Home' }));

        expect((await screen.findByRole('menuitem', { name: /Account Management/ })).closest('a')?.getAttribute('href')).toBe('/account');
    });

    it('should hide the environment selector in Account Management', () => {
        renderShellAt('/account/overview');

        expect(screen.getByRole('button', { name: 'Account Management' })).toBeTruthy();
        expect(screen.queryByRole('button', { name: /Switch environment/ })).toBeNull();
    });

    it('should link the other apps to the environment of the session from Account Management', async () => {
        useEnvironmentStore.setState({ currentEnvironment: useEnvironmentStore.getState().environments[1] });
        renderShellAt('/account/overview');

        await userEvent.setup().click(screen.getByRole('button', { name: 'Account Management' }));

        expect((await screen.findByRole('menuitem', { name: /^Home/ })).closest('a')?.getAttribute('href')).toBe(
            '/environments/env-2/home',
        );
        expect(
            screen
                .getByRole('menuitem', { name: /API Management/ })
                .closest('a')
                ?.getAttribute('href'),
        ).toBe('/environments/env-2/apim');
    });

    it('should keep the environment selector in the other apps', () => {
        renderShellAt('/environments/env-1/apim');

        expect(screen.getByRole('button', { name: /Switch environment/ })).toBeTruthy();
    });
});

describe('ShellLayout document title', () => {
    beforeEach(() => {
        resetAllStores();
        seedBootstrap();
        seedEnvironments();
        document.title = 'Gravitee Gamma';
    });

    function PageWithBreadcrumbs({ labels }: { readonly labels: readonly string[] }) {
        useLayoutConfig({ breadcrumbs: labels.map(label => ({ label })) }, [labels]);
        return null;
    }

    function renderShellAt(path: string, page: ReactNode) {
        return render(
            <MemoryRouter initialEntries={[path]}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={MODULES} />}>
                        <Route path="*" element={page} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
    }

    it('should name the tab after the page, most specific first, then the app', async () => {
        renderShellAt('/environments/env-1/apim/apis/api-1/overview', <PageWithBreadcrumbs labels={['API Proxies', 'Badge Demo']} />);

        await waitFor(() => expect(document.title).toBe('Badge Demo · API Proxies · API Management · Gravitee Gamma'));
    });

    it('should name the tab after the app while the module has not set breadcrumbs yet', async () => {
        renderShellAt('/environments/env-1/apim', null);

        await waitFor(() => expect(document.title).toBe('API Management · Gravitee Gamma'));
    });

    it('should leave the Home app out of host page titles', async () => {
        renderShellAt('/environments/env-1/tasks', <PageWithBreadcrumbs labels={['Tasks']} />);

        await waitFor(() => expect(document.title).toBe('Tasks · Gravitee Gamma'));
    });

    it('should follow in-app navigation without remounting the shell', async () => {
        let navigate!: NavigateFunction;
        render(
            <MemoryRouter initialEntries={['/environments/env-1/apim']}>
                <NavigateHandle onReady={n => (navigate = n)} />
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={MODULES} />}>
                        <Route path="apim/*" element={<PageWithBreadcrumbs labels={['API Proxies']} />} />
                        <Route path="tasks" element={<PageWithBreadcrumbs labels={['Tasks']} />} />
                    </Route>
                </Routes>
            </MemoryRouter>,
        );
        await waitFor(() => expect(document.title).toBe('API Proxies · API Management · Gravitee Gamma'));

        act(() => navigate('/environments/env-1/tasks'));

        await waitFor(() => expect(document.title).toBe('Tasks · Gravitee Gamma'));
    });

    it('should restore the previous title when the shell goes away', async () => {
        const { unmount } = renderShellAt('/environments/env-1/apim', <PageWithBreadcrumbs labels={['API Proxies']} />);
        await waitFor(() => expect(document.title).toBe('API Proxies · API Management · Gravitee Gamma'));

        unmount();

        expect(document.title).toBe('Gravitee Gamma');
    });
});
