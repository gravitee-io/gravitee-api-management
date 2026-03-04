import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Globe, BookOpen } from 'lucide-react';
import { AppDropdown } from './AppDropdown';

const apps = [
  { key: 'api-management', name: 'API Management', icon: Globe },
  { key: 'developer-portal', name: 'Developer Portal', icon: BookOpen },
];

function setupAppDropdownHarness(props?: Partial<Parameters<typeof AppDropdown>[0]>) {
  const user = userEvent.setup();
  render(<AppDropdown apps={apps} activeAppKey="api-management" {...props} />);

  return {
    getTrigger: () => screen.getByRole('button', { name: /API Management/i }),
    openMenu: async () => {
      await user.click(screen.getByRole('button', { name: /API Management/i }));
    },
  };
}

describe('AppDropdown', () => {
  it('renders the active app name in the trigger', () => {
    const harness = setupAppDropdownHarness();
    expect(harness.getTrigger()).toBeInTheDocument();
  });

  it('shows menu items when opened', async () => {
    const harness = setupAppDropdownHarness();
    await harness.openMenu();
    expect(screen.getByText('Developer Portal')).toBeInTheDocument();
  });
});
