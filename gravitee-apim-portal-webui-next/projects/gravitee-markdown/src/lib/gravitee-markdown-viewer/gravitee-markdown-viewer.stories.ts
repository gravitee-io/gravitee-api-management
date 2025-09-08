/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { GraviteeMarkdownViewerComponent, GraviteeMarkdownViewerModule } from './public-api';

export default {
  title: 'Components/Gravitee Markdown Viewer',
  component: GraviteeMarkdownViewerComponent,
  decorators: [
    moduleMetadata({
      imports: [GraviteeMarkdownViewerModule],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A markdown to html viewer component built using marked.js and highlight.js.',
      },
    },
  },
} as Meta<GraviteeMarkdownViewerComponent>;

export const WithoutContent: StoryObj<GraviteeMarkdownViewerComponent> = {
  render: () => ({
    template: `
      <div style="height: 650px">
        <h3>Markdown viewer without content</h3>
        <gmd-viewer></gmd-viewer>
      </div>
    `,
  }),
};

export const WithSampleContent: StoryObj<GraviteeMarkdownViewerComponent> = {
  render: () => ({
    template: `
      <div style="height: 650px">
        <h3>Markdown Editor with Sample Content</h3>
        <div style="height: 100%;background-color: #fff;">
          <div style="min-height: 100%;padding: 16px;border: 1px solid #b2aaa9;border-radius: 4px;">
            <gmd-viewer [content]="content"></gmd-viewer>
          </div>
        </div>
      </div>
    `,
    props: {
      content: `# Welcome to Gravitee API Management

This is a **markdown editor** component that allows you to write and edit markdown content.

## Features

- *Italic text* and **bold text**
- \`Inline code\`
- [Links](https://gravitee.io)
- [Anchor to image block](#image-block)
- Lists:
  <ul>
    <li>Item 1</li>
    <li>Item 2</li>
    <li>Item 3</li>
  </ul>

## Data Table

| Product | Price | Stock |
| :--- | :---: | ---: |
| Laptop | $1200 | 15 |
| Phone | $800 | 50 |
| Headset | $150 | 25 |

## Quote Block

> This is a blockquote Lorem Ipsum is simply dummy text of the printing and typesetting industry.
Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type
and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting,
 remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing
 Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.


## Html Block

<details>
  <summary style="font-size: 1.2em; cursor: pointer;">Click to Reveal the section!</summary>
  <p style="padding-left: 15px; font-style: italic; color: #555;">The details and summary and tags create a collapsible section</p>
</details>

<br/>

<div style="background-color: #f0f8ff; padding: 20px; border-radius: 8px; border: 1px solid #cceeff;">
  <h3 style="color: #004085;">A Styled Box</h3>
  <p>You can use a div with style attributes to create a custom-styled container. This is great for callouts or warnings.</p>
</div>

<h2 id="image-block">Image Block</h2>

![Gravitee Logo](https://raw.githubusercontent.com/gravitee-io/gravitee-api-management/refs/heads/master/gravitee-apim-portal-webui-next/src/assets/images/logo.png)

<img
    src="https://raw.githubusercontent.com/gravitee-io/gravitee-api-management/refs/heads/master/gravitee-apim-portal-webui-next/src/assets/images/logo.png"
    alt="Gravitee Logo"
/>

---

*Built and displayed with Marked for the best editing experience.*`,
    },
  }),
};
