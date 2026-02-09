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
  title: 'Gravitee Markdown/Gravitee Markdown Viewer',
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
      <div style="height: 650px; display: flex; flex-flow: column;">
        <h3>Markdown Editor with Sample Content</h3>
        <div style="background-color: #fff; overflow: scroll">
          <gmd-viewer [content]="content"></gmd-viewer>
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

### Card block

<div style="background-color: white; width: 600px;">
  <gmd-card>
    <gmd-card-title>Card Title</gmd-card-title>
    <gmd-md>
      ### Mb block

      > Lorem Ipsum is simply dummy text of the printing and typesetting industry.

      Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
      It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
      It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
      and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

      1. Lorem ipsum dolor sit amet
      2. Consectetur adipiscing elit
      3. Integer molestie lorem at massa
    </gmd-md>
  </gmd-card>
</div>

---

*Built and displayed with Marked for the best editing experience.*`,
    },
  }),
};

export const PortalHomePage: StoryObj<GraviteeMarkdownViewerComponent> = {
  render: () => ({
    template: `
        <gmd-viewer [content]="content"></gmd-viewer>
    `,
    props: {
      content: `
<gmd-grid>
    <gmd-md class="homepage-title">
        # Welcome to the Developer Portal
        Access all APIs, documentation, and tools to build your next integration.
    </gmd-md>
    <gmd-cell style="text-align: center; margin: auto;">
        <gmd-button link="/catalog">Explore all APIs</gmd-button>
        >Get started</gmd-button>
    </gmd-cell>
    <img class="homepage-cover-photo" src="assets/homepage/desk.png" title="Homepage picture"/>
</gmd-grid>

### Your toolkit for building

<gmd-grid columns="3">
    <gmd-md>
        ![book](assets/homepage/book.svg "Book icon")
        #### API catalog
        Browse and test all available APIs in one place.
    </gmd-md>
    <gmd-md>
        ![laptop](assets/homepage/laptop.svg "Laptop icon")
        #### Interactive docs
        Explore clear, structured documentation with code samples.
    </gmd-md>
    <gmd-md>
        ![vector](assets/homepage/vector.svg "Vector icon")
        #### Usage analytics
        Track API usage, error rates, and performance metrics.
    </gmd-md>
    <gmd-md>
        ![group](assets/homepage/group.svg "Group icon")
        #### API catalog
        Browse and test all available APIs in one place.
    </gmd-md>
    <gmd-md>
        ![support](assets/homepage/support.svg "Support icon")
        #### Interactive docs
        Explore clear, structured documentation with code samples.
    </gmd-md>
    <gmd-md>
        ![support](assets/homepage/service.svg "Service icon")
        #### Usage analytics
        Track API usage, error rates, and performance metrics.
    </gmd-md>
</gmd-grid>

### Get started in minutes

<gmd-grid columns="3">
    <gmd-card backgroundColor="none">
        <gmd-card-title>Your first API call</gmd-card-title>
        <gmd-md>Learn how to make a basic request and receive a response.Learn how to make a basic request and receive a response.</gmd-md>
        <div class="flex-container">
          <gmd-button link="/catalog" appearance="outlined" class="get-started-card__button"
          >Read <img src="assets/homepage/arrow-right.svg" alt="arrow right icon" title="Arrow right icon"/></gmd-button>
        </div>
    </gmd-card>
    <gmd-card backgroundColor="none">
        <gmd-card-title>Authentication walkthrough</gmd-card-title>
        <gmd-md>A step-by-step guide to generating and managing API keys.</gmd-md>
        <div class="flex-container">
          <gmd-button link="/catalog" appearance="outlined" class="get-started-card__button"
          >Read <img src="assets/homepage/arrow-right.svg" alt="arrow right icon" title="Arrow right icon"/></gmd-button>
        </div>
    </gmd-card>
    <gmd-card backgroundColor="none">
        <gmd-card-title>Integrating SDK into your project</gmd-card-title>
        <gmd-md>Use our official library to simplify your code.</gmd-md>
        <div class="flex-container">
          <gmd-button link="/catalog" appearance="outlined" class="get-started-card__button"
          >Read <img src="assets/homepage/arrow-right.svg" alt="arrow right icon" title="Arrow right icon"/></gmd-button>
        </div>
    </gmd-card>
</gmd-grid>
<style>
  .homepage-title {
    display: flex;
    flex-direction: column;
    max-width: 100%;
    text-align: center;
    margin: auto;
  }

  .homepage-cover-photo {
    display: flex;
    max-width: 100%;
    margin: 80px auto;
  }

  .get-started-card__button {
    --gmd-button-outlined-label-text-weight: 700;
    --gmd-button-outlined-label-text-color: black;
    margin-top: auto;
    padding-top: 12px;
  }

  .flex-container {
    display: flex;
    flex-direction: column;
    height: 100%
  }
</style>
      `,
    },
  }),
};
