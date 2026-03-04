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
  Home,
} from 'lucide-react';
import type { NavItem } from '@baros/components/layout/AppSidebar';
import type { OrgOption, EnvOption } from '@baros/components/layout/OrgEnvSelector';

export const mockNavItems: NavItem[] = [
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

export const mockOrganizations: OrgOption[] = [
  { key: 'gravitee', name: 'Gravitee Inc', icon: Building2 },
  { key: 'acme', name: 'Acme Corp.', icon: Landmark },
  { key: 'wayne', name: 'Wayne Tech', icon: Command },
];

export const mockEnvironments: EnvOption[] = [
  { key: 'prod', name: 'Production' },
  { key: 'staging', name: 'Staging' },
  { key: 'dev', name: 'Development' },
];

export const mockUser = { name: 'Jane Doe', email: 'jane.doe@gravitee.io' };
