import { forwardRef, type ComponentPropsWithRef, type ReactNode } from 'react';
import { cn } from '@baros/lib/utils';
import { ScrollArea } from '@baros/components/ui/scroll-area';
import { SidebarInset, SidebarProvider } from '@baros/components/ui/sidebar';

interface MainLayoutProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Side navigation element (typically an <AppSidebar />). */
  readonly sidebar?: ReactNode;
  /** Top navigation element (typically a <TopNav />). */
  readonly topnav?: ReactNode;
  /** Main page content. */
  readonly children: ReactNode;
  /** Whether the sidebar starts in the open state. Defaults to true. */
  readonly defaultOpen?: boolean;
}

const MainLayout = forwardRef<HTMLDivElement, MainLayoutProps>(
  ({ className, sidebar, topnav, children, defaultOpen = true, ...props }, ref) => (
    <SidebarProvider defaultOpen={defaultOpen}>
      <div ref={ref} className={cn('flex min-h-svh w-full', className)} {...props}>
        {sidebar}

        <SidebarInset>
          {topnav}

          <ScrollArea className="flex-1">
            <main className="mx-auto w-full max-w-[var(--content-max-width)] p-[var(--content-padding)]">
              {children}
            </main>
          </ScrollArea>
        </SidebarInset>
      </div>
    </SidebarProvider>
  ),
);

MainLayout.displayName = 'MainLayout';

export { MainLayout };
export type { MainLayoutProps };
