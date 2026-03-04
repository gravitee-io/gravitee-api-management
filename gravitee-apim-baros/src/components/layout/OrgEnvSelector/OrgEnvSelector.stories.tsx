import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react';
import { Building2, Landmark, Command } from 'lucide-react';
import { OrgSelector, EnvSelector } from './OrgEnvSelector';

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

const orgMeta = {
  title: 'Layout/OrgSelector',
  component: OrgSelector,
  tags: ['autodocs'],
} satisfies Meta<typeof OrgSelector>;

export default orgMeta;
type Story = StoryObj<typeof orgMeta>;

export const DefaultOrg: Story = {
  args: {
    organizations: orgs,
    activeOrgKey: 'gravitee',
  },
};

export const InteractiveOrg: Story = {
  render: () => {
    const [orgKey, setOrgKey] = useState('gravitee');
    return <OrgSelector organizations={orgs} activeOrgKey={orgKey} onOrgChange={setOrgKey} />;
  },
};

export const DefaultEnv: Story = {
  render: () => {
    const [envKey, setEnvKey] = useState('prod');
    return <EnvSelector environments={envs} activeEnvKey={envKey} onEnvChange={setEnvKey} />;
  },
};

export const Combined: Story = {
  render: () => {
    const [orgKey, setOrgKey] = useState('gravitee');
    const [envKey, setEnvKey] = useState('prod');

    return (
      <div className="flex items-center gap-2">
        <OrgSelector organizations={orgs} activeOrgKey={orgKey} onOrgChange={setOrgKey} />
        <EnvSelector environments={envs} activeEnvKey={envKey} onEnvChange={setEnvKey} />
      </div>
    );
  },
};
