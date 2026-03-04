import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Building2 } from 'lucide-react';
import { OrgEnvSelector, type OrgEnvSelectorProps } from './OrgEnvSelector';

const defaultProps: OrgEnvSelectorProps = {
  organizations: [
    { key: 'gravitee', name: 'Gravitee Inc', icon: Building2 },
    { key: 'acme', name: 'Acme Corp' },
  ],
  environments: [
    { key: 'prod', name: 'Production' },
    { key: 'staging', name: 'Staging' },
  ],
  activeOrgKey: 'gravitee',
  activeEnvKey: 'prod',
};

function setupOrgEnvSelectorHarness(props?: Partial<OrgEnvSelectorProps>) {
  const user = userEvent.setup();
  render(<OrgEnvSelector {...defaultProps} {...props} />);

  return {
    clickTrigger: () => user.click(screen.getByRole('button')),
    getRoot: () => screen.getByRole('button'),
  };
}

describe('OrgEnvSelector', () => {
  it('renders without crashing', () => {
    const harness = setupOrgEnvSelectorHarness();
    expect(harness.getRoot()).toBeInTheDocument();
  });

  it('displays the active org and env names', () => {
    setupOrgEnvSelectorHarness();
    expect(screen.getByText('Gravitee Inc')).toBeInTheDocument();
    expect(screen.getByText('Production')).toBeInTheDocument();
  });
});
