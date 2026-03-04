import { forwardRef, type ComponentPropsWithRef, type ReactNode } from 'react';
import { cn } from '@baros/lib/utils';
import { Separator } from '@baros/components/ui/separator';

interface TopNavProps extends Omit<ComponentPropsWithRef<'header'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Left-aligned content (logo, app selector, etc.). */
  readonly leading?: ReactNode;
  /** Center content (environment selector, etc.). Grows to fill available space. */
  readonly center?: ReactNode;
  /** Right-aligned content (theme toggle, user menu, notifications, etc.). */
  readonly trailing?: ReactNode;
}

const TopNav = forwardRef<HTMLElement, TopNavProps>(({ className, leading, center, trailing, ...props }, ref) => (
  <header
    ref={ref}
    className={cn(
      'flex h-[var(--topnav-height)] shrink-0 items-center border-b border-topnav-border bg-topnav px-4 text-topnav-foreground',
      className,
    )}
    {...props}
  >
    {leading && <div className="flex items-center gap-2">{leading}</div>}
    {center && (
      <div className="flex flex-1 items-center justify-center gap-2">
        {center}
      </div>
    )}
    {!center && <div className="flex-1" />}
    {trailing && (
      <div className="flex items-center gap-2">
        <Separator orientation="vertical" className="mx-2 h-6" />
        {trailing}
      </div>
    )}
  </header>
));

TopNav.displayName = 'TopNav';

export { TopNav };
export type { TopNavProps };
