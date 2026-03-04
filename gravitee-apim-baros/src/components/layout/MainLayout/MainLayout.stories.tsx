import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Search } from 'lucide-react';
import { Separator } from '@baros/components/ui/separator';
import { SidebarTrigger } from '@baros/components/ui/sidebar';
import { mockNavItems, mockOrganizations, mockEnvironments, mockUser } from '../../../../.storybook/mock-data';
import { AppSidebar } from '../AppSidebar';
import { GraviteeLogo, GraviteeIcon } from '../GraviteeLogo';
import { OrgEnvSelector } from '../OrgEnvSelector';
import { ThemeToggle } from '../ThemeToggle';
import { TopNav } from '../TopNav';
import { TopNavUser } from '../TopNavUser';
import { MainLayout } from './MainLayout';

function findParentTitle(key: string): string {
  for (const item of mockNavItems) {
    if (item.key === key) return item.title;
    if (item.items?.some(sub => sub.key === key)) return item.title;
  }
  return 'Home';
}

function findSubTitle(key: string): string | undefined {
  for (const item of mockNavItems) {
    const sub = item.items?.find(s => s.key === key);
    if (sub) return sub.title;
  }
  return undefined;
}

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
  render: () => {
    const [activeKey, setActiveKey] = useState('api-list');
    const [activeOrgKey, setActiveOrgKey] = useState('gravitee');
    const [activeEnvKey, setActiveEnvKey] = useState('prod');

    const parentTitle = findParentTitle(activeKey);
    const subTitle = findSubTitle(activeKey);

    return (
      <MainLayout
        sidebar={
          <AppSidebar
            logo={<GraviteeLogo />}
            collapsedLogo={<GraviteeIcon />}
            navItems={mockNavItems}
            activeItemKey={activeKey}
            onNavItemClick={setActiveKey}
          />
        }
        topnav={
          <TopNav
            leading={
              <div className="flex items-center gap-1">
                <SidebarTrigger />
                <Separator orientation="vertical" className="mx-1 h-4" />
                <OrgEnvSelector
                  organizations={mockOrganizations}
                  environments={mockEnvironments}
                  activeOrgKey={activeOrgKey}
                  activeEnvKey={activeEnvKey}
                  onOrgChange={setActiveOrgKey}
                  onEnvChange={setActiveEnvKey}
                />
              </div>
            }
            trailing={
              <div className="flex items-center gap-1">
                <div className="relative">
                  <Search className="absolute left-2 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                  <input
                    type="search"
                    placeholder="Search..."
                    className="h-7 w-48 rounded-md border border-input bg-background pl-8 pr-2 text-xs placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    aria-label="Global search"
                  />
                </div>
                <ThemeToggle />
                <TopNavUser user={mockUser} />
              </div>
            }
          />
        }
      >
        <div className="space-y-6">
          <div>
            <nav aria-label="Breadcrumb" className="mb-1 flex items-center gap-1 text-sm text-muted-foreground">
              <span>{parentTitle}</span>
              {subTitle && (
                <>
                  <span className="mx-0.5">&gt;</span>
                  <span className="font-medium text-foreground">{subTitle}</span>
                </>
              )}
            </nav>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              {subTitle ?? parentTitle}
            </h1>
            <p className="text-sm text-muted-foreground">
              Manage your {(subTitle ?? parentTitle).toLowerCase()} here.
            </p>
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
    );
  },
};

export const CollapsedSidebar: Story = {
  render: () => (
    <MainLayout
      defaultOpen={false}
      sidebar={
        <AppSidebar
          logo={<GraviteeLogo />}
          collapsedLogo={<GraviteeIcon />}
          navItems={mockNavItems}
          activeItemKey="policies"
        />
      }
      topnav={
        <TopNav
          leading={
            <div className="flex items-center gap-1">
              <SidebarTrigger />
              <Separator orientation="vertical" className="mx-1 h-4" />
              <OrgEnvSelector
                organizations={mockOrganizations}
                environments={mockEnvironments}
                activeOrgKey="gravitee"
                activeEnvKey="prod"
              />
            </div>
          }
          trailing={
            <div className="flex items-center gap-1">
              <ThemeToggle />
              <TopNavUser user={mockUser} />
            </div>
          }
        />
      }
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
    <MainLayout topnav={<TopNav leading={<h1 className="text-xs font-semibold">Full Width</h1>} />}>
      <div className="rounded-lg border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-card-foreground">Full Width Layout</h2>
        <p className="mt-2 text-sm text-muted-foreground">No sidebar, content takes the full width.</p>
      </div>
    </MainLayout>
  ),
};
