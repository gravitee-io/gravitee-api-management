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
import { MemoryRouter } from 'react-router-dom';

import { LoginPage } from './LoginPage';
import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { TEST_CONFIG } from '../../../testing/factories';
import { useAuthStore } from '../auth.store';

interface StoryArgs {
    sessionCheckFailed: boolean;
}

function Decorator({ children, sessionCheckFailed }: StoryArgs & { readonly children: ReactNode }) {
    useEffect(() => {
        useBootstrapStore.setState({ config: TEST_CONFIG, loading: false, error: null });
        useAuthStore.setState({ user: null, sessionCheckFailed });
        return () => {
            useBootstrapStore.setState({ config: null, loading: false, error: null });
            useAuthStore.setState({ sessionCheckFailed: false });
        };
    }, [sessionCheckFailed]);

    return <MemoryRouter initialEntries={['/login']}>{children}</MemoryRouter>;
}

const meta: Meta<StoryArgs> = {
    title: 'Auth/LoginPage',
    component: LoginPage,
    args: { sessionCheckFailed: false },
    parameters: {
        fullPage: true,
        docs: {
            description: {
                component: 'The sign-in page of the Gamma console, shown to anyone the console does not know as signed in.',
            },
        },
    },
    decorators: [
        (Story, ctx) => (
            <Decorator sessionCheckFailed={ctx.args.sessionCheckFailed}>
                <Story />
            </Decorator>
        ),
    ],
    render: () => <LoginPage />,
};

export default meta;
type Story = StoryObj<StoryArgs>;

export const Default: Story = {};

/**
 * The request for the signed-in user failed at startup, and not with a 401: the user may well be signed in.
 * The page says so, as nothing else tells this case from a sign-out.
 */
export const SessionCheckFailed: Story = {
    args: { sessionCheckFailed: true },
};
