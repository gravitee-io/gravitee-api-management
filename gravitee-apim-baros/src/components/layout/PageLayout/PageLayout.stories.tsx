import { Fragment, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Rocket, Plus, Search } from 'lucide-react';
import { Button } from '@baros/components/ui/button';
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
import { MainLayout } from '../MainLayout';
import { OrgSelector, EnvSelector } from '../OrgEnvSelector';
import { ThemeToggle } from '../ThemeToggle';
import { TopNav } from '../TopNav';
import { TopNavUser } from '../TopNavUser';
import { PageLayout } from './PageLayout';

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

function AppShell({
  activeNavKey,
  breadcrumbs,
  children,
}: {
  activeNavKey: string;
  breadcrumbs: BreadcrumbEntry[];
  children: React.ReactNode;
}) {
  const [activeAppKey, setActiveAppKey] = useState('api-management');
  const [activeOrgKey, setActiveOrgKey] = useState('gravitee');
  const [activeEnvKey, setActiveEnvKey] = useState('prod');

  return (
    <MainLayout
      sidebar={
        <AppSidebar
          navItems={mockNavItems}
          activeItemKey={activeNavKey}
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
      subheader={<SubHeader breadcrumbs={breadcrumbs} />}
    >
      {children}
    </MainLayout>
  );
}

const meta = {
  title: 'Layout/PageLayout',
  component: PageLayout,
  tags: ['autodocs'],
  parameters: {
    layout: 'fullscreen',
  },
} satisfies Meta<typeof PageLayout>;

export default meta;
type Story = StoryObj<typeof meta>;

const apiDetailsTabs = [
  { key: 'activity', label: 'Activity', href: '#activity' },
  { key: 'configuration', label: 'Configuration', href: '#configuration' },
  { key: 'consumers', label: 'Consumers', href: '#consumers' },
  { key: 'policies', label: 'Policies', href: '#policies' },
];

const tabContent: Record<string, { heading: string; rows: { label: string; value: string }[] }> = {
  activity: {
    heading: 'Recent Activity',
    rows: [
      { label: 'API deployed to production', value: '2 minutes ago' },
      { label: 'Health check passed', value: '10 minutes ago' },
      { label: 'Rate limit policy updated', value: '1 hour ago' },
      { label: 'New subscription approved', value: '3 hours ago' },
    ],
  },
  configuration: {
    heading: 'General Configuration',
    rows: [
      { label: 'Endpoint', value: 'https://api.example.com/v2/payments' },
      { label: 'Load balancing', value: 'Round Robin' },
      { label: 'Timeout', value: '30 000 ms' },
      { label: 'CORS', value: 'Enabled' },
    ],
  },
  consumers: {
    heading: 'Active Consumers',
    rows: [
      { label: 'Mobile App (iOS)', value: '12 400 req/day' },
      { label: 'Mobile App (Android)', value: '9 800 req/day' },
      { label: 'Partner Portal', value: '3 200 req/day' },
      { label: 'Internal Dashboard', value: '1 100 req/day' },
    ],
  },
  policies: {
    heading: 'Applied Policies',
    rows: [
      { label: 'Rate Limiting', value: '1 000 req/min' },
      { label: 'JWT Authentication', value: 'Active' },
      { label: 'IP Filtering', value: '3 rules' },
      { label: 'Request Transformation', value: 'Active' },
    ],
  },
};

export const APIDetails: Story = {
  render: () => {
    const [activeTab, setActiveTab] = useState('activity');
    const content = tabContent[activeTab];

    return (
      <AppShell
        activeNavKey="api-list"
        breadcrumbs={[
          { label: 'APIs & Events', href: '#' },
          { label: 'API List', href: '#' },
          { label: 'Payment Service v2' },
        ]}
      >
        <PageLayout
          title="Payment Service v2"
          description="REST API for processing payments and managing transactions."
          tabs={apiDetailsTabs}
          activeTab={activeTab}
          onTabClick={(tab, e) => {
            e.preventDefault();
            setActiveTab(tab.key);
          }}
          actions={
            <Button size="sm">
              <Rocket className="mr-1.5 size-3.5" />
              Deploy
            </Button>
          }
        >
          <div className="rounded-lg border border-border bg-card p-6 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold text-card-foreground">{content.heading}</h2>
            <div className="space-y-3">
              {content.rows.map(({ label, value }) => (
                <div key={label} className="flex items-center justify-between border-b border-border pb-3 last:border-0">
                  <span className="text-sm text-foreground">{label}</span>
                  <span className="text-sm text-muted-foreground">{value}</span>
                </div>
              ))}
            </div>
          </div>
        </PageLayout>
      </AppShell>
    );
  },
};

const apis = [
  { name: 'Payment Service v2', status: 'Published', version: '2.1.0', calls: '24.5k/day' },
  { name: 'User Management', status: 'Published', version: '1.8.3', calls: '18.2k/day' },
  { name: 'Order Processing', status: 'Published', version: '3.0.1', calls: '12.1k/day' },
  { name: 'Inventory Sync', status: 'Staging', version: '1.0.0-rc.2', calls: '940/day' },
  { name: 'Notification Gateway', status: 'Published', version: '2.4.0', calls: '8.7k/day' },
  { name: 'Analytics Collector', status: 'Draft', version: '0.9.0', calls: '--' },
];

export const APIList: Story = {
  render: () => (
    <AppShell
      activeNavKey="api-list"
      breadcrumbs={[{ label: 'APIs & Events', href: '#' }, { label: 'API List' }]}
    >
      <PageLayout
        title="API List"
        description="Browse and manage all your APIs."
        actions={
          <Button size="sm">
            <Plus className="mr-1.5 size-3.5" />
            Create API
          </Button>
        }
      >
        <div className="overflow-hidden rounded-lg border border-border">
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Name</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Version</th>
                <th className="px-4 py-3 text-right font-medium text-muted-foreground">Calls</th>
              </tr>
            </thead>
            <tbody>
              {apis.map((api) => (
                <tr key={api.name} className="border-b border-border last:border-0 hover:bg-muted/30">
                  <td className="px-4 py-3 font-medium text-foreground">{api.name}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                        api.status === 'Published'
                          ? 'bg-success/10 text-success'
                          : api.status === 'Staging'
                            ? 'bg-warning/10 text-warning'
                            : 'bg-muted text-muted-foreground'
                      }`}
                    >
                      {api.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{api.version}</td>
                  <td className="px-4 py-3 text-right text-muted-foreground">{api.calls}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </PageLayout>
    </AppShell>
  ),
};
