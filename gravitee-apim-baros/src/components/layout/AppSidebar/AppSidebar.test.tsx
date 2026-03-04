import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Globe, Shield } from 'lucide-react';
import { SidebarProvider } from '@baros/components/ui/sidebar';
import type { AppSidebarProps, NavItem } from './AppSidebar';
import { AppSidebar } from './AppSidebar';

const mockNavItems: NavItem[] = [
  {
    key: 'apis',
    title: 'APIs & Events',
    url: '#',
    icon: Globe,
    items: [
      { key: 'api-list', title: 'API List', url: '#' },
      { key: 'event-streams', title: 'Event Streams', url: '#' },
    ],
  },
  {
    key: 'gov',
    title: 'Governance',
    url: '#',
    icon: Shield,
    items: [
      { key: 'policies', title: 'Policies', url: '#' },
      { key: 'quality-rules', title: 'Quality Rules', url: '#' },
    ],
  },
];

function setupAppSidebarHarness(props?: Partial<AppSidebarProps>) {
  const user = userEvent.setup();
  render(
    <SidebarProvider defaultOpen>
      <AppSidebar
        navItems={mockNavItems}
        {...props}
      />
    </SidebarProvider>,
  );

  return {
    getNav: () => screen.getByRole('navigation'),
    clickParentItem: (name: string) =>
      user.click(screen.getByRole('button', { name: new RegExp(name, 'i') })),
    querySubItem: (name: string) => screen.queryByText(name),
  };
}

describe('AppSidebar', () => {
  it('renders parent navigation items', () => {
    const harness = setupAppSidebarHarness();

    expect(harness.getNav()).toBeInTheDocument();
    expect(screen.getByText('APIs & Events')).toBeInTheDocument();
    expect(screen.getByText('Governance')).toBeInTheDocument();
  });

  it('auto-expands the parent containing the active sub-item', () => {
    const harness = setupAppSidebarHarness({ activeItemKey: 'api-list' });

    expect(harness.querySubItem('API List')).toBeInTheDocument();
    expect(harness.querySubItem('Event Streams')).toBeInTheDocument();
  });

  it('expands sub-items when a parent item is clicked', async () => {
    const harness = setupAppSidebarHarness();

    expect(harness.querySubItem('Policies')).not.toBeInTheDocument();

    await harness.clickParentItem('Governance');

    expect(harness.querySubItem('Policies')).toBeInTheDocument();
    expect(harness.querySubItem('Quality Rules')).toBeInTheDocument();
  });

  it('renders footer content when provided', () => {
    setupAppSidebarHarness({ footer: <div data-testid="sidebar-footer">Org / Env</div> });

    expect(screen.getByTestId('sidebar-footer')).toHaveTextContent('Org / Env');
  });

  it('calls onNavItemClick when a sub-item is clicked', async () => {
    const onNavItemClick = vi.fn();
    const user = userEvent.setup();
    render(
      <SidebarProvider defaultOpen>
        <AppSidebar
          navItems={mockNavItems}
          activeItemKey="api-list"
          onNavItemClick={onNavItemClick}
        />
      </SidebarProvider>,
    );

    await user.click(screen.getByText('Event Streams'));

    expect(onNavItemClick).toHaveBeenCalledWith('event-streams');
  });
});
