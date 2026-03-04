import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { SidebarProvider } from '@baros/components/ui/sidebar';
import { GraviteeLogo, GraviteeIcon } from '../GraviteeLogo';
import { mockNavItems } from '../../../../.storybook/mock-data';
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
    logo: <GraviteeLogo />,
    collapsedLogo: <GraviteeIcon />,
    navItems: mockNavItems,
    activeItemKey: 'api-list',
  },
};

export const WithActiveSubItem: Story = {
  render: () => {
    const [activeKey, setActiveKey] = useState('policies');
    return (
      <AppSidebar
        logo={<GraviteeLogo />}
        collapsedLogo={<GraviteeIcon />}
        navItems={mockNavItems}
        activeItemKey={activeKey}
        onNavItemClick={setActiveKey}
      />
    );
  },
};

export const Minimal: Story = {
  args: {
    logo: <GraviteeLogo />,
    collapsedLogo: <GraviteeIcon />,
    navItems: mockNavItems.slice(0, 3),
    activeItemKey: 'agent-overview',
  },
};
