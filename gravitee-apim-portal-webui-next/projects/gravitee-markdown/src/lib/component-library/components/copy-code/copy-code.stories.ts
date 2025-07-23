import type { Meta, StoryObj } from '@storybook/angular';
import { CopyCodeComponent } from './copy-code.component';

const meta: Meta<CopyCodeComponent> = {
  title: 'Components/Copy Code',
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