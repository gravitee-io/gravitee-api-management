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

import { GraviteeMarkdownCardComponent } from './gravitee-markdown-card.component';
import { GraviteeMarkdownCardModule } from './gravitee-markdown-card.module';

export default {
  title: 'Gravitee Markdown/Gravitee Markdown Card',
  component: GraviteeMarkdownCardComponent,
  decorators: [
    moduleMetadata({
      imports: [GraviteeMarkdownCardModule],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A card to display markdown and html using our gmd-viewer',
      },
    },
  },
} as Meta<GraviteeMarkdownCardComponent>;

export const WithSampleContent: StoryObj<GraviteeMarkdownCardComponent> = {
  render: () => ({
    template: `
      <div style="background-color: white; width: 600px;">
        <gmd-card>
          <pre>{{ markdownContent }}</pre>
        </gmd-card>
      </div>
    `,
    props: {
      markdownContent: `
        <h4>md content</h4>

        > Lorem Ipsum is simply dummy text of the printing and typesetting industry.

        Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
        It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
        It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
        and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

        1. Lorem ipsum dolor sit amet
        2. Consectetur adipiscing elit
        3. Integer molestie lorem at massa
      `,
    },
  }),
};

export const WithTitle: StoryObj<GraviteeMarkdownCardComponent> = {
  render: () => ({
    template: `
      <div style="background-color: white; width: 600px;">
        <gmd-card>
          <gmd-card-title>Card Title</gmd-card-title>
          <pre>{{ markdownContent }}</pre>
        </gmd-card>
      </div>
    `,
    props: {
      markdownContent: `
        <h4>md content</h4>

        > Lorem Ipsum is simply dummy text of the printing and typesetting industry.

        Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
        It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
        It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
        and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

        1. Lorem ipsum dolor sit amet
        2. Consectetur adipiscing elit
        3. Integer molestie lorem at massa
      `,
    },
  }),
};

export const WithMdOverrides: StoryObj<GraviteeMarkdownCardComponent> = {
  render: () => ({
    template: `
      <div style="background-color: white; width: 600px;">
        <gmd-card>
          <gmd-card-title>Card Title</gmd-card-title>
          <pre>{{ markdownContent }}</pre>
        </gmd-card>
      </div>
    `,
    props: {
      markdownContent: `
        # h1
        ## h2
        ### h3
        #### h4

        > Lorem Ipsum is simply dummy text of the printing and typesetting industry.

        Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
        It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
        It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
        and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

        1. Lorem ipsum dolor sit amet
        2. Consectetur adipiscing elit
        3. Integer molestie lorem at massa
      `,
    },
  }),
};

export const WithTitleAndSubtitle: StoryObj<GraviteeMarkdownCardComponent> = {
  render: () => ({
    template: `
      <div style="background-color: white; width: 600px;">
        <gmd-card>
          <gmd-card-title>Card Title</gmd-card-title>
          <gmd-card-subtitle>Card Subtitle</gmd-card-subtitle>
          <pre>{{ markdownContent }}</pre>
        </gmd-card>
      </div>
    `,
    props: {
      markdownContent: `
        <h4>md content</h4>

        > Lorem Ipsum is simply dummy text of the printing and typesetting industry.

        Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
        It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
        It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
        and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

        1. Lorem ipsum dolor sit amet
        2. Consectetur adipiscing elit
        3. Integer molestie lorem at massa
      `,
    },
  }),
};

export const FigmaSimpleExample: StoryObj<GraviteeMarkdownCardComponent> = {
  render: () => ({
    template: `
      <div style="background-color: white; width: 300px;">
        <gmd-card>
          <gmd-card-title>A simple placeholder</gmd-card-title>
          <pre>{{ markdownContent }}</pre>
        </gmd-card>
      </div>
    `,
    props: {
      markdownContent: `Here's a paragraph. Are you happy now? Just fill in your own content here.`,
    },
  }),
};

export const FigmaSimpleExampleWithSubtitle: StoryObj<GraviteeMarkdownCardComponent> = {
  render: () => ({
    template: `
      <div style="background-color: white; width: 300px;">
        <gmd-card>
          <gmd-card-title>A simple placeholder</gmd-card-title>
          <gmd-card-subtitle>Version: 2.0</gmd-card-subtitle>
          <pre>{{ markdownContent }}</pre>
        </gmd-card>
      </div>
    `,
    props: {
      markdownContent: `Here's a paragraph. Are you happy now? Just fill in your own content here.`,
    },
  }),
};
