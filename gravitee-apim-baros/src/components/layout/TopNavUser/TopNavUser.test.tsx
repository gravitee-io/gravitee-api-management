import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TopNavUser, type TopNavUserProps } from './TopNavUser';

const defaultUser = { name: 'Jane Doe', email: 'jane@example.com' };

function setupTopNavUserHarness(props?: Partial<TopNavUserProps>) {
  const user = userEvent.setup();
  render(<TopNavUser user={defaultUser} {...props} />);

  return {
    clickTrigger: () => user.click(screen.getByRole('button')),
    getRoot: () => screen.getByRole('button'),
  };
}

describe('TopNavUser', () => {
  it('renders without crashing', () => {
    const harness = setupTopNavUserHarness();
    expect(harness.getRoot()).toBeInTheDocument();
  });

  it('shows user initials in the avatar', () => {
    setupTopNavUserHarness();
    expect(screen.getByText('JD')).toBeInTheDocument();
  });
});
