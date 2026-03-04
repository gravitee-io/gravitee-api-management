import { forwardRef, type ComponentPropsWithRef, type ReactNode } from 'react';
import { cn } from '@baros/lib/utils';
import { ScrollArea } from '@baros/components/ui/scroll-area';

interface MainLayoutProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Side navigation element (typically a <SideNav />). */
  readonly sidebar?: ReactNode;
  /** Top navigation element (typically a <TopNav />). */
  readonly topnav?: ReactNode;
  /** Main page content. */
  readonly children: ReactNode;
}

const MainLayout = forwardRef<HTMLDivElement, MainLayoutProps>(
  ({ className, sidebar, topnav, children, ...props }, ref) => (
    <div ref={ref} className={cn('flex h-screen overflow-hidden bg-background', className)} {...props}>
      {sidebar}

      <div className="flex flex-1 flex-col overflow-hidden">
        {topnav}

        <ScrollArea className="flex-1">
          <main className="mx-auto w-full max-w-[var(--content-max-width)] p-[var(--content-padding)]">
            {children}
          </main>
        </ScrollArea>
      </div>
    </div>
  ),
);

MainLayout.displayName = 'MainLayout';

export { MainLayout };
export type { MainLayoutProps };
