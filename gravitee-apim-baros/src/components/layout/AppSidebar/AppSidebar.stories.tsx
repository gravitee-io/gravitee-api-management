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
import { SidebarProvider } from '@baros/components/ui/sidebar';
import { AppSidebar } from './AppSidebar';
import type { Organization, NavItem } from './AppSidebar';

const mockOrganizations: Organization[] = [
  { name: 'Gravitee Inc', logo: Building2, plan: 'Enterprise' },
  { name: 'Acme Corp.', logo: Landmark, plan: 'Startup' },
  { name: 'Wayne Tech', logo: Command, plan: 'Free' },
];

const mockNavItems: NavItem[] = [
  { key: 'apis-events', title: 'APIs & Events', url: '#', icon: Globe },
  { key: 'ai-agents', title: 'AI & Agents', url: '#', icon: Bot },
  { key: 'tools-integrations', title: 'Tools & Integrations', url: '#', icon: Puzzle },
  { key: 'mcp-studio', title: 'MCP Studio', url: '#', icon: FlaskConical },
  { key: 'governance', title: 'Governance', url: '#', icon: Shield },
  { key: 'observability', title: 'Observability', url: '#', icon: BarChart3 },
  { key: 'access-tenancy', title: 'Access & Tenancy', url: '#', icon: Users },
  { key: 'developer-portal', title: 'Developer Portal', url: '#', icon: BookOpen },
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
    organizations: mockOrganizations,
    navItems: mockNavItems,
    activeItemKey: 'apis-events',
    user: mockUser,
  },
};

export const WithActiveItem: Story = {
  render: () => {
    const [activeKey, setActiveKey] = useState('governance');
    return (
      <AppSidebar
        logo={<span className="text-lg font-bold text-primary">Gravitee</span>}
        organizations={mockOrganizations}
        navItems={mockNavItems}
        activeItemKey={activeKey}
        onNavItemClick={setActiveKey}
        user={mockUser}
      />
    );
  },
};

export const MinimalNoOrgs: Story = {
  args: {
    logo: <span className="text-lg font-bold text-primary">G</span>,
    navItems: mockNavItems.slice(0, 4),
    activeItemKey: 'mcp-studio',
  },
};
