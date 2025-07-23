import type { Meta, StoryObj } from '@storybook/angular';
import { CardActionsComponent } from './card-actions.component';
import { ButtonComponent } from '../button/button.component';

const meta: Meta<CardActionsComponent> = {
  title: 'Components/Card Actions',
  component: CardActionsComponent,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
};

export default meta;
type Story = StoryObj<CardActionsComponent>;

export const Default: Story = {
  render: () => ({
    template: `
      <div style="border: 1px solid #e0e0e0; border-radius: 8px; background: #ffffff;">
        <div style="padding: 20px;">
          <h3>Card Content</h3>
          <p>This is the card content area. The actions will appear below.</p>
        </div>
        <card-actions>
          <app-button text="Primary Action" href="/primary" variant="filled"></app-button>
          <app-button text="Secondary Action" href="/secondary" variant="outlined"></app-button>
        </card-actions>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const SingleAction: Story = {
  render: () => ({
    template: `
      <div style="border: 1px solid #e0e0e0; border-radius: 8px; background: #ffffff;">
        <div style="padding: 20px;">
          <h3>Simple Card</h3>
          <p>A card with just one action button.</p>
        </div>
        <card-actions>
          <app-button text="Learn More" href="/learn" variant="filled"></app-button>
        </card-actions>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const MultipleActions: Story = {
  render: () => ({
    template: `
      <div style="border: 1px solid #e0e0e0; border-radius: 8px; background: #ffffff;">
        <div style="padding: 20px;">
          <h3>Multiple Actions</h3>
          <p>A card with multiple action buttons of different variants.</p>
        </div>
        <card-actions>
          <app-button text="Primary" href="/primary" variant="filled"></app-button>
          <app-button text="Secondary" href="/secondary" variant="outlined"></app-button>
          <app-button text="Tertiary" href="/tertiary" variant="text"></app-button>
        </card-actions>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const ExternalLinks: Story = {
  render: () => ({
    template: `
      <div style="border: 1px solid #e0e0e0; border-radius: 8px; background: #ffffff;">
        <div style="padding: 20px;">
          <h3>External Resources</h3>
          <p>Links to external resources that open in new tabs.</p>
        </div>
        <card-actions>
          <app-button text="GitHub" href="https://github.com" type="external" variant="filled"></app-button>
          <app-button text="Documentation" href="https://docs.example.com" type="external" variant="outlined"></app-button>
          <app-button text="Support" href="https://support.example.com" type="external" variant="text"></app-button>
        </card-actions>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const CustomStyledButtons: Story = {
  render: () => ({
    template: `
      <div style="border: 1px solid #e0e0e0; border-radius: 8px; background: #ffffff;">
        <div style="padding: 20px;">
          <h3>Custom Styled Buttons</h3>
          <p>Buttons with custom colors and styling.</p>
        </div>
        <card-actions>
          <app-button text="Success" href="/success" backgroundColor="#28a745" textColor="#ffffff"></app-button>
          <app-button text="Warning" href="/warning" backgroundColor="#ffc107" textColor="#000000"></app-button>
          <app-button text="Danger" href="/danger" backgroundColor="#dc3545" textColor="#ffffff"></app-button>
        </card-actions>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const DifferentSizes: Story = {
  render: () => ({
    template: `
      <div style="border: 1px solid #e0e0e0; border-radius: 8px; background: #ffffff;">
        <div style="padding: 20px;">
          <h3>Different Button Sizes</h3>
          <p>Buttons with different border radius and styling.</p>
        </div>
        <card-actions>
          <app-button text="Small" href="/small" borderRadius="4px" backgroundColor="#6c757d"></app-button>
          <app-button text="Medium" href="/medium" borderRadius="8px" backgroundColor="#007bff"></app-button>
          <app-button text="Large" href="/large" borderRadius="12px" backgroundColor="#28a745"></app-button>
        </card-actions>
      </div>
    `,
    imports: [ButtonComponent],
  }),
};

export const Standalone: Story = {
  render: () => ({
    template: `
      <card-actions>
        <app-button text="Action 1" href="/action1" variant="filled"></app-button>
        <app-button text="Action 2" href="/action2" variant="outlined"></app-button>
        <app-button text="Action 3" href="/action3" variant="text"></app-button>
      </card-actions>
    `,
    imports: [ButtonComponent],
  }),
};

export const Empty: Story = {
  render: () => ({
    template: `
      <card-actions>
        <!-- No buttons - shows empty actions area -->
      </card-actions>
    `,
  }),
};

export const WithCustomContent: Story = {
  render: () => ({
    template: `
      <div style="border: 1px solid #e0e0e0; border-radius: 8px; background: #ffffff;">
        <div style="padding: 20px;">
          <h3>Mixed Content</h3>
          <p>A card with buttons and other content in the actions area.</p>
        </div>
        <card-actions>
          <app-button text="Primary" href="/primary" variant="filled"></app-button>
          <span style="color: #666; font-size: 12px;">Last updated: 2024</span>
          <app-button text="Secondary" href="/secondary" variant="outlined"></app-button>
        </card-actions>
      </div>
    `,
    imports: [ButtonComponent],
  }),
}; 