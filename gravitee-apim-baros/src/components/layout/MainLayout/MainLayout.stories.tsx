import type { Meta, StoryObj } from '@storybook/react';
import { LayoutDashboard, Globe, Settings, HelpCircle, Bell, Search, User } from 'lucide-react';
import { SideNav } from '../SideNav';
import { TopNav } from '../TopNav';
import { Button } from '@baros/components/ui/button';
import { Separator } from '@baros/components/ui/separator';
import { MainLayout } from './MainLayout';

const navItems = [
  { icon: <LayoutDashboard className="h-5 w-5" />, label: 'Dashboard', active: true },
  { icon: <Globe className="h-5 w-5" />, label: 'APIs' },
  { icon: <Settings className="h-5 w-5" />, label: 'Settings' },
];

const meta = {
  title: 'Layout/MainLayout',
  component: MainLayout,
  tags: ['autodocs'],
  parameters: {
    layout: 'fullscreen',
  },
} satisfies Meta<typeof MainLayout>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Overview: Story = {
  render: () => (
    <MainLayout
      sidebar={
        <SideNav
          header={<span className="text-lg font-semibold text-primary">Gravitee</span>}
          items={navItems}
          footer={
            <button
              type="button"
              className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-sidebar-accent"
            >
              <HelpCircle className="h-5 w-5" />
              <span>Help & Support</span>
            </button>
          }
        />
      }
      topnav={
        <TopNav
          leading={
            <nav aria-label="Breadcrumb" className="flex items-center gap-1 text-sm text-muted-foreground">
              <span>Home</span>
              <span className="mx-1">/</span>
              <span className="font-medium text-foreground">Dashboard</span>
            </nav>
          }
          trailing={
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
          }
        />
      }
    >
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Dashboard</h1>
          <p className="text-sm text-muted-foreground">Welcome to the Gravitee API Management console.</p>
        </div>

        <Separator />

        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {['Total APIs', 'Active Subscriptions', 'Requests Today'].map(title => (
            <div key={title} className="rounded-lg border border-border bg-card p-6 shadow-sm">
              <h3 className="text-sm font-medium text-muted-foreground">{title}</h3>
              <p className="mt-2 text-3xl font-bold text-card-foreground">
                {title === 'Total APIs' ? '42' : title === 'Active Subscriptions' ? '128' : '1.2M'}
              </p>
            </div>
          ))}
        </div>

        <div className="rounded-lg border border-border bg-card p-6 shadow-sm">
          <h2 className="mb-4 text-lg font-semibold text-card-foreground">Recent Activity</h2>
          <div className="space-y-3">
            {[
              { action: 'API "Payment Service" deployed', time: '2 minutes ago' },
              { action: 'New subscription to "User API"', time: '15 minutes ago' },
              { action: 'Rate limit updated on "Orders API"', time: '1 hour ago' },
              { action: 'Health check alert resolved', time: '3 hours ago' },
            ].map(({ action, time }) => (
              <div key={action} className="flex items-center justify-between border-b border-border pb-3 last:border-0">
                <span className="text-sm text-foreground">{action}</span>
                <span className="text-xs text-muted-foreground">{time}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </MainLayout>
  ),
};

export const CollapsedSidebar: Story = {
  render: () => (
    <MainLayout
      sidebar={
        <SideNav
          collapsed
          header={<span className="text-lg font-bold text-primary">G</span>}
          items={navItems}
        />
      }
      topnav={<TopNav leading={<h1 className="text-sm font-semibold">Dashboard</h1>} />}
    >
      <div className="rounded-lg border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-card-foreground">Content Area</h2>
        <p className="mt-2 text-sm text-muted-foreground">
          The sidebar is collapsed, giving more room to the main content area.
        </p>
      </div>
    </MainLayout>
  ),
};

export const NoSidebar: Story = {
  render: () => (
    <MainLayout topnav={<TopNav leading={<h1 className="text-sm font-semibold">Full Width</h1>} />}>
      <div className="rounded-lg border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-card-foreground">Full Width Layout</h2>
        <p className="mt-2 text-sm text-muted-foreground">No sidebar, content takes the full width.</p>
      </div>
    </MainLayout>
  ),
};
