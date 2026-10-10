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

import { ModuleUpdating } from './ModuleUpdating';

const meta: Meta<typeof ModuleUpdating> = {
    title: 'Modules/ModuleUpdating',
    component: ModuleUpdating,
    args: {
        moduleName: 'Agent Management',
        attempting: false,
        onRetryNow: () => window.alert('Retry now triggered'),
    },
    parameters: {
        docs: {
            description: {
                component:
                    'Shown in place of a module, inside the console shell, once its load has kept failing for a while. The console keeps retrying and opens the module here as soon as it loads.',
            },
        },
    },
};

export default meta;
type Story = StoryObj<typeof ModuleUpdating>;

export const Default: Story = {};

/** An attempt is running: the button stays focusable but does nothing until the attempt ends. */
export const Attempting: Story = {
    args: { attempting: true },
};
