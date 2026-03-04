import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { SidebarProvider } from '@baros/components/ui/sidebar';
import { mockNavItems, mockOrganizations, mockEnvironments } from '../../../../.storybook/mock-data';
import { OrgSelector, EnvSelector } from '../OrgEnvSelector';
import { AppSidebar } from './AppSidebar';

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
    navItems: mockNavItems,
    activeItemKey: 'api-list',
    footer: (
      <div className="flex flex-col gap-2">
        <OrgSelector organizations={mockOrganizations} activeOrgKey="gravitee" />
        <EnvSelector environments={mockEnvironments} activeEnvKey="prod" />
      </div>
    ),
  },
};

export const WithActiveSubItem: Story = {
  render: () => {
    const [activeKey, setActiveKey] = useState('policies');
    const [activeOrgKey, setActiveOrgKey] = useState('gravitee');
    const [activeEnvKey, setActiveEnvKey] = useState('prod');
    return (
      <AppSidebar
        navItems={mockNavItems}
        activeItemKey={activeKey}
        onNavItemClick={setActiveKey}
        footer={
          <div className="flex flex-col gap-2">
            <OrgSelector organizations={mockOrganizations} activeOrgKey={activeOrgKey} onOrgChange={setActiveOrgKey} />
            <EnvSelector environments={mockEnvironments} activeEnvKey={activeEnvKey} onEnvChange={setActiveEnvKey} />
          </div>
        }
      />
    );
  },
};

export const Minimal: Story = {
  args: {
    navItems: mockNavItems.slice(0, 3),
    activeItemKey: 'agent-overview',
  },
};
