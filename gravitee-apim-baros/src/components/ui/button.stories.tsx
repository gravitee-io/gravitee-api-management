import type { Meta, StoryObj } from '@storybook/react';
import { ArrowRight, Mail, Plus, Search, Trash2 } from 'lucide-react';
import { Button } from './button';

const meta = {
  title: 'Primitives/Button',
  component: Button,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'destructive', 'outline', 'secondary', 'ghost', 'link'],
    },
    size: {
      control: 'select',
      options: ['default', 'sm', 'lg', 'icon'],
    },
  },
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: { children: 'Button' },
};

export const Destructive: Story = {
  args: { children: 'Delete', variant: 'destructive' },
};

export const Outline: Story = {
  args: { children: 'Outline', variant: 'outline' },
};

export const Secondary: Story = {
  args: { children: 'Secondary', variant: 'secondary' },
};

export const Ghost: Story = {
  args: { children: 'Ghost', variant: 'ghost' },
};

export const Link: Story = {
  args: { children: 'Link', variant: 'link' },
};

export const Small: Story = {
  args: { children: 'Small', size: 'sm' },
};

export const Large: Story = {
  args: { children: 'Large', size: 'lg' },
};

export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-wrap items-center gap-4">
      <Button variant="default">Default</Button>
      <Button variant="secondary">Secondary</Button>
      <Button variant="destructive">Destructive</Button>
      <Button variant="outline">Outline</Button>
      <Button variant="ghost">Ghost</Button>
      <Button variant="link">Link</Button>
    </div>
  ),
};

export const IconLeft: Story = {
  args: { children: 'Add item', iconLeft: <Plus /> },
};

export const IconRight: Story = {
  args: { children: 'Next', iconRight: <ArrowRight /> },
};

export const IconOnly: Story = {
  args: { size: 'icon', 'aria-label': 'Search', iconLeft: <Search /> },
};

export const WithIcons: Story = {
  render: () => (
    <div className="flex flex-wrap items-center gap-4">
      <Button iconLeft={<Plus />}>Add item</Button>
      <Button variant="secondary" iconLeft={<Mail />}>
        Send email
      </Button>
      <Button variant="destructive" iconLeft={<Trash2 />}>
        Delete
      </Button>
      <Button variant="outline" iconRight={<ArrowRight />}>
        Next
      </Button>
      <Button variant="ghost" iconLeft={<Search />}>
        Search
      </Button>
      <Button size="icon" variant="outline" aria-label="Search">
        <Search />
      </Button>
    </div>
  ),
};
