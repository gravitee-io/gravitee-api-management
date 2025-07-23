/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import type { Meta, StoryObj } from '@storybook/angular';
import { applicationConfig, moduleMetadata } from '@storybook/angular';

import { GraviteeMarkdownEditorComponent } from './gravitee-markdown-editor.component';
import { GRAVITEE_MONACO_EDITOR_CONFIG } from '../gravitee-monaco-wrapper/data/gravitee-monaco-editor-config';

const meta: Meta<GraviteeMarkdownEditorComponent> = {
  title: 'Gravitee Markdown/Editor',
  component: GraviteeMarkdownEditorComponent,
  parameters: {
    layout: 'padded',
  },
  decorators: [
    moduleMetadata({
      imports: [GraviteeMarkdownEditorComponent],
    }),
    applicationConfig({
      providers: [
        {
          provide: GRAVITEE_MONACO_EDITOR_CONFIG,
          useValue: {
            baseUrl: '..',
            theme: 'vs' as const,
          },
        },
      ],
    }),
  ],
  argTypes: {
    darkTheme: {
      control: 'boolean',
      description: 'Enable dark theme for the editor',
    },
    highlightTheme: {
      control: 'select',
      options: ['github', 'github-dark', 'vs', 'vs-dark'],
      description: 'Syntax highlighting theme for the preview',
    },
    contentChange: {
      action: 'contentChange',
      description: 'Event emitted when content changes',
    },
    errorChange: {
      action: 'errorChange',
      description: 'Event emitted when validation errors occur',
    },
  },
};

export default meta;
type Story = StoryObj<GraviteeMarkdownEditorComponent>;

export const WithSampleContent: Story = {
  args: {
    darkTheme: false,
    highlightTheme: 'github',
  },
  render: args => ({
    props: {
      ...args,
      content: `# Welcome to Gravitee

This is a **markdown editor** with live preview.

## Features

- *Real-time* preview
- Syntax highlighting
- Split-panel layout

### Code Example

\`\`\`javascript
function hello() {
  console.log("Hello, Gravitee!");
}
\`\`\`

> This editor makes it easy to write and preview markdown content.
`,
    },
  }),
  parameters: {
    docs: {
      description: {
        story: 'Markdown editor with sample content demonstrating various markdown features.',
      },
    },
  },
};
