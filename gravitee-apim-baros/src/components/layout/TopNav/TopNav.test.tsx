import { render, screen } from '@testing-library/react';
import type { TopNavProps } from './TopNav';
import { TopNav } from './TopNav';

function setupTopNavHarness(props?: Partial<TopNavProps>) {
  render(<TopNav {...props} />);

  return {
    getHeader: () => screen.getByRole('banner'),
    queryLeading: (text: string) => screen.queryByText(text),
    queryCenter: (text: string) => screen.queryByText(text),
    queryTrailing: (text: string) => screen.queryByText(text),
  };
}

describe('TopNav', () => {
  it('renders as a header element', () => {
    const harness = setupTopNavHarness();

    expect(harness.getHeader()).toBeInTheDocument();
  });

  it('renders leading content', () => {
    const harness = setupTopNavHarness({ leading: <span>Breadcrumbs</span> });

    expect(harness.queryLeading('Breadcrumbs')).toBeInTheDocument();
  });

  it('renders center content', () => {
    const harness = setupTopNavHarness({ center: <span>Environment</span> });

    expect(harness.queryCenter('Environment')).toBeInTheDocument();
  });

  it('renders trailing content', () => {
    const harness = setupTopNavHarness({ trailing: <span>User Menu</span> });

    expect(harness.queryTrailing('User Menu')).toBeInTheDocument();
  });
});
