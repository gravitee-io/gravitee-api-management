import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Building2, Landmark, Command } from 'lucide-react';
import { OrgEnvSelector } from './OrgEnvSelector';

const meta = {
  title: 'Layout/OrgEnvSelector',
  component: OrgEnvSelector,
  tags: ['autodocs'],
} satisfies Meta<typeof OrgEnvSelector>;

export default meta;
type Story = StoryObj<typeof meta>;

const orgs = [
  { key: 'gravitee', name: 'Gravitee Inc', icon: Building2 },
  { key: 'acme', name: 'Acme Corp.', icon: Landmark },
  { key: 'wayne', name: 'Wayne Tech', icon: Command },
];

const envs = [
  { key: 'prod', name: 'Production' },
  { key: 'staging', name: 'Staging' },
  { key: 'dev', name: 'Development' },
];

export const Default: Story = {
  args: {
    organizations: orgs,
    environments: envs,
    activeOrgKey: 'gravitee',
    activeEnvKey: 'prod',
  },
};

export const Interactive: Story = {
  render: () => {
    const [orgKey, setOrgKey] = useState('gravitee');
    const [envKey, setEnvKey] = useState('prod');

    return (
      <OrgEnvSelector
        organizations={orgs}
        environments={envs}
        activeOrgKey={orgKey}
        activeEnvKey={envKey}
        onOrgChange={setOrgKey}
        onEnvChange={setEnvKey}
      />
    );
  },
};
