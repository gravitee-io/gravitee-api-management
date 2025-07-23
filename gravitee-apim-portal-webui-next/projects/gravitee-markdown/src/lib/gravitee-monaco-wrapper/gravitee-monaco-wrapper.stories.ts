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
import { moduleMetadata } from '@storybook/angular';

import { GRAVITEE_MONACO_EDITOR_CONFIG } from './data/gravitee-monaco-editor-config';
import { GraviteeMonacoWrapperComponent } from './gravitee-monaco-wrapper.component';
import { GraviteeMonacoWrapperService } from './gravitee-monaco-wrapper.service';

const meta: Meta<GraviteeMonacoWrapperComponent> = {
  title: 'Gravitee Markdown/Monaco Wrapper',
  component: GraviteeMonacoWrapperComponent,
  parameters: {
    layout: 'padded',
  },
  decorators: [
    moduleMetadata({
      providers: [
        GraviteeMonacoWrapperService,
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
    languageConfig: {
      control: 'object',
      description: 'Language configuration for the editor',
    },
    options: {
      control: 'object',
      description: 'Monaco Editor options',
    },
    disableMiniMap: {
      control: 'boolean',
      description: 'Disable the minimap',
    },
    disableAutoFormat: {
      control: 'boolean',
      description: 'Disable auto formatting',
    },
    singleLineMode: {
      control: 'boolean',
      description: 'Enable single line mode',
    },
  },
};

export default meta;
type Story = StoryObj<GraviteeMonacoWrapperComponent>;

export const Default: Story = {
  args: {
    languageConfig: {
      language: 'markdown',
    },
    options: {
      theme: 'vs',
      fontSize: 14,
      lineNumbers: 'on',
      wordWrap: 'on',
    },
    disableMiniMap: false,
    disableAutoFormat: false,
    singleLineMode: false,
  },
  parameters: {
    docs: {
      description: {
        story: 'Default Monaco Editor with markdown language support.',
      },
    },
  },
};

export const SingleLine: Story = {
  args: {
    ...Default.args,
    singleLineMode: true,
    options: {
      ...Default.args?.options,
      lineNumbers: 'off',
      minimap: { enabled: false },
    },
  },
  parameters: {
    docs: {
      description: {
        story: 'Monaco Editor in single line mode, useful for simple text input.',
      },
    },
  },
};

export const NoMiniMap: Story = {
  args: {
    ...Default.args,
    disableMiniMap: true,
    options: {
      ...Default.args?.options,
      minimap: { enabled: false },
    },
  },
  parameters: {
    docs: {
      description: {
        story: 'Monaco Editor without minimap for a cleaner interface.',
      },
    },
  },
};

export const ReadOnly: Story = {
  args: {
    ...Default.args,
    options: {
      ...Default.args?.options,
      readOnly: true,
    },
  },
  parameters: {
    docs: {
      description: {
        story: 'Read-only Monaco Editor for displaying content without editing.',
      },
    },
  },
};

export const LargeFont: Story = {
  args: {
    ...Default.args,
    options: {
      ...Default.args?.options,
      fontSize: 18,
      lineHeight: 24,
    },
  },
  parameters: {
    docs: {
      description: {
        story: 'Monaco Editor with larger font size for better readability.',
      },
    },
  },
};

export const DarkTheme: Story = {
  args: {
    ...Default.args,
    options: {
      ...Default.args?.options,
      theme: 'vs-dark',
    },
  },
  parameters: {
    docs: {
      description: {
        story: 'Monaco Editor with dark theme.',
      },
    },
  },
};

export const Compact: Story = {
  args: {
    ...Default.args,
    options: {
      ...Default.args?.options,
      fontSize: 12,
      lineHeight: 16,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
    },
  },
  parameters: {
    docs: {
      description: {
        story: 'Compact Monaco Editor with smaller font and no minimap.',
      },
    },
  },
};
