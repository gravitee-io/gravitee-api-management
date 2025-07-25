import type { Meta, StoryObj } from '@storybook/angular';
import { CopyCodeComponent } from './copy-code.component';

const meta: Meta<CopyCodeComponent> = {
  title: 'Gravitee Markdown/Components/Copy Code',
  component: CopyCodeComponent,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    text: {
      control: 'text',
      description: 'The text/code to be copied when the button is clicked',
    },
  },
};

export default meta;
type Story = StoryObj<CopyCodeComponent>;

export const Default: Story = {
  args: {
    text: 'npm install @angular/core',
  },
  render: (args) => ({
    props: {
      ...args,
      generateCode: () => {
        const attributes: string[] = [];
        
        if (args.text) {
          attributes.push(`text="${args.text}"`);
        }
        
        const attributesStr = attributes.length > 0 ? ' ' + attributes.join(' ') : '';
        return `<app-copy-code${attributesStr}></app-copy-code>`;
      }
    },
    template: `
      <div style="display: flex; flex-direction: column; gap: 20px; align-items: center;">
        <app-copy-code [text]="text"></app-copy-code>
        
        <div style="margin-top: 20px; padding: 16px; background: #f5f5f5; border-radius: 8px; font-family: monospace; font-size: 14px; max-width: 600px; width: 100%;">
          <div style="margin-bottom: 8px; font-weight: bold; color: #333;">Generated Code:</div>
          <pre style="margin: 0; white-space: pre-wrap; word-break: break-all;">{{ generateCode() }}</pre>
        </div>
      </div>
    `,
  }),
};

export const LongCode: Story = {
  args: {
    text: `npm install @angular/core @angular/common @angular/platform-browser @angular/forms @angular/router rxjs zone.js`,
  },
};

export const JSONCode: Story = {
  args: {
    text: JSON.stringify({ name: 'example', version: '1.0.0', dependencies: { '@angular/core': '^17.0.0' } }, null, 2),
  },
};

export const GitCommand: Story = {
  args: {
    text: 'git clone https://github.com/example/repo.git',
  },
};

export const DockerCommand: Story = {
  args: {
    text: 'docker run -p 8080:8080 myapp',
  },
};

export const YarnCommand: Story = {
  args: {
    text: 'yarn add @angular/core',
  },
};

export const PnpmCommand: Story = {
  args: {
    text: 'pnpm add @angular/core',
  },
};

export const Empty: Story = {
  args: {
    text: '',
  },
};

export const MultipleExamples: Story = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <app-copy-code text="npm install @angular/core"></app-copy-code>
        <app-copy-code text="yarn add @angular/core"></app-copy-code>
        <app-copy-code text="pnpm add @angular/core"></app-copy-code>
      </div>
    `,
  }),
}; 