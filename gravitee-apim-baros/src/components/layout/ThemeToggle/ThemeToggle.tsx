import { forwardRef, useEffect, useState, type ComponentPropsWithRef } from 'react';
import { Moon, Sun } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import { Button } from '@baros/components/ui/button';

type Theme = 'light' | 'dark';

interface ThemeToggleProps extends Omit<ComponentPropsWithRef<typeof Button>, 'children' | 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
}

const ThemeToggle = forwardRef<HTMLButtonElement, ThemeToggleProps>(({ className, onClick, ...props }, ref) => {
  const [theme, setTheme] = useState<Theme>(() =>
    typeof document !== 'undefined' && document.documentElement.classList.contains('dark') ? 'dark' : 'light',
  );

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
  }, [theme]);

  return (
    <Button
      ref={ref}
      variant="ghost"
      size="icon"
      aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
      className={cn('h-8 w-8', className)}
      onClick={event => {
        setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
        onClick?.(event);
      }}
      {...props}
    >
      {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
    </Button>
  );
});

ThemeToggle.displayName = 'ThemeToggle';

export { ThemeToggle };
export type { ThemeToggleProps };
