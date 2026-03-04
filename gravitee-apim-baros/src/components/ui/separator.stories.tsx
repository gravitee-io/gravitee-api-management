import type { Meta, StoryObj } from '@storybook/react';
import { Separator } from './separator';

const meta = {
  title: 'Primitives/Separator',
  component: Separator,
  tags: ['autodocs'],
} satisfies Meta<typeof Separator>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Horizontal: Story = {
  render: () => (
    <div className="space-y-4">
      <div className="text-sm text-foreground">Above</div>
      <Separator />
      <div className="text-sm text-foreground">Below</div>
    </div>
  ),
};

export const Vertical: Story = {
  render: () => (
    <div className="flex h-8 items-center gap-4">
      <span className="text-sm text-foreground">Left</span>
      <Separator orientation="vertical" />
      <span className="text-sm text-foreground">Right</span>
    </div>
  ),
};
