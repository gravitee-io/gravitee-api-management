import type { Meta, StoryObj } from '@storybook/angular';
import { GraviteeMarkdownEditorComponent } from './gravitee-markdown-editor.component';
import { FormsModule } from '@angular/forms';

const meta: Meta<GraviteeMarkdownEditorComponent> = {
  title: 'Gravitee Markdown/Editor',
  component: GraviteeMarkdownEditorComponent,
  parameters: {
    layout: 'padded',
  },
  tags: ['autodocs'],
  argTypes: {
    placeholder: {
      control: 'text',
      description: 'Placeholder text for the editor'
    },
    contentChange: {
      action: 'contentChanged',
      description: 'Event emitted when content changes'
    }
  },
  decorators: [
    (Story) => ({
      ...Story(),
      imports: [FormsModule]
    })
  ]
};

export default meta;
type Story = StoryObj<GraviteeMarkdownEditorComponent>;

export const Default: Story = {
  args: {
    placeholder: 'Enter your markdown content here...'
  }
};

export const WithInitialContent: Story = {
  args: {
    placeholder: 'Start writing your markdown...',
    content: `# Welcome to Gravitee Markdown Editor

This is a **powerful** markdown editor with live preview.

## Features

- **Split view** - Edit on the left, preview on the right
- **Real-time preview** as you type
- **Syntax highlighting** for code blocks
- **GitHub Flavored Markdown** support

\`\`\`javascript
// Example code block
function hello() {
  console.log("Hello, World!");
}
\`\`\`

> This is a blockquote example.

| Feature | Status |
|---------|--------|
| Split View | ✅ |
| Live Preview | ✅ |
| Syntax Highlighting | ✅ |`
  }
};

export const CustomPlaceholder: Story = {
  args: {
    placeholder: 'Start writing your documentation here...',
    content: ''
  }
};

export const EmptyEditor: Story = {
  args: {
    placeholder: 'Begin typing your markdown content...',
    content: ''
  }
};

export const WithCodeExample: Story = {
  args: {
    placeholder: 'Write your code documentation...',
    content: `# API Documentation

## Authentication

To authenticate with the API, you need to include your API key in the request headers.

\`\`\`typescript
interface ApiRequest {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  url: string;
  headers: {
    'Authorization': string;
    'Content-Type': string;
  };
  body?: any;
}

// Example usage
const request: ApiRequest = {
  method: 'GET',
  url: '/api/v1/users',
  headers: {
    'Authorization': 'Bearer your-api-key',
    'Content-Type': 'application/json'
  }
};
\`\`\`

## Response Format

All API responses follow this structure:

\`\`\`json
{
  "success": true,
  "data": {},
  "message": "Operation completed successfully"
}
\`\`\`

> **Note:** Always handle errors appropriately in your application.`
  }
}; 