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
} from 'lucide-react';
import { SidebarProvider } from '@baros/components/ui/sidebar';
import { AppSidebar } from './AppSidebar';
import type { NavItem } from './AppSidebar';

const mockNavItems: NavItem[] = [
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

const mockUser = {
  name: 'Jane Doe',
  email: 'jane.doe@gravitee.io',
};

const meta = {
  title: 'Layout/AppSidebar',
  component: AppSidebar,
  tags: ['autodocs'],
  parameters: {
    layout: 'fullscreen',
  },
  decorators: [
    Story => (
      <SidebarProvider>
        <div className="flex h-screen">
          <Story />
        </div>
      </SidebarProvider>
    ),
  ],
} satisfies Meta<typeof AppSidebar>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    logo: <span className="text-lg font-bold text-primary">Gravitee</span>,
    collapsedLogo: <span className="text-lg font-bold text-primary">G</span>,
    navItems: mockNavItems,
    activeItemKey: 'api-list',
    user: mockUser,
  },
};

export const WithActiveSubItem: Story = {
  render: () => {
    const [activeKey, setActiveKey] = useState('policies');
    return (
      <AppSidebar
        logo={<span className="text-lg font-bold text-primary">Gravitee</span>}
        collapsedLogo={<span className="text-lg font-bold text-primary">G</span>}
        navItems={mockNavItems}
        activeItemKey={activeKey}
        onNavItemClick={setActiveKey}
        user={mockUser}
      />
    );
  },
};

export const Minimal: Story = {
  args: {
    logo: <span className="text-lg font-bold text-primary">Gravitee</span>,
    collapsedLogo: <span className="text-lg font-bold text-primary">G</span>,
    navItems: mockNavItems.slice(0, 3),
    activeItemKey: 'agent-overview',
  },
};
