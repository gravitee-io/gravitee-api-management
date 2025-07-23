import { moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { CardComponent } from './card.component';
import { CardActionsComponent } from './card-actions.component';
import { ButtonComponent } from '../button/button.component';

const meta: Meta<CardComponent> = {
  title: 'Components/Card',
  component: CardComponent,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    title: {
      control: 'text',
      description: 'The card title displayed in the header',
    },
    centered: {
      control: 'boolean',
      description: 'Whether to center the title and content',
    },
    borderRadius: {
      control: 'text',
      description: 'Custom border radius (e.g., "8px", "12px")',
    },
    backgroundColor: {
      control: 'color',
      description: 'Custom background color',
    },
    borderColor: {
      control: 'color',
      description: 'Custom border color',
    },
    borderWidth: {
      control: 'text',
      description: 'Custom border width (e.g., "1px", "2px")',
    },
    elevation: {
      control: { type: 'select' },
      options: [1, 2, 3, 4, 5],
      description: 'Shadow elevation level (1-5)',
    },
  },
  decorators: [
    moduleMetadata({
      imports: [CardComponent, CardActionsComponent, ButtonComponent],
    })
  ],
};

export default meta;
type Story = StoryObj<CardComponent>;

export const Default: Story = {
  args: {
    title: 'Card Title',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title" [borderRadius]="borderRadius" [backgroundColor]="backgroundColor" [borderColor]="borderColor" [borderWidth]="borderWidth" [elevation]="elevation">
        This is the card content. You can put any text, HTML, or other components here.
      </app-card>
    `,
  }),
};

export const WithTitle: Story = {
  args: {
    title: 'Welcome to Gravitee',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title">
        This card has a title and some content. The title appears in the header section of the card.
      </app-card>
    `,
  }),
};

export const Centered: Story = {
  args: {
    title: 'Centered Card',
    centered: true,
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title" [centered]="centered">
        This card has centered title and content. All text is aligned to the center for a more balanced appearance.
      </app-card>
    `,
  }),
};

export const Elevated: Story = {
  args: {
    title: 'Featured Content',
    elevation: 4,
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title" [elevation]="elevation">
        This card has increased elevation for more visual prominence. Perfect for highlighting important content.
      </app-card>
    `,
  }),
};

export const CustomStyled: Story = {
  args: {
    title: 'Custom Design',
    backgroundColor: '#f8f9fa',
    borderColor: '#007bff',
    borderRadius: '12px',
    borderWidth: '2px',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title" [backgroundColor]="backgroundColor" [borderColor]="borderColor" [borderRadius]="borderRadius" [borderWidth]="borderWidth">
        This card demonstrates custom styling with a light background, blue border, and rounded corners.
      </app-card>
    `,
  }),
};

export const WithActions: Story = {
  args: {
    title: 'Card with Actions',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title">
        This card has action buttons at the bottom.
        <card-actions>
          <app-button href="/primary" variant="filled">Primary Action</app-button>
          <app-button href="/secondary" variant="outlined">Secondary Action</app-button>
        </card-actions>
      </app-card>
    `,
    imports: [CardActionsComponent, ButtonComponent],
  }),
};

export const ExternalActions: Story = {
  args: {
    title: 'External Resources',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title">
        Links to external resources that will open in new tabs.
        <card-actions>
          <app-button href="https://github.com" type="external" variant="filled">GitHub</app-button>
          <app-button href="https://docs.example.com" type="external" variant="outlined">Documentation</app-button>
        </card-actions>
      </app-card>
    `,
    imports: [CardActionsComponent, ButtonComponent],
  }),
};

export const CompleteCard: Story = {
  args: {
    title: 'Premium Feature',
    elevation: 3,
    backgroundColor: '#ffffff',
    borderColor: '#28a745',
    borderRadius: '8px',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title" [elevation]="elevation" [backgroundColor]="backgroundColor" [borderColor]="borderColor" [borderRadius]="borderRadius">
        A fully customized card with premium styling, custom action buttons, and elevated design.
        <card-actions>
          <app-button text="Get Started" href="/premium/signup" variant="filled" backgroundColor="#28a745"></app-button>
          <app-button text="Learn More" href="/premium/features" variant="outlined"></app-button>
        </card-actions>
      </app-card>
    `,
    imports: [CardActionsComponent, ButtonComponent],
  }),
};

export const MultipleActions: Story = {
  args: {
    title: 'Multiple Action Buttons',
  },
  render: (args) => ({
    props: args,
    template: `
      <app-card [title]="title">
        You can add as many action buttons as you need within the card-actions component.
        <card-actions>
          <app-button href="/primary" variant="filled">Primary Action</app-button>
          <app-button href="/secondary" variant="outlined">Secondary Action</app-button>
          <app-button href="/tertiary" variant="text">Tertiary Action</app-button>
        </card-actions>
      </app-card>
    `,
    imports: [CardActionsComponent, ButtonComponent],
  }),
};

export const NoTitle: Story = {
  args: {},
  render: (args) => ({
    props: args,
    template: `
      <app-card>
        This card doesn't have a title. It's just content with action buttons.
        <card-actions>
          <app-button href="/action" variant="filled">Action</app-button>
        </card-actions>
      </app-card>
    `,
    imports: [CardActionsComponent, ButtonComponent],
  }),
};

export const AllElevations: Story = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <app-card title="Elevation 1" [elevation]="1">Card with elevation 1</app-card>
        <app-card title="Elevation 2" [elevation]="2">Card with elevation 2</app-card>
        <app-card title="Elevation 3" [elevation]="3">Card with elevation 3</app-card>
        <app-card title="Elevation 4" [elevation]="4">Card with elevation 4</app-card>
        <app-card title="Elevation 5" [elevation]="5">Card with elevation 5</app-card>
      </div>
    `,
  }),
};

export const ColorVariants: Story = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <app-card title="Default" backgroundColor="#ffffff" borderColor="#e0e0e0">Default card styling</app-card>
        <app-card title="Success" backgroundColor="#d4edda" borderColor="#28a745">Success themed card</app-card>
        <app-card title="Warning" backgroundColor="#fff3cd" borderColor="#ffc107">Warning themed card</app-card>
        <app-card title="Danger" backgroundColor="#f8d7da" borderColor="#dc3545">Danger themed card</app-card>
        <app-card title="Info" backgroundColor="#d1ecf1" borderColor="#17a2b8">Info themed card</app-card>
      </div>
    `,
  }),
};

export const CenteredVsLeftAligned: Story = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <div>
          <h4>Left Aligned (Default)</h4>
          <app-card title="Left Aligned Card">This card has left-aligned title and content. The text flows naturally from left to right.</app-card>
        </div>
        <div>
          <h4>Centered</h4>
          <app-card title="Centered Card" centered="true">This card has centered title and content. All text is aligned to the center for a more balanced appearance.</app-card>
        </div>
      </div>
    `,
  }),
}; 