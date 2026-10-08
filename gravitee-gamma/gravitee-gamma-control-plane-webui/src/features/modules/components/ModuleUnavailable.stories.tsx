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

import { ModuleUnavailable } from './ModuleUnavailable';

const meta: Meta<typeof ModuleUnavailable> = {
    title: 'Modules/ModuleUnavailable',
    component: ModuleUnavailable,
    args: {
        moduleName: 'Agent Management',
        onReload: () => window.alert('Reload triggered'),
    },
    parameters: {
        docs: {
            description: {
                component:
                    'Shown in place of a module, inside the console shell, once every attempt to load it has failed or when it crashed while rendering.',
            },
        },
    },
};

export default meta;
type Story = StoryObj<typeof ModuleUnavailable>;

export const Default: Story = {};
