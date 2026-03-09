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

import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';
import { GmdMdComponent } from '../block/gmd-md.component';
import { GmdCardSubtitleComponent } from './components/card-subtitle/gmd-card-subtitle.component';
import { GmdCardTitleComponent } from './components/card-title/gmd-card-title.component';
import { GmdCardComponent } from './gmd-card.component';
import { GmdCardModule } from './gmd-card.module';

export default {
  title: 'Gravitee Markdown/Components/Card',
  component: GmdCardComponent,
  decorators: [
    moduleMetadata({
      imports: [GmdCardModule, GmdCardTitleComponent, GmdCardSubtitleComponent, GmdMdComponent, GraviteeMarkdownViewerModule],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A card to display markdown and html using our gmd-viewer',
      },
    },
  },
  render: args => ({
    template: `
      <div style="width: {{ width }}; padding: {{ padding || 0 }}">
        <gmd-viewer [content]="contentToRender"></gmd-viewer>
      </div>
    `,
    props: args,
  }),
} as Meta<GmdCardComponent>;

export const WithEmptyContent: StoryObj = {
  args: {
    contentToRender: ``,
    width: '600px',
  },
};

export const WithSampleContent: StoryObj = {
  args: {
    width: '600px',
    contentToRender: `# Hello world!
<gmd-card>
  <gmd-md>
    Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
    It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
    It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
    and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

    1. Lorem ipsum dolor sit amet
    2. Consectetur adipiscing elit
    3. Integer molestie lorem at massa
  </gmd-md>
</gmd-card>
`,
  },
};

export const WithTitle: StoryObj = {
  args: {
    width: '600px',
    contentToRender: `<gmd-card>
  <gmd-card-title>Card Title</gmd-card-title>
  <gmd-md>
    #### Simple text block

    Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
    It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
    It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
    and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.

    #### Quote block

    > Lorem Ipsum is simply dummy text of the printing and typesetting industry.

    #### List of items

    1. Lorem ipsum dolor sit amet
    2. Consectetur adipiscing elit
    3. Integer molestie lorem at massa

    - Lorem ipsum dolor sit amet
    - Consectetur adipiscing elit
    - Integer molestie lorem at massa

    1. First item
        - Nested item 1
        - Nested item 2
    2. Second item
        - Another nested item

    #### Data table

    | Product | Price | Stock |
    | :--- | :---: | ---: |
    | Laptop | $1200 | 15 |
    | Phone | $800 | 50 |
    | Headset | $150 | 25 |
  </gmd-md>
</gmd-card>`,
  },
};

export const WithTitleAndSubtitle: StoryObj = {
  args: {
    width: '600px',
    contentToRender: `
<gmd-card>
  <gmd-card-title>Card Title</gmd-card-title>
  <gmd-card-subtitle>Card Subtitle</gmd-card-subtitle>
  <gmd-md>
    Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
    It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
    It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
    and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.
  </gmd-md>
</gmd-card>
      `,
  },
};

export const MarginOverrides: StoryObj = {
  args: {
    width: '600px',
    contentToRender: `
<h3>This title should have a bigger margin than in the card</h3>
<gmd-card>
  <gmd-card-title>Card Title</gmd-card-title>
  <gmd-md>
    # h1
    ## h2
    ### h3
    #### h4

    Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book.
    It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged.
    It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages,
    and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.
  </gmd-md>
</gmd-card>
      `,
  },
};

export const FigmaSimpleExample: StoryObj = {
  args: {
    width: '300px',
    contentToRender: `
<gmd-card>
  <gmd-card-title>A simple placeholder</gmd-card-title>
  <gmd-md>Here's a paragraph. Are you happy now? Just fill in your own content here.</gmd-md>
</gmd-card>
      `,
  },
};

export const FigmaSimpleExampleWithSubtitle: StoryObj = {
  args: {
    width: '300px',
    contentToRender: `
<gmd-card>
  <gmd-card-title>A simple placeholder</gmd-card-title>
  <gmd-card-subtitle>Version: 2.0</gmd-card-subtitle>
  <gmd-md>Here's a paragraph. Are you happy now? Just fill in your own content here.</gmd-md>
</gmd-card>
      `,
  },
};

export const WithGlobalCssOverrides: StoryObj = {
  args: {
    width: '300px',
    contentToRender: `
<div style="display: flex; width: 1200px; gap: 12px;">
<gmd-card class="with-global-css-overrides-1">
  <gmd-card-title>A simple placeholder with one global override class</gmd-card-title>
  <gmd-card-subtitle>Version: 2.0</gmd-card-subtitle>
  <gmd-md>Here's a paragraph. Are you happy now? Just fill in your own content here.</gmd-md>
</gmd-card>
<gmd-card class="with-global-css-overrides-2" >
  <gmd-card-title>A simple placeholder with another global override class</gmd-card-title>
  <gmd-card-subtitle>Version: 2.0</gmd-card-subtitle>
  <gmd-md>Here's a paragraph. Are you happy now? Just fill in your own content here.</gmd-md>
</gmd-card>
<gmd-card>
  <gmd-card-title>A simple placeholder with no override classes</gmd-card-title>
  <gmd-card-subtitle>Version: 2.0</gmd-card-subtitle>
  <gmd-md>Here's a paragraph. Are you happy now? Just fill in your own content here.</gmd-md>
</gmd-card>
<gmd-card style="--gmd-card-outline-color: yellow;">
  <gmd-card-title>A simple placeholder with inline styling</gmd-card-title>
  <gmd-card-subtitle>Version: 2.0</gmd-card-subtitle>
  <gmd-md>Here's a paragraph. Are you happy now? Just fill in your own content here.</gmd-md>
</gmd-card>
</div>
      `,
  },
};

export const MultipleCardUsingInputs: StoryObj = {
  args: {
    width: '100%',
    padding: '20px',
    contentToRender: `<gmd-grid columns="4">
    <gmd-cell>

        <gmd-card backgroundColor="#ffffff" textColor="orange">
            <gmd-card-title>First card</gmd-card-title>
            <gmd-card-subtitle>Version: 1.0</gmd-card-subtitle>
            <gmd-md>
              #### Here's the first card.

              Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,
              when an unknown printer took a galley of type and scrambled it to make a type specimen book.'
            </gmd-md>
        </gmd-card>
    </gmd-cell>

    <!-- empty line to ensure that it does not break the layout-->
    <gmd-cell>
        <gmd-card backgroundColor="#222222" textColor="#f5f5f5">
            <gmd-card-title>Second card</gmd-card-title>
            <gmd-card-subtitle>Version: 2.0</gmd-card-subtitle>
             <gmd-md>
              #### Here's the second card.


              <!-- empty line to ensure that it does not break the layout-->
              Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,
              when an unknown printer took a galley of type and scrambled it to make a type specimen book.'
            </gmd-md>
        </gmd-card>
    </gmd-cell>
    <gmd-cell>
        <gmd-card backgroundColor="rgb(0, 102, 204)" textColor="white">
            <gmd-card-title>Third card</gmd-card-title>
            <gmd-card-subtitle>Version: 3.0</gmd-card-subtitle>
            <gmd-md>
              #### Here's the third card.
              Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,
              when an unknown printer took a galley of type and scrambled it to make a type specimen book.'
            </gmd-md>
        </gmd-card>
    </gmd-cell>
    <gmd-cell>
        <gmd-card>
            <gmd-card-title>Third card</gmd-card-title>
            <gmd-card-subtitle>Version: 3.0</gmd-card-subtitle>
            <gmd-md>
              #### Here's the third card.
              Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,
              when an unknown printer took a galley of type and scrambled it to make a type specimen book.'
            </gmd-md>
        </gmd-card>
    </gmd-cell>
</gmd-grid>`,
  },
};
