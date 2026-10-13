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
import type { Meta, StoryObj } from '@storybook/react';
import { type ReactNode, useEffect } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import { useEnvironmentStore } from './environment.store';
import { EnvironmentGuard } from './EnvironmentGuard';
import { EnvironmentsLoadFailed } from './EnvironmentsLoadFailed';
import { RootRedirect } from './RootRedirect';
import { ShellLayout } from '../../shared/components/ShellLayout';
import { useBootstrapStore } from '../../shared/config/bootstrap.store';
import { buildUser, TEST_CONFIG } from '../../testing/factories';
import { useAuthStore } from '../auth/auth.store';
import type { GammaModule } from '../modules';

const MODULES: readonly GammaModule[] = [
    { id: 'aim', name: 'AIM Module', version: '1.0.0', remoteName: 'gravitee_gamma_module_aim', exposedModule: 'App' },
];

interface StoryArgs {
    retrying: boolean;
}

function Decorator({ children, retrying }: StoryArgs & { readonly children: ReactNode }) {
    useEffect(() => {
        useBootstrapStore.setState({ config: TEST_CONFIG, loading: false, error: null });
        useAuthStore.setState({ user: buildUser({ firstname: 'John', lastname: 'Doe', displayName: 'John Doe' }) });
        useEnvironmentStore.setState({
            organizationId: TEST_CONFIG.organizationId,
            loadFailed: true,
            initialized: true,
            loading: retrying,
        });
        return () => {
            useBootstrapStore.setState({ config: null, loading: false, error: null });
            useAuthStore.setState({ user: null });
            useEnvironmentStore.getState().reset();
        };
    }, [retrying]);

    return <>{children}</>;
}

const meta: Meta<StoryArgs> = {
    title: 'Environment/EnvironmentsLoadFailed',
    component: EnvironmentsLoadFailed,
    args: { retrying: false },
    parameters: {
        docs: {
            description: {
                component:
                    'Shown in place of the console pages when the environments could not be loaded. "Retry" loads them again without reloading the page: in these stories the platform is not there, so a retry fails right away.',
            },
        },
    },
    decorators: [
        (Story, ctx) => (
            <Decorator retrying={ctx.args.retrying}>
                <Story />
            </Decorator>
        ),
    ],
    render: () => <EnvironmentsLoadFailed />,
};

export default meta;
type Story = StoryObj<StoryArgs>;

export const Default: Story = {};

/** A retry is running: the button stays focusable but does nothing until the retry ends. */
export const Retrying: Story = {
    args: { retrying: true },
};

/** The whole page, as a direct link to a module opens it: the console shell, with the message in place of the page. */
export const InTheConsole: Story = {
    parameters: { fullPage: true },
    render: () => (
        // Mirrors AppRoutes: the guard sits between the shell and the page.
        <MemoryRouter initialEntries={['/environments/default/aim']}>
            <Routes>
                <Route path="/environments/:envHrid" element={<ShellLayout modules={MODULES} />}>
                    <Route element={<EnvironmentGuard />}>
                        <Route path="*" element={null} />
                    </Route>
                </Route>
            </Routes>
        </MemoryRouter>
    ),
};

/** The whole page, as `/` shows it right after a sign-in: no shell, the message alone in the middle. */
export const OnABarePage: Story = {
    parameters: { fullPage: true },
    render: () => (
        <MemoryRouter>
            <RootRedirect />
        </MemoryRouter>
    ),
};
