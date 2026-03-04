import { Fragment, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Search } from 'lucide-react';
import { Separator } from '@baros/components/ui/separator';
import { SidebarTrigger } from '@baros/components/ui/sidebar';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@baros/components/ui/breadcrumb';
import { mockNavItems, mockApps, mockOrganizations, mockEnvironments, mockUser } from '../../../../.storybook/mock-data';
import { AppDropdown } from '../AppDropdown';
import { AppSidebar } from '../AppSidebar';
import { GraviteeLogo } from '../GraviteeLogo';
import { OrgSelector, EnvSelector } from '../OrgEnvSelector';
import { PageLayout } from '../PageLayout';
import { ThemeToggle } from '../ThemeToggle';
import { TopNav } from '../TopNav';
import { TopNavUser } from '../TopNavUser';
import { MainLayout } from './MainLayout';

interface BreadcrumbEntry {
  label: string;
  href?: string;
}

function SubHeader({ breadcrumbs }: { breadcrumbs: BreadcrumbEntry[] }) {
  return (
    <div className="flex items-center gap-2 border-b border-border px-4 py-2">
      <SidebarTrigger />
      {breadcrumbs.length > 0 && (
        <>
          <Separator orientation="vertical" className="mx-1 h-4" />
          <Breadcrumb>
            <BreadcrumbList>
              {breadcrumbs.map((crumb, index) => {
                const isFirst = index === 0;
                const isLast = index === breadcrumbs.length - 1;
                return (
                  <Fragment key={crumb.label}>
                    <BreadcrumbItem>
                      {isFirst || isLast ? (
                        <BreadcrumbPage>{crumb.label}</BreadcrumbPage>
                      ) : (
                        <BreadcrumbLink href={crumb.href}>{crumb.label}</BreadcrumbLink>
                      )}
                    </BreadcrumbItem>
                    {!isLast && <BreadcrumbSeparator />}
                  </Fragment>
                );
              })}
            </BreadcrumbList>
          </Breadcrumb>
        </>
      )}
    </div>
  );
}

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

function buildBreadcrumbs(key: string): BreadcrumbEntry[] {
  const parent = findParentTitle(key);
  const sub = findSubTitle(key);
  const crumbs: BreadcrumbEntry[] = [{ label: parent, href: '#' }];
  if (sub) crumbs.push({ label: sub });
  return crumbs;
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
    const [activeAppKey, setActiveAppKey] = useState('api-management');
    const [activeOrgKey, setActiveOrgKey] = useState('gravitee');
    const [activeEnvKey, setActiveEnvKey] = useState('prod');

    const pageTitle = findSubTitle(activeKey) ?? findParentTitle(activeKey);

    return (
      <MainLayout
        sidebar={
          <AppSidebar
            navItems={mockNavItems}
            activeItemKey={activeKey}
            onNavItemClick={setActiveKey}
            footer={
              <div className="flex flex-col gap-2">
                <OrgSelector
                  organizations={mockOrganizations}
                  activeOrgKey={activeOrgKey}
                  onOrgChange={setActiveOrgKey}
                />
                <EnvSelector
                  environments={mockEnvironments}
                  activeEnvKey={activeEnvKey}
                  onEnvChange={setActiveEnvKey}
                />
              </div>
            }
          />
        }
        topnav={
          <TopNav
            leading={
              <div className="flex items-center gap-2">
                <GraviteeLogo />
                <AppDropdown apps={mockApps} activeAppKey={activeAppKey} onAppChange={setActiveAppKey} />
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
        subheader={<SubHeader breadcrumbs={buildBreadcrumbs(activeKey)} />}
      >
        <PageLayout
          title={pageTitle}
          description={`Manage your ${pageTitle.toLowerCase()} here.`}
        >
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
        </PageLayout>
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
          navItems={mockNavItems}
          activeItemKey="policies"
          footer={
            <div className="flex flex-col gap-2">
              <OrgSelector organizations={mockOrganizations} activeOrgKey="gravitee" />
              <EnvSelector environments={mockEnvironments} activeEnvKey="prod" />
            </div>
          }
        />
      }
      topnav={
        <TopNav
          leading={
            <div className="flex items-center gap-2">
              <GraviteeLogo />
              <AppDropdown apps={mockApps} activeAppKey="api-management" />
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
      subheader={<SubHeader breadcrumbs={[{ label: 'Governance', href: '#' }, { label: 'Policies' }]} />}
    >
      <PageLayout
        title="Policies"
        description="The sidebar is collapsed, giving more room to the main content area."
      >
        <div className="rounded-lg border border-border bg-card p-6">
          <h2 className="text-lg font-semibold text-card-foreground">Content Area</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            This demonstrates the layout with a collapsed sidebar.
          </p>
        </div>
      </PageLayout>
    </MainLayout>
  ),
};

export const NoSidebar: Story = {
  render: () => (
    <MainLayout topnav={<TopNav leading={<h1 className="text-xs font-semibold">Full Width</h1>} />}>
      <PageLayout
        title="Full Width Layout"
        description="No sidebar, content takes the full width."
      >
        <div className="rounded-lg border border-border bg-card p-6">
          <h2 className="text-lg font-semibold text-card-foreground">Content Area</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            This layout has no sidebar — content fills the available space.
          </p>
        </div>
      </PageLayout>
    </MainLayout>
  ),
};
