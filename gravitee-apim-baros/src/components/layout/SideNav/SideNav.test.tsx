import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { SideNavProps } from './SideNav';
import { SideNav } from './SideNav';

const defaultItems = [
  { icon: <span>D</span>, label: 'Dashboard' },
  { icon: <span>A</span>, label: 'APIs', active: true },
  { icon: <span>S</span>, label: 'Settings' },
];

function setupSideNavHarness(props?: Partial<SideNavProps>) {
  const user = userEvent.setup();
  render(<SideNav items={defaultItems} header={<span>Logo</span>} {...props} />);

  return {
    getNav: () => screen.getByRole('navigation', { name: /main navigation/i }),
    getItems: () => screen.getAllByRole('button'),
    getActiveItem: () => screen.getByRole('button', { current: 'page' }),
    getHeader: () => screen.getByText('Logo'),
    clickItem: (name: string) => user.click(screen.getByRole('button', { name: new RegExp(name, 'i') })),
  };
}

describe('SideNav', () => {
  it('renders navigation with items', () => {
    const harness = setupSideNavHarness();

    expect(harness.getNav()).toBeInTheDocument();
    expect(harness.getItems()).toHaveLength(3);
  });

  it('marks the active item with aria-current', () => {
    const harness = setupSideNavHarness();

    expect(harness.getActiveItem()).toHaveTextContent('APIs');
  });

  it('renders header content', () => {
    const harness = setupSideNavHarness();

    expect(harness.getHeader()).toBeInTheDocument();
  });

  it('calls onClick when an item is clicked', async () => {
    const onClick = vi.fn();
    const items = [{ label: 'Dashboard', onClick }];
    const harness = setupSideNavHarness({ items });

    await harness.clickItem('Dashboard');

    expect(onClick).toHaveBeenCalledOnce();
  });

  it('hides labels when collapsed', () => {
    setupSideNavHarness({ collapsed: true });

    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument();
    expect(screen.queryByText('APIs')).not.toBeInTheDocument();
  });
});
