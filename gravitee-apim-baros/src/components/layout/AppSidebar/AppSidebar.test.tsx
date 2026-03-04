import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Globe, Shield, Building2 } from 'lucide-react';
import { SidebarProvider } from '@baros/components/ui/sidebar';
import type { AppSidebarProps, Organization, NavItem } from './AppSidebar';
import { AppSidebar } from './AppSidebar';

const mockOrgs: Organization[] = [
  { name: 'Org Alpha', logo: Building2, plan: 'Enterprise' },
  { name: 'Org Beta', logo: Building2, plan: 'Free' },
];

const mockNavItems: NavItem[] = [
  { key: 'apis', title: 'APIs & Events', url: '#', icon: Globe },
  { key: 'gov', title: 'Governance', url: '#', icon: Shield },
];

const mockUser = { name: 'Jane Doe', email: 'jane@example.com' };

function setupAppSidebarHarness(props?: Partial<AppSidebarProps>) {
  const user = userEvent.setup();
  render(
    <SidebarProvider defaultOpen>
      <AppSidebar
        organizations={mockOrgs}
        navItems={mockNavItems}
        user={mockUser}
        {...props}
      />
    </SidebarProvider>,
  );

  return {
    getNav: () => screen.getByRole('navigation'),
    getNavItems: () => within(screen.getByRole('navigation')).getAllByRole('link'),
    getOrgTrigger: () => screen.getByText('Org Alpha'),
    getUserTrigger: () => screen.getByText('Jane Doe'),
    clickOrgTrigger: () => user.click(screen.getByText('Org Alpha')),
    clickNavItem: (name: string) =>
      user.click(screen.getByRole('link', { name: new RegExp(name, 'i') })),
  };
}

describe('AppSidebar', () => {
  it('renders navigation items', () => {
    const harness = setupAppSidebarHarness();

    expect(harness.getNav()).toBeInTheDocument();
    expect(harness.getNavItems()).toHaveLength(2);
    expect(screen.getByText('APIs & Events')).toBeInTheDocument();
    expect(screen.getByText('Governance')).toBeInTheDocument();
  });

  it('renders the active organization name', () => {
    const harness = setupAppSidebarHarness();

    expect(harness.getOrgTrigger()).toBeInTheDocument();
  });

  it('renders user info in the footer', () => {
    const harness = setupAppSidebarHarness();

    expect(harness.getUserTrigger()).toBeInTheDocument();
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
  });

  it('marks the active nav item', () => {
    setupAppSidebarHarness({ activeItemKey: 'apis' });

    const link = screen.getByRole('link', { name: /apis & events/i });
    expect(link.closest('[data-active="true"]')).toBeTruthy();
  });

  it('calls onNavItemClick when a nav item is clicked', async () => {
    const onNavItemClick = vi.fn();
    const harness = setupAppSidebarHarness({ onNavItemClick });

    await harness.clickNavItem('Governance');

    expect(onNavItemClick).toHaveBeenCalledWith('gov');
  });

  it('renders logo when provided', () => {
    setupAppSidebarHarness({ logo: <span>Logo</span> });

    expect(screen.getByText('Logo')).toBeInTheDocument();
  });
});
