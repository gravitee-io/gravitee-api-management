import type { Meta, StoryObj } from '@storybook/angular';
import { moduleMetadata } from '@storybook/angular';
import { GridComponent } from './grid.component';
import { GridCellComponent } from './grid-cell.component';
import { CardComponent } from '../card/card.component';
import { CardActionsComponent } from '../card/card-actions.component';
import { ButtonComponent } from '../button/button.component';
import { ImageComponent } from '../image/image.component';

const meta: Meta<GridComponent> = {
  title: 'Gravitee Markdown/Components/Grid',
  component: GridComponent,
  decorators: [
    moduleMetadata({
      imports: [
        GridComponent,
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
    columns: {
      control: { type: 'select' },
      options: [2, 3, 4, 5, 6],
      description: 'Number of columns for large screens',
    },
    gap: {
      control: { type: 'select' },
      options: ['small', 'medium', 'large', 'xl'],
      description: 'Gap between grid cells',
    },
    align: {
      control: { type: 'select' },
      options: ['start', 'center', 'end', 'stretch'],
      description: 'Vertical alignment of grid cells',
    },
    backgroundColor: {
      control: 'color',
      description: 'Custom background color for the grid',
    },
    padding: {
      control: 'text',
      description: 'Custom padding for the grid (e.g., "16px", "24px")',
    },
    borderRadius: {
      control: 'text',
      description: 'Custom border radius for the grid (e.g., "8px", "12px")',
    },
  },
};

export default meta;
type Story = StoryObj<GridComponent>;

export const TestGrid: Story = {
  args: {
    columns: 2,
    gap: 'large',
    align: 'stretch',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-grid [columns]="columns" [gap]="gap" [align]="align">
        <app-grid-cell>
          <h3>Test Cell 1</h3>
          <p>This is a test cell to verify the grid is working.</p>
        </app-grid-cell>
        <app-grid-cell>
          <h3>Test Cell 2</h3>
          <p>This is another test cell to verify the grid is working.</p>
        </app-grid-cell>
      </app-grid>
    `,
  }),
};

export const Basic: Story = {
  args: {
    columns: 3,
    gap: 'medium',
    align: 'stretch',
  },
  render: (args) => ({
    props: {
      ...args,
      generateCode: () => {
        const attributes: string[] = [];
        
        if (args.columns && args.columns !== 3) {
          attributes.push(`columns="${args.columns}"`);
        }
        if (args.gap && args.gap !== 'medium') {
          attributes.push(`gap="${args.gap}"`);
        }
        if (args.align && args.align !== 'stretch') {
          attributes.push(`align="${args.align}"`);
        }
        if (args.backgroundColor && args.backgroundColor !== 'transparent') {
          attributes.push(`backgroundColor="${args.backgroundColor}"`);
        }
        if (args.padding && args.padding !== '0') {
          attributes.push(`padding="${args.padding}"`);
        }
        if (args.borderRadius && args.borderRadius !== '0') {
          attributes.push(`borderRadius="${args.borderRadius}"`);
        }
        
        const attributesStr = attributes.length > 0 ? ' ' + attributes.join(' ') : '';
        return `<app-grid${attributesStr}>
  <app-grid-cell>
    <h3>Cell 1</h3>
    <p>This is the first cell content. It can contain any markdown or custom components.</p>
  </app-grid-cell>
  <app-grid-cell>
    <h3>Cell 2</h3>
    <p>This is the second cell content. The grid is responsive and will adapt to different screen sizes.</p>
  </app-grid-cell>
  <app-grid-cell>
    <h3>Cell 3</h3>
    <p>This is the third cell content. On mobile, cells stack vertically. On tablet and desktop, they display in columns.</p>
  </app-grid-cell>
</app-grid>`;
      }
    },
    template: `
      <div style="display: flex; flex-direction: column; gap: 20px; align-items: center;">
        <app-grid 
          [columns]="columns" 
          [gap]="gap" 
          [align]="align"
          [backgroundColor]="backgroundColor"
          [padding]="padding"
          [borderRadius]="borderRadius"
        >
          <app-grid-cell>
            <h3>Cell 1</h3>
            <p>This is the first cell content. It can contain any markdown or custom components.</p>
          </app-grid-cell>
          <app-grid-cell>
            <h3>Cell 2</h3>
            <p>This is the second cell content. The grid is responsive and will adapt to different screen sizes.</p>
          </app-grid-cell>
          <app-grid-cell>
            <h3>Cell 3</h3>
            <p>This is the third cell content. On mobile, cells stack vertically. On tablet and desktop, they display in columns.</p>
          </app-grid-cell>
        </app-grid>
        
        <div style="margin-top: 20px; padding: 16px; background: #f5f5f5; border-radius: 8px; font-family: monospace; font-size: 14px; max-width: 600px; width: 100%;">
          <div style="margin-bottom: 8px; font-weight: bold; color: #333;">Generated Code:</div>
          <pre style="margin: 0; white-space: pre-wrap; word-break: break-all;">{{ generateCode() }}</pre>
        </div>
      </div>
    `,
  }),
};

export const WithMarkdown: Story = {
  args: {
    columns: 3,
    gap: 'medium',
    align: 'stretch',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-grid [columns]="columns" [gap]="gap" [align]="align">
        <app-grid-cell >
          # Cell 1

          This is the first cell content with markdown rendering.
        </app-grid-cell>
        <app-grid-cell markdown="true">
          ## Cell 2

          This is the second cell content with **markdown** support.
        </app-grid-cell>
        <app-grid-cell markdown="true">
          ### Cell 3

          This is the third cell content with \`code\` and *emphasis*.
        </app-grid-cell>
      </app-grid>
    `,
  }),
};

export const TwoColumns: Story = {
  args: {
    columns: 2,
    gap: 'large',
    align: 'start',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-grid [columns]="columns" [gap]="gap" [align]="align">
        <app-grid-cell>
          <h3>Left Column</h3>
          <p>This is the left column content. It can contain any markdown or custom components.</p>
        </app-grid-cell>
        <app-grid-cell>
          <h3>Right Column</h3>
          <p>This is the right column content. The grid automatically adjusts for different screen sizes.</p>
        </app-grid-cell>
      </app-grid>
    `,
  }),
};

export const FourColumns: Story = {
  args: {
    columns: 4,
    gap: 'small',
    align: 'center',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-grid [columns]="columns" [gap]="gap" [align]="align">
        <app-grid-cell>
          <h4>Col 1</h4>
          <p>First column content.</p>
        </app-grid-cell>
        <app-grid-cell>
          <h4>Col 2</h4>
          <p>Second column content.</p>
        </app-grid-cell>
        <app-grid-cell>
          <h4>Col 3</h4>
          <p>Third column content.</p>
        </app-grid-cell>
        <app-grid-cell>
          <h4>Col 4</h4>
          <p>Fourth column content.</p>
        </app-grid-cell>
      </app-grid>
    `,
  }),
};

export const WithCards: Story = {
  args: {
    columns: 3,
    gap: 'large',
    align: 'stretch',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-grid [columns]="columns" [gap]="gap" [align]="align">
        <app-grid-cell>
          <app-card title="Feature 1" centered="true">
            <p>This card contains important information about the first feature.</p>
            <card-actions>
              <app-button href="/feature1" text="Learn More">Learn More</app-button>
            </card-actions>
          </app-card>
        </app-grid-cell>
        <app-grid-cell>
          <app-card title="Feature 2" centered="true">
            <p>This card contains important information about the second feature.</p>
            <card-actions>
              <app-button href="/feature2" text="Learn More">Learn More</app-button>
            </card-actions>
          </app-card>
        </app-grid-cell>
        <app-grid-cell>
          <app-card title="Feature 3" centered="true">
            <p>This card contains important information about the third feature.</p>
            <card-actions>
              <app-button href="/feature3" text="Learn More">Learn More</app-button>
            </card-actions>
          </app-card>
        </app-grid-cell>
      </app-grid>
    `,
  }),
};

export const MixedContent: Story = {
  args: {
    columns: 3,
    gap: 'large',
    align: 'start',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-grid [columns]="columns" [gap]="gap" [align]="align">
        <app-grid-cell backgroundColor="light" padding="large" border="true">
          <h3>Text Content</h3>
          <p>This cell contains plain text content with a light background and border.</p>
          <p>It demonstrates how the grid can handle different types of content.</p>
        </app-grid-cell>
        <app-grid-cell>
          <app-card title="Card Content" centered="true">
            <p>This cell contains a card component, showing how custom components work within the grid.</p>
            <card-actions>
              <app-button href="/action" text="Action">Action</app-button>
            </card-actions>
          </app-card>
        </app-grid-cell>
        <app-grid-cell backgroundColor="primary" padding="large">
          <h3 style="color: white;">Highlighted Content</h3>
          <p style="color: white;">This cell has a primary background color to highlight important content.</p>
        </app-grid-cell>
      </app-grid>
    `,
  }),
};

export const ResponsiveSpans: Story = {
  args: {
    columns: 3,
    gap: 'large',
    align: 'stretch',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-grid [columns]="columns" [gap]="gap" [align]="align">
        <app-grid-cell span="2" backgroundColor="success" padding="large">
          <h3 style="color: white;">Wide Cell (Spans 2 Columns)</h3>
          <p style="color: white;">This cell spans 2 columns on desktop, but will stack on mobile and tablet.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="warning" padding="large">
          <h3>Side Cell</h3>
          <p>This cell takes up the remaining space next to the wide cell.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="info" padding="large">
          <h3 style="color: white;">Full Width Cell</h3>
          <p style="color: white;">This cell takes the full width below the previous cells.</p>
        </app-grid-cell>
      </app-grid>
    `,
  }),
};

export const StyledGrid: Story = {
  args: {
    columns: 3,
    gap: 'xl',
    align: 'center',
    backgroundColor: '#f8f9fa',
    padding: '24px',
    borderRadius: '12px',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-grid [columns]="columns" [gap]="gap" [align]="align" [backgroundColor]="backgroundColor" [padding]="padding" [borderRadius]="borderRadius">
        <app-grid-cell backgroundColor="white" shadow="elevated" padding="large">
          <h3>Styled Cell 1</h3>
          <p>This cell has a white background and elevated shadow.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="white" shadow="elevated" padding="large">
          <h3>Styled Cell 2</h3>
          <p>This cell also has a white background and elevated shadow.</p>
        </app-grid-cell>
        <app-grid-cell backgroundColor="white" shadow="elevated" padding="large">
          <h3>Styled Cell 3</h3>
          <p>This cell completes the styled grid layout.</p>
        </app-grid-cell>
      </app-grid>
    `,
  }),
}; 