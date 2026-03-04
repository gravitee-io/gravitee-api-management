import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Globe, Radio, BookOpen, ShieldCheck } from 'lucide-react';
import { AppDropdown } from './AppDropdown';

const mockApps = [
  { key: 'api-management', name: 'API Management', icon: Globe },
  { key: 'kafka', name: 'Kafka', icon: Radio },
  { key: 'developer-portal', name: 'Developer Portal', icon: BookOpen },
  { key: 'access-management', name: 'Access Management', icon: ShieldCheck },
];

const meta = {
  title: 'Layout/AppDropdown',
  component: AppDropdown,
  tags: ['autodocs'],
} satisfies Meta<typeof AppDropdown>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    apps: mockApps,
    activeAppKey: 'api-management',
  },
};

export const Interactive: Story = {
  render: () => {
    const [appKey, setAppKey] = useState('api-management');

    return <AppDropdown apps={mockApps} activeAppKey={appKey} onAppChange={setAppKey} />;
  },
};
