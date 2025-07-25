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
    href: '/example',
    type: 'internal',
    variant: 'filled',
  },
  render: (args) => ({
    props: {
      ...args,
      generateCode: () => {
        const attributes: string[] = [];
        
        if (args.href) {
          attributes.push(`href="${args.href}"`);
        }
        if (args.type && args.type !== 'internal') {
          attributes.push(`type="${args.type}"`);
        }
        if (args.variant && args.variant !== 'filled') {
          attributes.push(`variant="${args.variant}"`);
        }
        if (args.borderRadius && args.borderRadius !== '4px') {
          attributes.push(`borderRadius="${args.borderRadius}"`);
        }
        if (args.backgroundColor) {
          attributes.push(`backgroundColor="${args.backgroundColor}"`);
        }
        if (args.textColor) {
          attributes.push(`textColor="${args.textColor}"`);
        }
        if (args.textTransform && args.textTransform !== 'none') {
          attributes.push(`textTransform="${args.textTransform}"`);
        }
        
        const attributesStr = attributes.length > 0 ? ' ' + attributes.join(' ') : '';
        return `<app-button${attributesStr}>Click Me</app-button>`;
      }
    },
    template: `
      <div style="display: flex; flex-direction: column; gap: 20px; align-items: center;">
        <app-button 
          [href]="href" 
          [type]="type" 
          [variant]="variant"
          [borderRadius]="borderRadius"
          [backgroundColor]="backgroundColor"
          [textColor]="textColor"
          [textTransform]="textTransform"
        >
          Click Me
        </app-button>
        
        <div style="margin-top: 20px; padding: 16px; background: #f5f5f5; border-radius: 8px; font-family: monospace; font-size: 14px; max-width: 600px; width: 100%;">
          <div style="margin-bottom: 8px; font-weight: bold; color: #333;">Generated Code:</div>
          <pre style="margin: 0; white-space: pre-wrap; word-break: break-all;">{{ generateCode() }}</pre>
        </div>
      </div>
    `,
  }),
};

export const Filled: Story = {
  args: {
    href: '/primary',
    type: 'internal',
    variant: 'filled',
  },
  render: (args) => ({
    props: args,
    template: '<app-button [href]="href" [type]="type" [variant]="variant">Primary Action</app-button>',
  }),
};

export const Outlined: Story = {
  args: {
    href: '/secondary',
    type: 'internal',
    variant: 'outlined',
  },
  render: (args) => ({
    props: args,
    template: '<app-button [href]="href" [type]="type" [variant]="variant">Secondary Action</app-button>',
  }),
};

export const Text: Story = {
  args: {
    href: '/text',
    type: 'internal',
    variant: 'text',
  },
  render: (args) => ({
    props: args,
    template: '<app-button [href]="href" [type]="type" [variant]="variant">Text Button</app-button>',
  }),
};

export const ExternalLink: Story = {
  args: {
    href: 'https://example.com',
    type: 'external',
    variant: 'filled',
  },
  render: (args) => ({
    props: args,
    template: '<app-button [href]="href" [type]="type" [variant]="variant">External Link</app-button>',
  }),
};

export const CustomStyling: Story = {
  args: {
    href: '/custom',
    type: 'internal',
    variant: 'filled',
    borderRadius: '20px',
    backgroundColor: '#ff6b6b',
    textColor: '#ffffff',
    textTransform: 'uppercase',
  },
  render: (args) => ({
    props: args,
    template: '<app-button [href]="href" [type]="type" [variant]="variant" [borderRadius]="borderRadius" [backgroundColor]="backgroundColor" [textColor]="textColor" [textTransform]="textTransform">Custom Styled</app-button>',
  }),
};

export const Rounded: Story = {
  args: {
    href: '/rounded',
    type: 'internal',
    variant: 'filled',
    borderRadius: '50%',
    backgroundColor: '#4ecdc4',
    textColor: '#ffffff',
  },
  render: (args) => ({
    props: args,
    template: '<app-button [href]="href" [type]="type" [variant]="variant" [borderRadius]="borderRadius" [backgroundColor]="backgroundColor" [textColor]="textColor">Rounded Button</app-button>',
  }),
};

export const AllVariants: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-button href="/filled" variant="filled">Filled</app-button>
        <app-button href="/outlined" variant="outlined">Outlined</app-button>
        <app-button href="/text" variant="text">Text</app-button>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const ExternalLinks: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-button href="https://github.com" type="external" variant="filled">GitHub</app-button>
        <app-button href="https://docs.example.com" type="external" variant="outlined">Documentation</app-button>
        <app-button href="https://support.example.com" type="external" variant="text">Support</app-button>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const CustomColors: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-button href="/success" backgroundColor="#28a745" textColor="#ffffff">Success</app-button>
        <app-button href="/warning" backgroundColor="#ffc107" textColor="#000000">Warning</app-button>
        <app-button href="/danger" backgroundColor="#dc3545" textColor="#ffffff">Danger</app-button>
        <app-button href="/info" backgroundColor="#17a2b8" textColor="#ffffff">Info</app-button>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const DifferentSizes: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-button href="/small" borderRadius="4px" backgroundColor="#6c757d">Small</app-button>
        <app-button href="/medium" borderRadius="8px" backgroundColor="#007bff">Medium</app-button>
        <app-button href="/large" borderRadius="12px" backgroundColor="#28a745">Large</app-button>
      </div>
    `,
    imports: [ButtonComponent],
  }),
}; 