import type { Meta, StoryObj } from '@storybook/angular';
import { ButtonComponent } from './button.component';

const meta: Meta<ButtonComponent> = {
  title: 'Components/Button',
  component: ButtonComponent,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    text: {
      control: 'text',
      description: 'The text displayed on the button',
    },
    href: {
      control: 'text',
      description: 'The URL the button links to',
    },
    type: {
      control: { type: 'select' },
      options: ['internal', 'external'],
      description: 'Whether the link opens in the same tab or new tab',
    },
    variant: {
      control: { type: 'select' },
      options: ['filled', 'outlined', 'text'],
      description: 'The visual variant of the button',
    },
    borderRadius: {
      control: 'text',
      description: 'Custom border radius (e.g., "8px", "50%")',
    },
    backgroundColor: {
      control: 'color',
      description: 'Custom background color',
    },
    textColor: {
      control: 'color',
      description: 'Custom text color',
    },
    textTransform: {
      control: { type: 'select' },
      options: ['none', 'uppercase', 'lowercase', 'capitalize'],
      description: 'Text transform style',
    },
  },
};

export default meta;
type Story = StoryObj<ButtonComponent>;

export const Default: Story = {
  args: {
    text: 'Click Me',
    href: '/example',
    type: 'internal',
    variant: 'filled',
  },
};

export const Filled: Story = {
  args: {
    text: 'Primary Action',
    href: '/primary',
    type: 'internal',
    variant: 'filled',
  },
};

export const Outlined: Story = {
  args: {
    text: 'Secondary Action',
    href: '/secondary',
    type: 'internal',
    variant: 'outlined',
  },
};

export const Text: Story = {
  args: {
    text: 'Text Button',
    href: '/text',
    type: 'internal',
    variant: 'text',
  },
};

export const ExternalLink: Story = {
  args: {
    text: 'External Link',
    href: 'https://example.com',
    type: 'external',
    variant: 'filled',
  },
};

export const CustomStyling: Story = {
  args: {
    text: 'Custom Styled',
    href: '/custom',
    type: 'internal',
    variant: 'filled',
    borderRadius: '20px',
    backgroundColor: '#ff6b6b',
    textColor: '#ffffff',
    textTransform: 'uppercase',
  },
};

export const Rounded: Story = {
  args: {
    text: 'Rounded Button',
    href: '/rounded',
    type: 'internal',
    variant: 'filled',
    borderRadius: '50%',
    backgroundColor: '#4ecdc4',
    textColor: '#ffffff',
  },
};

export const AllVariants: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-button text="Filled" href="/filled" variant="filled"></app-button>
        <app-button text="Outlined" href="/outlined" variant="outlined"></app-button>
        <app-button text="Text" href="/text" variant="text"></app-button>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const ExternalLinks: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-button text="GitHub" href="https://github.com" type="external" variant="filled"></app-button>
        <app-button text="Documentation" href="https://docs.example.com" type="external" variant="outlined"></app-button>
        <app-button text="Support" href="https://support.example.com" type="external" variant="text"></app-button>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const CustomColors: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-button text="Success" href="/success" backgroundColor="#28a745" textColor="#ffffff"></app-button>
        <app-button text="Warning" href="/warning" backgroundColor="#ffc107" textColor="#000000"></app-button>
        <app-button text="Danger" href="/danger" backgroundColor="#dc3545" textColor="#ffffff"></app-button>
        <app-button text="Info" href="/info" backgroundColor="#17a2b8" textColor="#ffffff"></app-button>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const DifferentSizes: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-button text="Small" href="/small" borderRadius="4px" backgroundColor="#6c757d"></app-button>
        <app-button text="Medium" href="/medium" borderRadius="8px" backgroundColor="#007bff"></app-button>
        <app-button text="Large" href="/large" borderRadius="12px" backgroundColor="#28a745"></app-button>
      </div>
    `,
    imports: [ButtonComponent],
  }),
}; 