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
import { ThemeProvider } from '@gravitee/graphene-core';
import '@gravitee/graphene-core/fonts';
import '@gravitee/graphene-core/styles';
import type { Preview } from '@storybook/react';

const preview: Preview = {
    parameters: {
        layout: 'fullscreen',
        controls: { expanded: true },
    },
    decorators: [
        Story => (
            <ThemeProvider defaultMode="light">
                <div style={{ padding: '24px', background: 'var(--color-background)', minHeight: '100vh' }}>
                    <Story />
                </div>
            </ThemeProvider>
        ),
    ],
};

export default preview;
