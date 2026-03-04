import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { PageLayoutProps } from './PageLayout';
import { PageLayout } from './PageLayout';

const defaultTabs = [
  { key: 'activity', label: 'Activity', href: '/apis/1/activity' },
  { key: 'config', label: 'Configuration', href: '/apis/1/config' },
  { key: 'consumers', label: 'Consumers', href: '/apis/1/consumers' },
];

function setupPageLayoutHarness(props?: Partial<PageLayoutProps>) {
  const user = userEvent.setup();
  render(
    <PageLayout title="My API" description="A test API." {...props}>
      <div data-testid="content">Page content</div>
    </PageLayout>,
  );

  return {
    user,
    getHeading: () => screen.getByRole('heading', { level: 1 }),
    getDescription: () => screen.getByText('A test API.'),
    getTabNav: () => screen.queryByRole('navigation', { name: /page sections/i }),
    getSeparator: () => document.querySelector('[data-orientation="horizontal"]'),
    getContent: () => screen.getByTestId('content'),
    getTab: (name: string) => screen.getByRole('link', { name }),
  };
}

describe('PageLayout', () => {
  it('renders title, description, and content', () => {
    const harness = setupPageLayoutHarness();

    expect(harness.getHeading()).toHaveTextContent('My API');
    expect(harness.getDescription()).toBeInTheDocument();
    expect(harness.getContent()).toHaveTextContent('Page content');
  });

  it('renders a separator when no tabs are provided', () => {
    const harness = setupPageLayoutHarness();

    expect(harness.getSeparator()).toBeInTheDocument();
    expect(harness.getTabNav()).not.toBeInTheDocument();
  });

  it('renders tabs instead of separator when tabs are provided', () => {
    const harness = setupPageLayoutHarness({ tabs: defaultTabs, activeTab: 'activity' });

    expect(harness.getTabNav()).toBeInTheDocument();
    expect(harness.getSeparator()).not.toBeInTheDocument();
    expect(harness.getTab('Activity')).toBeInTheDocument();
    expect(harness.getTab('Configuration')).toBeInTheDocument();
    expect(harness.getTab('Consumers')).toBeInTheDocument();
  });

  it('marks the active tab with aria-current', () => {
    const harness = setupPageLayoutHarness({ tabs: defaultTabs, activeTab: 'config' });

    expect(harness.getTab('Configuration')).toHaveAttribute('aria-current', 'page');
    expect(harness.getTab('Activity')).not.toHaveAttribute('aria-current');
  });

  it('calls onTabClick when a tab is clicked', async () => {
    const onTabClick = vi.fn();
    const harness = setupPageLayoutHarness({ tabs: defaultTabs, activeTab: 'activity', onTabClick });

    await harness.user.click(harness.getTab('Consumers'));

    expect(onTabClick).toHaveBeenCalledTimes(1);
    expect(onTabClick).toHaveBeenCalledWith(
      defaultTabs[2],
      expect.objectContaining({ type: 'click' }),
    );
  });

  it('renders the actions slot', () => {
    setupPageLayoutHarness({ actions: <button>Deploy</button> });

    expect(screen.getByRole('button', { name: 'Deploy' })).toBeInTheDocument();
  });

  it('renders without description', () => {
    render(
      <PageLayout title="Simple Page">
        <p>Content</p>
      </PageLayout>,
    );

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Simple Page');
    expect(screen.getByText('Content')).toBeInTheDocument();
  });
});
