import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Building2 } from 'lucide-react';
import { OrgSelector, EnvSelector, type OrgSelectorProps, type EnvSelectorProps } from './OrgEnvSelector';

const defaultOrgProps: OrgSelectorProps = {
  organizations: [
    { key: 'gravitee', name: 'Gravitee Inc', icon: Building2 },
    { key: 'acme', name: 'Acme Corp' },
  ],
  activeOrgKey: 'gravitee',
};

const defaultEnvProps: EnvSelectorProps = {
  environments: [
    { key: 'prod', name: 'Production' },
    { key: 'staging', name: 'Staging' },
  ],
  activeEnvKey: 'prod',
};

describe('OrgSelector', () => {
  it('renders without crashing', () => {
    render(<OrgSelector {...defaultOrgProps} />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('displays the active org name', () => {
    render(<OrgSelector {...defaultOrgProps} />);
    expect(screen.getByText('Gravitee Inc')).toBeInTheDocument();
  });

  it('shows org options when opened', async () => {
    const user = userEvent.setup();
    render(<OrgSelector {...defaultOrgProps} />);

    await user.click(screen.getByRole('button'));
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
  });
});

describe('EnvSelector', () => {
  it('renders without crashing', () => {
    render(<EnvSelector {...defaultEnvProps} />);
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('displays the active env name', () => {
    render(<EnvSelector {...defaultEnvProps} />);
    expect(screen.getByText('Production')).toBeInTheDocument();
  });

  it('shows env options when opened', async () => {
    const user = userEvent.setup();
    render(<EnvSelector {...defaultEnvProps} />);

    await user.click(screen.getByRole('button'));
    expect(screen.getByText('Staging')).toBeInTheDocument();
  });
});
