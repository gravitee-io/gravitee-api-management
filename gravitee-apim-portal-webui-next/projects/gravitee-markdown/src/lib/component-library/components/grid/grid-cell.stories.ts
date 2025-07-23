import type { Meta, StoryObj } from '@storybook/angular';
import { moduleMetadata } from '@storybook/angular';
import { GridCellComponent } from './grid-cell.component';
import { CardComponent } from '../card/card.component';
import { CardActionsComponent } from '../card/card-actions.component';
import { ButtonComponent } from '../button/button.component';
import { ImageComponent } from '../image/image.component';

const meta: Meta<GridCellComponent> = {
  title: 'Components/Grid Cell',
  component: GridCellComponent,
  decorators: [
    moduleMetadata({
      imports: [
        GridCellComponent,
        CardComponent,
        CardActionsComponent,
        ButtonComponent,
        ImageComponent,
      ],
    }),
  ],
  parameters: {
    layout: 'padded',
  },
  argTypes: {
    markdown: {
      control: 'boolean',
      description: 'Whether to render the content as markdown',
    },
    span: {
      control: { type: 'select' },
      options: [1, 2, 3, 4],
      description: 'Number of columns this cell should span',
    },
    padding: {
      control: { type: 'select' },
      options: ['none', 'small', 'medium', 'large'],
      description: 'Padding size for the cell',
    },
    border: {
      control: 'boolean',
      description: 'Whether to show a border around the cell',
    },
    shadow: {
      control: { type: 'select' },
      options: ['none', 'small', 'elevated'],
      description: 'Shadow style for the cell',
    },
    backgroundColor: {
      control: { type: 'select' },
      options: ['none', 'light', 'white', 'primary', 'success', 'warning', 'danger', 'info'],
      description: 'Background color variant for the cell',
    },
    customBackgroundColor: {
      control: 'color',
      description: 'Custom background color (overrides backgroundColor)',
    },
    customBorderColor: {
      control: 'color',
      description: 'Custom border color',
    },
    customBorderRadius: {
      control: 'text',
      description: 'Custom border radius (e.g., "8px", "12px")',
    },
  },
};

export default meta;
type Story = StoryObj<GridCellComponent>;

export const Basic: Story = {
  args: {
    span: 1,
    padding: 'medium',
    border: false,
    shadow: 'none',
    backgroundColor: 'none',
  },
  render: (args) => ({
    props: args,
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 800px;">
        <app-grid-cell [span]="span" [padding]="padding" [border]="border" [shadow]="shadow" [backgroundColor]="backgroundColor">
          <h3>Basic Cell</h3>
          <p>This is a basic grid cell with default styling. It can contain any content or custom components.</p>
        </app-grid-cell>
      </div>
    `,
  }),
};

export const WithMarkdown: Story = {
  args: {
    markdown: true,
    span: 1,
    padding: 'medium',
    border: false,
    shadow: 'none',
    backgroundColor: 'none',
  },
  render: (args) => ({
    props: args,
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 800px;">
        <app-grid-cell [markdown]="markdown" [span]="span" [padding]="padding" [border]="border" [shadow]="shadow" [backgroundColor]="backgroundColor">
          # Basic Cell

          This is a basic grid cell with default styling. It can contain any markdown content or custom components.
        </app-grid-cell>
      </div>
    `,
  }),
};

export const StyledCell: Story = {
  args: {
    span: 1,
    padding: 'large',
    border: true,
    shadow: 'elevated',
    backgroundColor: 'light',
  },
  render: (args) => ({
    props: args,
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 800px;">
        <app-grid-cell [span]="span" [padding]="padding" [border]="border" [shadow]="shadow" [backgroundColor]="backgroundColor">
          <h3>Styled Cell</h3>
          <p>This cell has custom styling with padding, border, shadow, and background color.</p>
        </app-grid-cell>
      </div>
    `,
  }),
};

export const WideCell: Story = {
  args: {
    span: 2,
    padding: 'large',
    border: true,
    shadow: 'small',
    backgroundColor: 'primary',
  },
  render: (args) => ({
    props: args,
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 800px;">
        <app-grid-cell [span]="span" [padding]="padding" [border]="border" [shadow]="shadow" [backgroundColor]="backgroundColor">
          <h3 style="color: white;">Wide Cell</h3>
          <p style="color: white;">This cell spans 2 columns and has a primary background color. It demonstrates how cells can span multiple columns.</p>
        </app-grid-cell>
      </div>
    `,
  }),
};

export const FeaturedCell: Story = {
  args: {
    span: 3,
    padding: 'large',
    border: true,
    shadow: 'elevated',
    backgroundColor: 'success',
  },
  render: (args) => ({
    props: args,
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 800px;">
        <app-grid-cell [span]="span" [padding]="padding" [border]="border" [shadow]="shadow" [backgroundColor]="backgroundColor">
          <h3 style="color: white;">Featured Cell</h3>
          <p style="color: white;">This cell spans all 3 columns and has a success background color. It's perfect for highlighting important content.</p>
        </app-grid-cell>
      </div>
    `,
  }),
};

export const WithCard: Story = {
  args: {
    span: 1,
    padding: 'medium',
    border: false,
    shadow: 'none',
    backgroundColor: 'none',
  },
  render: (args) => ({
    props: args,
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 800px;">
        <app-grid-cell [span]="span" [padding]="padding" [border]="border" [shadow]="shadow" [backgroundColor]="backgroundColor">
          <app-card title="Card in Cell" centered="true">
            <p>This cell contains a card component, demonstrating how custom components work within grid cells.</p>
            <card-actions>
              <app-button href="/action" text="Action">Action</app-button>
            </card-actions>
          </app-card>
        </app-grid-cell>
      </div>
    `,
  }),
};

export const WithImage: Story = {
  args: {
    span: 1,
    padding: 'medium',
    border: true,
    shadow: 'small',
    backgroundColor: 'light',
  },
  render: (args) => ({
    props: args,
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 800px;">
        <app-grid-cell [span]="span" [padding]="padding" [border]="border" [shadow]="shadow" [backgroundColor]="backgroundColor">
          <h3>Image Cell</h3>
          <app-image src="https://via.placeholder.com/300x200" alt="Placeholder image" centered="true" rounded="true">
          </app-image>
          <p>This cell contains an image component along with text content.</p>
        </app-grid-cell>
      </div>
    `,
  }),
};

export const BackgroundVariants: Story = {
  render: () => ({
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 900px;">
        <app-grid-cell backgroundColor="light" padding="medium" border="true">
          <h3>Light Background</h3>
          <p>This cell has a light background color.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="primary" padding="medium" border="true">
          <h3 style="color: white;">Primary Background</h3>
          <p style="color: white;">This cell has a primary background color.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="success" padding="medium" border="true">
          <h3 style="color: white;">Success Background</h3>
          <p style="color: white;">This cell has a success background color.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="warning" padding="medium" border="true">
          <h3>Warning Background</h3>
          <p>This cell has a warning background color.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="danger" padding="medium" border="true">
          <h3 style="color: white;">Danger Background</h3>
          <p style="color: white;">This cell has a danger background color.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="info" padding="medium" border="true">
          <h3 style="color: white;">Info Background</h3>
          <p style="color: white;">This cell has an info background color.</p>
        </app-grid-cell>
      </div>
    `,
  }),
};

export const ShadowVariants: Story = {
  render: () => ({
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 900px;">
        <app-grid-cell backgroundColor="white" padding="medium" border="true" shadow="none">
          <h3>No Shadow</h3>
          <p>This cell has no shadow effect.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="white" padding="medium" border="true" shadow="small">
          <h3>Small Shadow</h3>
          <p>This cell has a small shadow effect.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="white" padding="medium" border="true" shadow="elevated">
          <h3>Elevated Shadow</h3>
          <p>This cell has an elevated shadow effect.</p>
        </app-grid-cell>
      </div>
    `,
  }),
};

export const CustomStyling: Story = {
  args: {
    span: 1,
    padding: 'large',
    border: true,
    shadow: 'elevated',
    backgroundColor: 'none',
    customBackgroundColor: '#e3f2fd',
    customBorderColor: '#2196f3',
    customBorderRadius: '16px',
  },
  render: (args) => ({
    props: args,
    template: `
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; max-width: 800px;">
        <app-grid-cell 
          [span]="span" 
          [padding]="padding" 
          [border]="border" 
          [shadow]="shadow" 
          [backgroundColor]="backgroundColor"
          [customBackgroundColor]="customBackgroundColor"
          [customBorderColor]="customBorderColor"
          [customBorderRadius]="customBorderRadius">
          <h3>Custom Styled Cell</h3>
          <p>This cell demonstrates custom background color, border color, and border radius styling.</p>
        </app-grid-cell>
      </div>
    `,
  }),
}; 