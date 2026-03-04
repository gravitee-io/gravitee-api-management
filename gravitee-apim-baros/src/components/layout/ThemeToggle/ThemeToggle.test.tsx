import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeToggle } from './ThemeToggle';

function setupThemeToggleHarness() {
  const user = userEvent.setup();
  render(<ThemeToggle />);

  return {
    getButton: () => screen.getByRole('button'),
    click: () => user.click(screen.getByRole('button')),
  };
}

describe('ThemeToggle', () => {
  beforeEach(() => {
    document.documentElement.classList.remove('dark');
  });

  it('renders a toggle button', () => {
    const harness = setupThemeToggleHarness();

    expect(harness.getButton()).toBeInTheDocument();
    expect(harness.getButton()).toHaveAccessibleName('Switch to dark mode');
  });

  it('toggles to dark mode on click', async () => {
    const harness = setupThemeToggleHarness();

    await harness.click();

    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(harness.getButton()).toHaveAccessibleName('Switch to light mode');
  });

  it('toggles back to light mode on second click', async () => {
    const harness = setupThemeToggleHarness();

    await harness.click();
    await harness.click();

    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(harness.getButton()).toHaveAccessibleName('Switch to dark mode');
  });
});
