import type { Meta, StoryObj } from '@storybook/angular';

import { GraviteeMarkdownViewerComponent } from './gravitee-markdown-viewer.component';

const meta: Meta<GraviteeMarkdownViewerComponent> = {
  title: 'Gravitee Markdown/Viewer',
  component: GraviteeMarkdownViewerComponent,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    content: {
      control: 'text',
      description: 'The markdown content to render',
    },
    darkTheme: {
      control: 'boolean',
      description: 'Enable dark theme',
    },
    highlightTheme: {
      control: 'select',
      options: ['github', 'monokai', 'vs', 'vs2015', 'atom-one-dark'],
      description: 'Syntax highlighting theme',
    },
  },
};

export default meta;
type Story = StoryObj<GraviteeMarkdownViewerComponent>;

export const Default: Story = {
  args: {
    content:
      '# Hello World\n\nThis is a **bold** text example with some *italic* content.\n\n## Features\n\n- **Syntax highlighting** for code blocks\n- **GitHub Flavored Markdown** support\n- **Dark/Light theme** support\n- **Responsive design**',
    darkTheme: false,
    highlightTheme: 'github',
  },
};

export const WithCodeBlock: Story = {
  args: {
    content: `# Code Example

Here's a TypeScript code block with syntax highlighting:

\`\`\`typescript
interface User {
  id: string;
  name: string;
  email: string;
}

function createUser(userData: Partial<User>): User {
  return {
    id: crypto.randomUUID(),
    name: userData.name || 'Anonymous',
    email: userData.email || 'anonymous@example.com'
  };
}

const user = createUser({ name: 'John Doe' });
console.log(user);
\`\`\`

This demonstrates the syntax highlighting capabilities.`,
    darkTheme: false,
    highlightTheme: 'github',
  },
};

export const WithTable: Story = {
  args: {
    content: `# Table Example

Here's a markdown table:

| Feature | Status | Description |
|---------|--------|-------------|
| Viewer | ✅ | Renders markdown content |
| Editor | ✅ | Edits markdown with preview |
| Themes | ✅ | Dark/Light theme support |
| Syntax Highlighting | ✅ | Code block highlighting |

> **Note**: This table demonstrates the table rendering capabilities.`,
    darkTheme: false,
    highlightTheme: 'github',
  },
};

export const DarkTheme: Story = {
  args: {
    content: `# Dark Theme Example

This is an example with the dark theme enabled.

## Features in Dark Mode

- **Better contrast** for code blocks
- **Easier on the eyes** in low-light environments
- **Consistent theming** across components

\`\`\`javascript
// Code looks great in dark mode
function greet(name) {
  return \`Hello, \${name}!\`;
}
\`\`\`

> The dark theme provides excellent readability for code and content.`,
    darkTheme: true,
    highlightTheme: 'monokai',
  },
};

export const EmptyContent: Story = {
  args: {
    content: '',
    darkTheme: false,
    highlightTheme: 'github',
  },
};
