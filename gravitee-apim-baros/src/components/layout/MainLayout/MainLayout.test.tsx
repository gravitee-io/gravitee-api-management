import { render, screen } from '@testing-library/react';
import type { MainLayoutProps } from './MainLayout';
import { MainLayout } from './MainLayout';

function setupMainLayoutHarness(props?: Partial<MainLayoutProps>) {
  render(
    <MainLayout
      sidebar={<aside data-testid="sidebar">Sidebar</aside>}
      topnav={<header data-testid="topnav">TopNav</header>}
      {...props}
    >
      <div data-testid="content">Page content</div>
    </MainLayout>,
  );

  return {
    getMain: () => screen.getByRole('main'),
    getSidebar: () => screen.getByTestId('sidebar'),
    getTopNav: () => screen.getByTestId('topnav'),
    getContent: () => screen.getByTestId('content'),
    getSubheader: () => screen.getByTestId('subheader'),
  };
}

describe('MainLayout', () => {
  it('renders sidebar, topnav, and main content', () => {
    const harness = setupMainLayoutHarness();

    expect(harness.getSidebar()).toBeInTheDocument();
    expect(harness.getTopNav()).toBeInTheDocument();
    expect(harness.getMain()).toBeInTheDocument();
    expect(harness.getContent()).toHaveTextContent('Page content');
  });

  it('renders subheader between topnav and content', () => {
    const harness = setupMainLayoutHarness({
      subheader: <div data-testid="subheader">Breadcrumb</div>,
    });

    expect(harness.getSubheader()).toHaveTextContent('Breadcrumb');
  });

  it('renders without sidebar', () => {
    render(
      <MainLayout topnav={<header>Nav</header>}>
        <p>Content only</p>
      </MainLayout>,
    );

    expect(screen.getByRole('main')).toBeInTheDocument();
    expect(screen.getByText('Content only')).toBeInTheDocument();
  });

  it('renders without topnav', () => {
    render(
      <MainLayout sidebar={<aside>Side</aside>}>
        <p>Content only</p>
      </MainLayout>,
    );

    expect(screen.getByRole('main')).toBeInTheDocument();
    expect(screen.getByText('Content only')).toBeInTheDocument();
  });
});
