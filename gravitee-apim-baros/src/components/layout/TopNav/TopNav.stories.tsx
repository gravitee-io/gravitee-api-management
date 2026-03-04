import type { Meta, StoryObj } from '@storybook/react';
import { Bell, Search, User } from 'lucide-react';
import { Button } from '@baros/components/ui/button';
import { TopNav } from './TopNav';

const meta = {
  title: 'Layout/TopNav',
  component: TopNav,
  tags: ['autodocs'],
} satisfies Meta<typeof TopNav>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    leading: <h1 className="text-sm font-semibold">Dashboard</h1>,
    trailing: (
      <div className="flex items-center gap-1">
        <Button variant="ghost" size="icon" aria-label="Search">
          <Search className="h-4 w-4" />
        </Button>
        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="h-4 w-4" />
        </Button>
        <Button variant="ghost" size="icon" aria-label="User profile">
          <User className="h-4 w-4" />
        </Button>
      </div>
    ),
  },
};

export const BreadcrumbStyle: Story = {
  args: {
    leading: (
      <nav aria-label="Breadcrumb" className="flex items-center gap-1 text-sm text-muted-foreground">
        <span>Home</span>
        <span>/</span>
        <span className="font-medium text-foreground">APIs</span>
      </nav>
    ),
  },
};
