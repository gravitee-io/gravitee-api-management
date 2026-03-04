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
  ChevronDown,
  Search,
  Home,
} from 'lucide-react';
import { Button } from '@baros/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '@baros/components/ui/dropdown-menu';
import { Separator } from '@baros/components/ui/separator';
import { SidebarTrigger } from '@baros/components/ui/sidebar';
import { AppSidebar } from '../AppSidebar';
import type { NavItem } from '../AppSidebar';
import { GraviteeLogo, GraviteeIcon } from '../GraviteeLogo';
import { ThemeToggle } from '../ThemeToggle';
import { TopNav } from '../TopNav';
import { MainLayout } from './MainLayout';

interface Organization {
  name: string;
  logo: React.ElementType;
  plan: string;
}

const organizations: Organization[] = [
  { name: 'Gravitee Inc', logo: Building2, plan: 'Enterprise' },
  { name: 'Acme Corp.', logo: Landmark, plan: 'Startup' },
  { name: 'Wayne Tech', logo: Command, plan: 'Free' },
];

const environments = ['Production', 'Staging', 'Development'];

const navItems: NavItem[] = [
  {
    key: 'dashboard',
    title: 'Dashboard',
    url: '#',
    icon: Home,
  },
  {
    key: 'apis-events',
    title: 'APIs & Events',
    url: '#',
    icon: Globe,
    items: [
      { key: 'api-list', title: 'API List', url: '#' },
      { key: 'event-streams', title: 'Event Streams', url: '#' },
      { key: 'subscriptions', title: 'Subscriptions', url: '#' },
    ],
  },
  {
    key: 'ai-agents',
    title: 'AI & Agents',
    url: '#',
    icon: Bot,
    items: [
      { key: 'agent-overview', title: 'Overview', url: '#' },
      { key: 'agent-configs', title: 'Configurations', url: '#' },
    ],
  },
  {
    key: 'tools-integrations',
    title: 'Tools & Integrations',
    url: '#',
    icon: Puzzle,
    items: [
      { key: 'connectors', title: 'Connectors', url: '#' },
      { key: 'plugins', title: 'Plugins', url: '#' },
    ],
  },
  {
    key: 'mcp-studio',
    title: 'MCP Studio',
    url: '#',
    icon: FlaskConical,
    items: [
      { key: 'mcp-editor', title: 'Editor', url: '#' },
      { key: 'mcp-deployments', title: 'Deployments', url: '#' },
    ],
  },
  {
    key: 'governance',
    title: 'Governance',
    url: '#',
    icon: Shield,
    items: [
      { key: 'policies', title: 'Policies', url: '#' },
      { key: 'quality-rules', title: 'Quality Rules', url: '#' },
    ],
  },
  {
    key: 'observability',
    title: 'Observability',
    url: '#',
    icon: BarChart3,
    items: [
      { key: 'dashboards', title: 'Dashboards', url: '#' },
      { key: 'alerts', title: 'Alerts', url: '#' },
      { key: 'logs', title: 'Logs', url: '#' },
    ],
  },
  {
    key: 'access-tenancy',
    title: 'Access & Tenancy',
    url: '#',
    icon: Users,
    items: [
      { key: 'users', title: 'Users', url: '#' },
      { key: 'roles', title: 'Roles', url: '#' },
      { key: 'tenants', title: 'Tenants', url: '#' },
    ],
  },
  {
    key: 'developer-portal',
    title: 'Developer Portal',
    url: '#',
    icon: BookOpen,
    items: [
      { key: 'portal-home', title: 'Portal Home', url: '#' },
      { key: 'api-catalog', title: 'API Catalog', url: '#' },
      { key: 'portal-docs', title: 'Documentation', url: '#' },
    ],
  },
];

function findParentTitle(key: string): string {
  for (const item of navItems) {
    if (item.key === key) return item.title;
    if (item.items?.some(sub => sub.key === key)) return item.title;
  }
  return 'Home';
}

function findSubTitle(key: string): string | undefined {
  for (const item of navItems) {
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
    const [activeOrg, setActiveOrg] = useState(organizations[0]);
    const [activeEnv, setActiveEnv] = useState(environments[0]);

    const parentTitle = findParentTitle(activeKey);
    const subTitle = findSubTitle(activeKey);

    return (
      <MainLayout
        sidebar={
          <AppSidebar
            logo={<GraviteeLogo />}
            collapsedLogo={<GraviteeIcon />}
            navItems={navItems}
            activeItemKey={activeKey}
            onNavItemClick={setActiveKey}
            user={{ name: 'Jane Doe', email: 'jane.doe@gravitee.io' }}
          />
        }
        topnav={
          <TopNav
            leading={
              <div className="flex items-center gap-1">
                <SidebarTrigger />
                <Separator orientation="vertical" className="mx-1 h-4" />

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="sm" className="h-7 gap-1 px-2 text-xs">
                      <activeOrg.logo className="size-3.5" />
                      {activeOrg.name}
                      <ChevronDown className="size-3 opacity-50" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="start" className="min-w-48">
                    <DropdownMenuGroup>
                      <DropdownMenuLabel className="text-xs text-muted-foreground">Organizations</DropdownMenuLabel>
                      {organizations.map(org => (
                        <DropdownMenuItem key={org.name} onClick={() => setActiveOrg(org)} className="gap-2">
                          <org.logo className="size-3.5 shrink-0" />
                          {org.name}
                        </DropdownMenuItem>
                      ))}
                    </DropdownMenuGroup>
                  </DropdownMenuContent>
                </DropdownMenu>

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="sm" className="h-7 gap-1 px-2 text-xs">
                      {activeEnv}
                      <ChevronDown className="size-3 opacity-50" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="start" className="min-w-40">
                    <DropdownMenuGroup>
                      <DropdownMenuLabel className="text-xs text-muted-foreground">Environments</DropdownMenuLabel>
                      {environments.map(env => (
                        <DropdownMenuItem key={env} onClick={() => setActiveEnv(env)}>
                          {env}
                        </DropdownMenuItem>
                      ))}
                    </DropdownMenuGroup>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            }
            trailing={
              <div className="flex items-center gap-1">
                <div className="relative">
                  <Search className="absolute left-2 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                  <input
                    type="search"
                    placeholder={`Search in ${parentTitle}...`}
                    className="h-7 w-48 rounded-md border border-input bg-background pl-8 pr-2 text-xs placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    aria-label={`Search within ${parentTitle}`}
                  />
                </div>
                <ThemeToggle />
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
          navItems={navItems}
          activeItemKey="policies"
          user={{ name: 'Jane Doe', email: 'jane.doe@gravitee.io' }}
        />
      }
      topnav={
        <TopNav
          leading={
            <div className="flex items-center gap-1">
              <SidebarTrigger />
              <Separator orientation="vertical" className="mx-1 h-4" />
              <span className="text-xs font-medium">Governance</span>
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
    <MainLayout topnav={<TopNav leading={<h1 className="text-xs font-semibold">Full Width</h1>} />}>
      <div className="rounded-lg border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-card-foreground">Full Width Layout</h2>
        <p className="mt-2 text-sm text-muted-foreground">No sidebar, content takes the full width.</p>
      </div>
    </MainLayout>
  ),
};
