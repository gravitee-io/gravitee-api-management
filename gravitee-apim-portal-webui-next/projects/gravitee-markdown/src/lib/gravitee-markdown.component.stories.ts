/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { GraviteeMarkdownComponent } from './gravitee-markdown.component';

export default {
  title: 'Components/GraviteeMarkdown',
  component: GraviteeMarkdownComponent,
  decorators: [
    moduleMetadata({
      imports: [GraviteeMarkdownComponent],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A simple markdown component for displaying markdown content.',
      },
    },
  },
} as Meta<GraviteeMarkdownComponent>;

export const Default: StoryObj<GraviteeMarkdownComponent> = {
  render: () => ({
    template: `
      <div style="padding: 20px;">
        <h2>Gravitee Markdown Component</h2>
        <gmd-hello-world></gmd-hello-world>
      </div>
    `,
  }),
};
