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
import type { Meta, StoryObj } from '@storybook/angular';
import { moduleMetadata } from '@storybook/angular';

import { GmdInstallMcpComponent } from './gmd-install-mcp.component';

export default {
  title: 'Gravitee Markdown/Components/Install MCP',
  component: GmdInstallMcpComponent,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    moduleMetadata({
      imports: [GmdInstallMcpComponent],
    }),
  ],
} as Meta<GmdInstallMcpComponent>;

export const HttpTransport: StoryObj<GmdInstallMcpComponent> = {
  render: () => ({
    template: `
      <div class="gmd-install-mcp-story">
        <gmd-install-mcp
          name="weather"
          url="https://api.example.com/mcp"
          clients="cursor,vscode,claude-desktop"
        />
      </div>
    `,
  }),
};

export const StdioTransport: StoryObj<GmdInstallMcpComponent> = {
  render: () => ({
    template: `
      <div class="gmd-install-mcp-story">
        <gmd-install-mcp
          name="weather-local"
          transport="stdio"
          command="npx"
          args='["-y","@acme/weather-mcp"]'
          clients="cursor,vscode,claude-desktop"
        />
      </div>
    `,
  }),
};

export const CursorOnly: StoryObj<GmdInstallMcpComponent> = {
  render: () => ({
    template: `
      <div class="gmd-install-mcp-story">
        <gmd-install-mcp
          name="weather"
          url="https://api.example.com/mcp"
          clients="cursor"
        />
      </div>
    `,
  }),
};

export const DarkSurface: StoryObj<GmdInstallMcpComponent> = {
  render: () => ({
    template: `
      <div class="gmd-install-mcp-story gmd-install-mcp-story--dark">
        <gmd-install-mcp
          name="weather"
          url="https://api.example.com/mcp"
          clients="cursor,vscode,claude-desktop"
        />
      </div>
    `,
  }),
};

export const MissingConfiguration: StoryObj<GmdInstallMcpComponent> = {
  render: () => ({
    template: `
      <div class="gmd-install-mcp-story">
        <gmd-install-mcp />
      </div>
    `,
  }),
};
