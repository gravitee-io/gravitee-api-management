import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import {
  Globe,
  Bot,
  Puzzle,
  FlaskConical,
  Shield,
  BarChart3,
  Users,
  BookOpen,
  Building2,
  Landmark,
  Command,
} from 'lucide-react';
import { Separator } from '@baros/components/ui/separator';
import { SidebarTrigger } from '@baros/components/ui/sidebar';
import { AppSidebar } from '../AppSidebar';
import type { NavItem, Organization } from '../AppSidebar';
import { ThemeToggle } from '../ThemeToggle';
import { TopNav } from '../TopNav';
import { MainLayout } from './MainLayout';

const organizations: Organization[] = [
  { name: 'Gravitee Inc', logo: Building2, plan: 'Enterprise' },
  { name: 'Acme Corp.', logo: Landmark, plan: 'Startup' },
  { name: 'Wayne Tech', logo: Command, plan: 'Free' },
];

const navItems: NavItem[] = [
  { key: 'apis-events', title: 'APIs & Events', url: '#', icon: Globe },
  { key: 'ai-agents', title: 'AI & Agents', url: '#', icon: Bot },
  { key: 'tools-integrations', title: 'Tools & Integrations', url: '#', icon: Puzzle },
  { key: 'mcp-studio', title: 'MCP Studio', url: '#', icon: FlaskConical },
  { key: 'governance', title: 'Governance', url: '#', icon: Shield },
  { key: 'observability', title: 'Observability', url: '#', icon: BarChart3 },
  { key: 'access-tenancy', title: 'Access & Tenancy', url: '#', icon: Users },
  { key: 'developer-portal', title: 'Developer Portal', url: '#', icon: BookOpen },
];

const sectionsWithEnvSelector = new Set(['apis-events', 'developer-portal']);

const environments = ['Production', 'Staging', 'Development'];

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
    const [activeKey, setActiveKey] = useState('apis-events');
    const showEnvSelector = sectionsWithEnvSelector.has(activeKey);

    return (
      <MainLayout
        sidebar={
          <AppSidebar
            logo={<span className="text-lg font-bold text-primary">Gravitee</span>}
            organizations={organizations}
            navItems={navItems}
            activeItemKey={activeKey}
            onNavItemClick={setActiveKey}
            user={{ name: 'Jane Doe', email: 'jane.doe@gravitee.io' }}
          />
        }
        topnav={
          <TopNav
            leading={
              <div className="flex items-center gap-2">
                <SidebarTrigger />
                <Separator orientation="vertical" className="h-4" />
                <nav aria-label="Breadcrumb" className="flex items-center gap-1 text-sm text-muted-foreground">
                  <span className="font-medium text-foreground">
                    {navItems.find(i => i.key === activeKey)?.title ?? 'Home'}
                  </span>
                </nav>
              </div>
            }
            center={
              showEnvSelector ? (
                <select
                  className="rounded-md border border-input bg-background px-3 py-1 text-sm"
                  aria-label="Environment"
                  defaultValue="Production"
                >
                  {environments.map(env => (
                    <option key={env} value={env}>{env}</option>
                  ))}
                </select>
              ) : undefined
            }
            trailing={<ThemeToggle />}
          />
        }
      >
        <div className="space-y-6">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              {navItems.find(i => i.key === activeKey)?.title ?? 'Home'}
            </h1>
            <p className="text-sm text-muted-foreground">
              Welcome to the Gravitee API Management console.
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
          logo={<span className="text-lg font-bold text-primary">G</span>}
          organizations={organizations}
          navItems={navItems}
          activeItemKey="governance"
          user={{ name: 'Jane Doe', email: 'jane.doe@gravitee.io' }}
        />
      }
      topnav={
        <TopNav
          leading={
            <div className="flex items-center gap-2">
              <SidebarTrigger />
              <Separator orientation="vertical" className="h-4" />
              <span className="text-sm font-semibold">Governance</span>
            </div>
          }
          trailing={<ThemeToggle />}
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
    <MainLayout topnav={<TopNav leading={<h1 className="text-sm font-semibold">Full Width</h1>} />}>
      <div className="rounded-lg border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-card-foreground">Full Width Layout</h2>
        <p className="mt-2 text-sm text-muted-foreground">No sidebar, content takes the full width.</p>
      </div>
    </MainLayout>
  ),
};
