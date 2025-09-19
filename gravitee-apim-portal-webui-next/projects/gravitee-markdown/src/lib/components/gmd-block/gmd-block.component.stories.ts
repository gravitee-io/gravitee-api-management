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

import { GmdBlockComponent } from './gmd-block.component';
import { GraviteeMarkdownEditorModule } from '../../gravitee-markdown-editor/gravitee-markdown-editor.module';
import { GraviteeMarkdownViewerModule } from '../../gravitee-markdown-viewer/gravitee-markdown-viewer.module';

export default {
  title: 'Gravitee Markdown/Components/GmdBlock',
  component: GmdBlockComponent,
  decorators: [
    moduleMetadata({
      imports: [GmdBlockComponent, GraviteeMarkdownEditorModule, GraviteeMarkdownViewerModule, FormsModule],
    }),
    applicationConfig({
      providers: [importProvidersFrom(GraviteeMarkdownEditorModule.forRoot({ theme: 'vs', baseUrl: '.' }))],
    }),
  ],
  parameters: {
    docs: {
      description: {
        component: 'A block component that automatically transforms its content into markdown using the same service as the markdown viewer.',
      },
    },
  },
} as Meta<GmdBlockComponent>;

export const MarkdownInside: StoryObj<GmdBlockComponent> = {
  render: () => ({
    template: `
      <div style="padding: 20px;">
        <h2>GmdBlock with Markdown Content</h2>
        <p>This story shows the GmdBlock component containing markdown content that gets automatically rendered by the markdown service:</p>

        <gmd-block>
          # Welcome to GmdBlock
          ## This is a GmdBlock component

          Some text inside the block.

          - Bullet points
          - *Italic text*
          - \`Inline code\`

          > This is a blockquote example

          \`\`\`javascript
          // Code block example
          function example() {{ '{' }}
            return "Hello, World!";
          {{ '}' }}
          \`\`\`
        </gmd-block>
      </div>
    `,
  }),
};

export const MarkdownWithHtmlInside: StoryObj<GmdBlockComponent> = {
  render: () => ({
    template: `
      <div style="padding: 20px;">
        <h2>GmdBlock with Markdown and HTML Content</h2>
        <p>This story shows the GmdBlock component containing both markdown and HTML content:</p>

        <gmd-block>
          # 🎨 Rich Content Block

          This block contains a mix of **markdown-style** text and *rich HTML* elements:

          ## Features
          - ✅ Content projection
          - ✅ HTML support  
          - ✅ Flexible styling

          ## Benefits
          - 🚀 Easy to use
          - 🎯 Simple API
          - 🔧 Customizable

          > This is a blockquote with **bold text** and *italic text*

          [Learn More](#) | [Documentation](#)
        </gmd-block>
      </div>
    `,
  }),
};

export const InGraviteeMarkdownEditor: StoryObj<GmdBlockComponent> = {
  render: () => ({
    props: {
      markdownContent: `<gmd-block>
# Welcome to GmdBlock

This is a **GmdBlock** component with markdown content inside a *gravitee-markdown-editor*.

## Features
- Simple content projection
- Works with any HTML content
- Perfect for custom blocks

> This component is designed to be a simple container that projects any content you put inside it.

### Code Example
\`\`\`html
<gmd-block>
  <h3>Your content here</h3>
  <p>Any HTML or markdown content</p>
</gmd-block>
\`\`\`
</gmd-block>`,
    },
    template: `
      <div style="padding: 20px; display: flex; flex-direction: column; gap: 16px;">
        <h2>GmdBlock in Gravitee Markdown Editor</h2>
        <p>This story shows the GmdBlock component being used within a gravitee-markdown-editor:</p>

        <div style="height: 500px; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
          <gmd-editor [(ngModel)]="markdownContent" />
        </div>

        <div style="margin-top: 16px; padding: 12px; background: #e3f2fd; border-radius: 4px; font-size: 14px;">
          <strong>Note:</strong> This demonstrates how the GmdBlock component can be used within markdown content.
          The editor shows the raw markdown, while the viewer renders the actual GmdBlock component.
        </div>
      </div>
    `,
  }),
};

export const InGridWithMarkdown: StoryObj<GmdBlockComponent> = {
  render: () => ({
    props: {
      gridMarkdownContent: `<gmd-grid columns="2">
  <gmd-cell>
    <gmd-block>
      # Getting Started
      
      Welcome to our platform! Here's what you need to know:
      
      - **Easy setup** in minutes
      - **Powerful features** for all users
      - **24/7 support** available
      
      > Start your journey today and see the difference!
    </gmd-block>
  </gmd-cell>
  
  <gmd-cell>
    <gmd-block>
      ## Features
      
      Our platform offers:
      
      ### Core Features
      - User management
      - Data analytics
      - Custom integrations
      
      ### Advanced Features
      - API access
      - Custom workflows
      - Real-time notifications
      
      *Everything you need in one place*
    </gmd-block>
  </gmd-cell>
</gmd-grid>

<div style="margin-top: 24px;">
  <gmd-grid columns="3">
    <gmd-cell>
      <gmd-block>
        ### Quick Tips
        
        - Use **bold** for emphasis
        - Add *italics* for style
        - Create lists easily
        
        > Pro tip: Markdown makes content creation simple!
      </gmd-block>
    </gmd-cell>
    
    <gmd-cell>
      <gmd-block>
        ### Resources
        
        - Documentation
        - Video tutorials
        - Community forum
        - Help center
        
        **Need help?** We're here for you!
      </gmd-block>
    </gmd-cell>
    
    <gmd-cell>
      <gmd-block>
        ### Support
        
        Contact us anytime:
        
        - Email: support.at.example.com
        - Phone: 1-800-HELP
        - Live chat available
        
        *We respond within 24 hours*
      </gmd-block>
    </gmd-cell>
  </gmd-grid>
</div>`,
    },
    template: `
      <div style="padding: 20px;">
        <h2>GmdBlock in Grid with Markdown Content</h2>
        <p>This story shows GmdBlock components used within a grid layout in markdown editor content:</p>
        
        <div style="height: 600px; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
          <gmd-editor [(ngModel)]="gridMarkdownContent" />
        </div>
      </div>
    `,
  }),
};

export const MarkdownContentAttribute: StoryObj<GmdBlockComponent> = {
  render: () => ({
    props: {
      markdownContentTest: `<gmd-grid columns="1">
  <gmd-cell markdownContent>
      # Hello from gmd-cell
      
      This is **markdown content** inside a cell with the \`markdownContent\` attribute.
      
      - List item 1
      - List item 2
      - List item 3
      
      > This should be rendered as markdown!
  </gmd-cell>
</gmd-grid>

<div markdownContent>
  ## Generic div with markdownContent
  
  This works with **any HTML tag** that has the \`markdownContent\` attribute!
  
  - Feature 1
  - Feature 2
  - Feature 3
  
  \`\`\`javascript
  console.log('Code blocks work too!');
  \`\`\`
</div>

<span markdownContent>
  ### Even span tags work!
  
  **Bold text** and *italic text* are supported.
</span>

<section markdownContent>
  #### Section tags work too!
  
  - Custom components
  - Any tag name
  - Flexible usage
  
  > The \`markdownContent\` attribute is truly generic!
</section>

<article markdownContent>
  ##### Article tags
  
  This demonstrates that **any HTML tag** can use the \`markdownContent\` attribute.
  
  \`\`\`typescript
  interface MarkdownContent {
    tagName: string;
    content: string;
  }
  \`\`\`
</article>

<custom-component markdownContent>
  ###### Even custom components!
  
  - Works with any tag name
  - Preserves all attributes
  - Renders markdown content
  
  **Note**: This works with any tag, including custom Angular components!
</custom-component>`,
    },
    template: `
      <div style="padding: 20px;">
        <h2>Flexible MarkdownContent Attribute Test</h2>
        <p>This story demonstrates that the markdownContent attribute works with <strong>any HTML tag</strong>:</p>
        
        <div style="height: 700px; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
          <gmd-editor [(ngModel)]="markdownContentTest" />
        </div>
      </div>
    `,
  }),
};

export const GmdBlockInGrid: StoryObj<GmdBlockComponent> = {
  render: () => ({
    props: {
      gmdBlockTest: `<gmd-grid columns="2">
  <gmd-block>
      # First Block
      
      This is a **gmd-block** component inside a grid cell.
      
      - Feature 1
      - Feature 2
      - Feature 3
      
      > This content is automatically processed as markdown!
  </gmd-block>
  
  <gmd-block>
      ## Second Block
      
      Another \`gmd-block\` with different content.
      
      \`\`\`typescript
      interface BlockContent {
        title: string;
        description: string;
      }
      \`\`\`
      
      **Note**: All markdown is rendered automatically!
  </gmd-block>
</gmd-grid>

<gmd-grid columns="1">
  <gmd-block>
      ### Single Column Block
      
      This \`gmd-block\` spans the full width of the grid.
      
      - List item 1
      - List item 2
      - List item 3
      
      > The content inside gmd-block is automatically processed as markdown!
  </gmd-block>
</gmd-grid>`,
    },
    template: `
      <div style="padding: 20px;">
        <h2>GmdBlock in Grid Test</h2>
        <p>This story demonstrates gmd-block components used within gmd-grid layouts:</p>
        
        <div style="height: 600px; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
          <gmd-editor [(ngModel)]="gmdBlockTest" />
        </div>
      </div>
    `,
  }),
};

export const GmdBlockExamples: StoryObj<GmdBlockComponent> = {
  render: () => ({
    props: {
      gmdBlockExamples: `<gmd-block>
  # Basic GmdBlock
  
  This is a simple \`gmd-block\` component with markdown content.
  
  - **Bold text**
  - *Italic text*
  - \`Code text\`
  
  > This is a blockquote
</gmd-block>

<gmd-block>
  ## Code Examples
  
  Here's some code:
  
  \`\`\`javascript
  function greet(name) {
    return \`Hello, \${name}!\`;
  }
  
  console.log(greet('World'));
  \`\`\`
  
  And some inline code: \`const x = 42;\`
</gmd-block>

<gmd-block>
  ### Lists and Tables
  
  **Unordered List:**
  - Item 1
  - Item 2
  - Item 3
  
  **Ordered List:**
  1. First item
  2. Second item
  3. Third item
  
  **Table:**
  | Column 1 | Column 2 | Column 3 |
  |----------|----------|----------|
  | Data 1   | Data 2   | Data 3   |
  | Data 4   | Data 5   | Data 6   |
</gmd-block>

<gmd-block>
  #### Links and Images
  
  Here's a [link to Gravitee](https://gravitee.io) and an image:
  
  ![Gravitee Logo](https://gravitee.io/assets/images/logo-gravitee.svg)
  
  **Note**: All markdown features work inside \`gmd-block\` components!
</gmd-block>`,
    },
    template: `
      <div style="padding: 20px;">
        <h2>GmdBlock Examples</h2>
        <p>This story shows various examples of gmd-block components with different markdown content:</p>
        
        <div style="height: 800px; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;">
          <gmd-editor [(ngModel)]="gmdBlockExamples" />
        </div>
      </div>
    `,
  }),
};
