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
import { useEffect } from 'react';

import { showStartupError } from './startup-error';

function StartupErrorPage() {
    useEffect(() => {
        showStartupError(new Error('Failed to fetch bootstrap config: 503'));
    }, []);
    return <div id="root" />;
}

const meta: Meta<typeof StartupErrorPage> = {
    title: 'Startup/StartupError',
    component: StartupErrorPage,
    parameters: {
        docs: {
            description: {
                component:
                    'Replaces the console, in plain DOM, when it cannot start: its code failed to load, or the bootstrap configuration could not be read.',
            },
        },
    },
};

export default meta;
type Story = StoryObj<typeof StartupErrorPage>;

export const Default: Story = {};
