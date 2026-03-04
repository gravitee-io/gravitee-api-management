import type { Meta, StoryObj } from '@storybook/react';
import { LayoutDashboard, Globe, Settings, HelpCircle } from 'lucide-react';
import { SideNav } from './SideNav';

const sampleItems = [
  { icon: <LayoutDashboard className="h-5 w-5" />, label: 'Dashboard' },
  { icon: <Globe className="h-5 w-5" />, label: 'APIs', active: true },
  { icon: <Settings className="h-5 w-5" />, label: 'Settings' },
];

const meta = {
  title: 'Layout/SideNav',
  component: SideNav,
  tags: ['autodocs'],
  decorators: [
    Story => (
      <div className="h-[600px]">
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof SideNav>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    header: <span className="text-lg font-semibold">Gravitee</span>,
    items: sampleItems,
    footer: (
      <button type="button" className="flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-sidebar-accent">
        <HelpCircle className="h-5 w-5" />
        <span>Help</span>
      </button>
    ),
  },
};

export const Collapsed: Story = {
  args: {
    collapsed: true,
    header: <span className="text-lg font-semibold">G</span>,
    items: sampleItems,
    footer: <HelpCircle className="h-5 w-5 text-muted-foreground" />,
  },
};
