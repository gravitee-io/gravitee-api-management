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

import { ModulesLoadFailed } from './ModulesLoadFailed';
import { ShellLayout } from '../../../shared/components/ShellLayout';
import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { buildEnvironment, buildUser, TEST_CONFIG } from '../../../testing/factories';
import { useAuthStore } from '../../auth/auth.store';
import { EnvironmentGuard } from '../../environment';
import { useEnvironmentStore } from '../../environment/environment.store';

/** The shell needs a signed-in user and an environment around the page. */
function ConsoleContext({ children }: { readonly children: ReactNode }) {
    useEffect(() => {
        const environment = buildEnvironment({ id: 'env-default-id', hrids: ['default'], name: 'Default environment' });
        useBootstrapStore.setState({ config: TEST_CONFIG, loading: false, error: null });
        useAuthStore.setState({ user: buildUser({ firstname: 'John', lastname: 'Doe', displayName: 'John Doe' }) });
        useEnvironmentStore.setState({
            organizationId: TEST_CONFIG.organizationId,
            environmentId: environment.id,
            environments: [environment],
            currentEnvironment: environment,
            initialized: true,
        });
        return () => {
            useBootstrapStore.setState({ config: null, loading: false, error: null });
            useAuthStore.setState({ user: null });
            useEnvironmentStore.getState().reset();
        };
    }, []);

    return <>{children}</>;
}

const meta: Meta<typeof ModulesLoadFailed> = {
    title: 'Modules/ModulesLoadFailed',
    component: ModulesLoadFailed,
    args: {
        retrying: false,
        onRetry: () => window.alert('Retry triggered'),
    },
    parameters: {
        docs: {
            description: {
                component:
                    'Shown in place of a page when the list of modules could not be loaded. The address is kept: once a retry loads the list, the page asked opens.',
            },
        },
    },
};

export default meta;
type Story = StoryObj<typeof ModulesLoadFailed>;

export const Default: Story = {};

/** A retry is running: the button stays focusable but does nothing until the retry ends. */
export const Retrying: Story = {
    args: { retrying: true },
};

/** The whole page, as a direct link to a module opens it: the console shell, with the message in place of the page. */
export const InTheConsole: Story = {
    parameters: { fullPage: true },
    render: args => (
        <ConsoleContext>
            {/* Mirrors AppRoutes: without the list, the shell has no module and any other address shows the message. */}
            <MemoryRouter initialEntries={['/environments/default/aim/agents']}>
                <Routes>
                    <Route path="/environments/:envHrid" element={<ShellLayout modules={[]} />}>
                        <Route element={<EnvironmentGuard />}>
                            <Route path="*" element={<ModulesLoadFailed {...args} />} />
                        </Route>
                    </Route>
                </Routes>
            </MemoryRouter>
        </ConsoleContext>
    ),
};
