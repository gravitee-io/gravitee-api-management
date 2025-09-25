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
import { importProvidersFrom } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { applicationConfig, Meta, moduleMetadata, StoryObj } from '@storybook/angular';

import { CellComponent } from './cell/cell.component';
import { GridComponent } from './grid.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';

export default {
  title: 'Gravitee Markdown/Components/Grid',
  component: GridComponent,
  decorators: [
    moduleMetadata({
      imports: [GridComponent, CellComponent, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, FormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' }))],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A responsive grid component that can contain cell components. The columns specification is for the full-width view.',
      },
    },
  },
  argTypes: {
    columns: {
      control: { type: 'number', min: 1, max: 6 },
      description: 'Number of columns for the grid (limited to 1-6)',
    },
  },
} as Meta<GridComponent>;

export const Default: StoryObj<GridComponent> = {
  args: {
    columns: 2,
  },
  render: args => ({
    props: args,
    template: `
      <style>
        gmd-cell {
          display: block;
          padding: 8px;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          background-color: #f9f9f9;
          min-height: 40px;
        }
      </style>
      <div style="padding: 20px;">
        <h2>Grid Component (${args.columns} columns)</h2>
        <gmd-grid [columns]="columns">
          <gmd-cell>
            <h3>First Column</h3>
            <p>This is the first cell in the grid.</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Second Column</h3>
            <p>This is the second cell in the grid.</p>
          </gmd-cell>
        </gmd-grid>
      </div>
    `,
  }),
};
export const SingleColumn: StoryObj<GridComponent> = {
  args: {
    columns: 1,
  },
  render: args => ({
    props: args,
    template: `
      <style>
        gmd-cell {
          display: block;
          padding: 8px;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          background-color: #f9f9f9;
          min-height: 40px;
        }
      </style>
      <div style="padding: 20px;">
        <h2>Grid Component (${args.columns} column)</h2>
        <gmd-grid [columns]="columns">
          <gmd-cell>
            <h3>Single Column</h3>
            <p>This is a single column grid layout.</p>
          </gmd-cell>
        </gmd-grid>
      </div>
    `,
  }),
};
export const ThreeColumns: StoryObj<GridComponent> = {
  args: {
    columns: 3,
  },
  render: args => ({
    props: args,
    template: `
      <style>
        gmd-cell {
          display: block;
          padding: 8px;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          background-color: #f9f9f9;
          min-height: 40px;
        }
      </style>
      <div style="padding: 20px;">
        <h2>Grid Component (${args.columns} columns)</h2>
        <gmd-grid [columns]="columns">
          <gmd-cell>
            <h3>Column 1</h3>
            <p>First cell content</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Column 2</h3>
            <p>Second cell content</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Column 3</h3>
            <p>Third cell content</p>
          </gmd-cell>
        </gmd-grid>
      </div>
    `,
  }),
};
export const FourColumns: StoryObj<GridComponent> = {
  args: {
    columns: 4,
  },
  render: args => ({
    props: args,
    template: `
      <style>
        gmd-cell {
          display: block;
          padding: 8px;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          background-color: #f9f9f9;
          min-height: 40px;
        }
      </style>
      <div style="padding: 20px;">
        <h2>Grid Component (${args.columns} columns)</h2>
        <gmd-grid [columns]="columns">
          <gmd-cell>
            <h3>Item 1</h3>
            <p>Content for item 1</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 2</h3>
            <p>Content for item 2</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 3</h3>
            <p>Content for item 3</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 4</h3>
            <p>Content for item 4</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 5</h3>
            <p>Content for item 5</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 6</h3>
            <p>Content for item 6</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 7</h3>
            <p>Content for item 7</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 8</h3>
            <p>Content for item 8</p>
          </gmd-cell>
        </gmd-grid>
      </div>
    `,
  }),
};
export const SixColumns: StoryObj<GridComponent> = {
  args: {
    columns: 6,
  },
  render: args => ({
    props: args,
    template: `
      <style>
        gmd-cell {
          display: block;
          padding: 8px;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          background-color: #f9f9f9;
          min-height: 40px;
        }
      </style>
      <div style="padding: 20px;">
        <h2>Grid Component (${args.columns} columns - Maximum)</h2>
        <gmd-grid [columns]="columns">
          <gmd-cell>
            <h3>Item 1</h3>
            <p>Content for item 1</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 2</h3>
            <p>Content for item 2</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 3</h3>
            <p>Content for item 3</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 4</h3>
            <p>Content for item 4</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 5</h3>
            <p>Content for item 5</p>
          </gmd-cell>
          <gmd-cell>
            <h3>Item 6</h3>
            <p>Content for item 6</p>
          </gmd-cell>
        </gmd-grid>
        <div style="margin-top: 16px; padding: 12px; background: #e3f2fd; border-radius: 4px; font-size: 14px;">
          <strong>Note:</strong> Column input is limited to 1-6. Values outside this range are automatically clamped.
        </div>
      </div>
    `,
  }),
};
export const WithCellsAndRichHTML: StoryObj<GridComponent> = {
  args: {
    columns: 2,
  },
  render: args => ({
    props: args,
    template: `
      <div style="padding: 20px;">
        <h2>Grid with Rich HTML Content</h2>
        <gmd-grid [columns]="columns">
          <gmd-cell>
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px;">
              <h3 style="margin-top: 0; color: white;">🎨 Feature Card</h3>
              <p>This cell contains rich HTML with:</p>
              <ul>
                <li><strong>Gradient backgrounds</strong></li>
                <li><strong>Custom styling</strong></li>
                <li><strong>Emojis and icons</strong></li>
              </ul>
              <button style="background: rgba(255,255,255,0.2); border: 1px solid white; color: white; padding: 8px 16px; border-radius: 4px; cursor: pointer;">
                Learn More
              </button>
            </div>
          </gmd-cell>
          <gmd-cell>
            <div style="border-left: 4px solid #4CAF50; padding-left: 16px; background: #f8f9fa;">
              <h3 style="color: #2e7d32; margin-top: 0;">📊 Data Visualization</h3>
              <div style="display: flex; align-items: center; margin: 16px 0;">
                <div style="width: 60px; height: 60px; background: #4CAF50; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; margin-right: 16px;">
                  85%
                </div>
                <div>
                  <p style="margin: 0; font-weight: bold;">Performance Score</p>
                  <p style="margin: 0; color: #666; font-size: 14px;">Above average</p>
                </div>
              </div>
              <div style="background: #e8f5e8; padding: 12px; border-radius: 4px; margin-top: 16px;">
                <p style="margin: 0; font-size: 14px; color: #2e7d32;">
                  ✅ All systems operational
                </p>
              </div>
            </div>
          </gmd-cell>
        </gmd-grid>
      </div>
    `,
  }),
};
export const WithPlainHTML: StoryObj<GridComponent> = {
  args: {
    columns: 3,
  },
  render: args => ({
    props: args,
    template: `
      <div style="padding: 20px;">
        <h2>Grid with Plain HTML Content (No Cell Components)</h2>
        <gmd-grid [columns]="columns">
          <article style="background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 8px; padding: 16px;">
            <h3 style="color: #856404; margin-top: 0;">📝 Article Card</h3>
            <p style="color: #856404; margin-bottom: 12px;">This is a plain HTML article element inside the grid without using gmd-cell components.</p>
            <time style="font-size: 12px; color: #6c757d;">Published: March 15, 2024</time>
          </article>

          <section style="background: #d1ecf1; border: 1px solid #bee5eb; border-radius: 8px; padding: 16px;">
            <h3 style="color: #0c5460; margin-top: 0;">🔧 Section Block</h3>
            <p style="color: #0c5460; margin-bottom: 12px;">This is a section element with different styling, showing how the grid works with any HTML content.</p>
            <div style="display: flex; gap: 8px;">
              <span style="background: #17a2b8; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px;">Tag 1</span>
              <span style="background: #17a2b8; color: white; padding: 4px 8px; border-radius: 4px; font-size: 12px;">Tag 2</span>
            </div>
          </section>

          <div style="background: #f8d7da; border: 1px solid #f5c6cb; border-radius: 8px; padding: 16px;">
            <h3 style="color: #721c24; margin-top: 0;">⚠️ Alert Box</h3>
            <p style="color: #721c24; margin-bottom: 12px;">This is a div element styled as an alert box, demonstrating the grid's flexibility with different HTML elements.</p>
            <button style="background: #dc3545; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; font-size: 14px;">
              Take Action
            </button>
          </div>
        </gmd-grid>

        <div style="margin-top: 24px; padding: 16px; background: #e9ecef; border-radius: 8px;">
          <h4 style="margin-top: 0; color: #495057;">💡 Note</h4>
          <p style="margin-bottom: 0; color: #495057; font-size: 14px;">
            This grid uses plain HTML elements (article, section, div) without gmd-cell components,
            showing that the grid container works with any content through content projection.
          </p>
        </div>
      </div>
    `,
  }),
};

export const WithMarkdownEditor: StoryObj<GridComponent> = {
  args: {
    columns: 3,
  },
  render: args => ({
    props: {
      ...args,
      markdownContent: `<gmd-grid columns="3">
    <gmd-cell>
        <h3>Column 1</h3>
        <p>First cell content</p>
    </gmd-cell>
    <gmd-cell>
        <h3>Column 2</h3>
        <p>Second cell content</p>
    </gmd-cell>
    <gmd-cell>
        <h3>Column 3</h3>
        <p>Third cell content</p>
    </gmd-cell>
</gmd-grid>`,
    },
    template: `
      <style>
        .editor-container {
          height: 500px;
        }
      </style>
      <div style="padding: 20px; display: flex; flex-direction: column; gap: 16px;">
        <h2>Grid Component with Markdown Editor</h2>
        <p>Edit the markdown content below to see the grid component rendered in real-time:</p>

        <div class="editor-container">
            <gmd-editor [(ngModel)]="markdownContent" />
        </div>

        <div style="margin-top: 16px; padding: 12px; background: #e3f2fd; border-radius: 4px; font-size: 14px;">
          <strong>Note:</strong> This demonstrates how the grid component can be used within markdown content.
          The editor shows the raw markdown, while the viewer renders the actual grid component.
        </div>
      </div>
    `,
  }),
};

export const WithCustomTheming: StoryObj<GridComponent> = {
  args: {
    columns: 3,
  },
  render: args => ({
    props: args,
    template: `
      <style>
        gmd-cell {
          display: block;
          padding: 12px;
          border: 1px solid #e0e0e0;
          border-radius: 6px;
          background-color: #f8f9fa;
          min-height: 60px;
        }

        .gap-examples {
          display: flex;
          flex-direction: column;
          gap: 24px;
        }

        .gap-example {
          border: 1px solid #dee2e6;
          border-radius: 8px;
          padding: 16px;
          background: white;
        }

        .gap-example h3 {
          margin-top: 0;
          margin-bottom: 12px;
          color: #495057;
        }

        .gap-example p {
          margin-bottom: 16px;
          color: #6c757d;
          font-size: 14px;
        }
      </style>
      <div style="padding: 20px;">
        <h2>Grid Component with Custom Gap Overrides</h2>
        <p>This story demonstrates how to customize the grid gap using CSS custom properties.</p>

        <div class="gap-examples">
          <div class="gap-example">
            <h3>Default Gap (16px)</h3>
            <gmd-grid [columns]="columns">
              <gmd-cell>
                <h4>Cell 1</h4>
                <p>Default spacing</p>
              </gmd-cell>
              <gmd-cell>
                <h4>Cell 2</h4>
                <p>Default spacing</p>
              </gmd-cell>
              <gmd-cell>
                <h4>Cell 3</h4>
                <p>Default spacing</p>
              </gmd-cell>
            </gmd-grid>
          </div>

          <div class="gap-example small-gap">
            <h3>Small Gap (8px)</h3>
            <gmd-grid [columns]="columns">
              <gmd-cell>
                <h4>Cell 1</h4>
                <p>Tighter spacing</p>
              </gmd-cell>
              <gmd-cell>
                <h4>Cell 2</h4>
                <p>Tighter spacing</p>
              </gmd-cell>
              <gmd-cell>
                <h4>Cell 3</h4>
                <p>Tighter spacing</p>
              </gmd-cell>
            </gmd-grid>
          </div>

          <div class="gap-example large-gap">
            <h3>Large Gap (32px)</h3>
            <gmd-grid [columns]="columns">
              <gmd-cell>
                <h4>Cell 1</h4>
                <p>More spacious</p>
              </gmd-cell>
              <gmd-cell>
                <h4>Cell 2</h4>
                <p>More spacious</p>
              </gmd-cell>
              <gmd-cell>
                <h4>Cell 3</h4>
                <p>More spacious</p>
              </gmd-cell>
            </gmd-grid>
          </div>

          <div class="gap-example no-gap">
            <h3>No Gap (0px)</h3>
            <gmd-grid [columns]="columns">
              <gmd-cell>
                <h4>Cell 1</h4>
                <p>No spacing</p>
              </gmd-cell>
              <gmd-cell>
                <h4>Cell 2</h4>
                <p>No spacing</p>
              </gmd-cell>
              <gmd-cell>
                <h4>Cell 3</h4>
                <p>No spacing</p>
              </gmd-cell>
            </gmd-grid>
          </div>
        </div>
      </div>
    `,
  }),
  parameters: {
    docs: {
      description: {
        story:
          'Demonstrates custom theming using the new @gmd.grid-overrides() mixin. Each example shows different gap spacing applied via SCSS classes.',
      },
    },
  },
};
