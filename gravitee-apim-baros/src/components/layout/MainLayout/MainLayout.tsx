import { forwardRef, type ComponentPropsWithRef, type ReactNode } from 'react';
import { cn } from '@baros/lib/utils';
import { ScrollArea } from '@baros/components/ui/scroll-area';
import { SidebarInset, SidebarProvider } from '@baros/components/ui/sidebar';

interface MainLayoutProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Side navigation element (typically an <AppSidebar />). */
  readonly sidebar?: ReactNode;
  /** Top navigation element (typically a <TopNav />). Rendered full-width above the sidebar and content. */
  readonly topnav?: ReactNode;
  /** Sub-header rendered between the top nav and the main content (e.g. sidebar trigger + breadcrumb). */
  readonly subheader?: ReactNode;
  /** Main page content. */
  readonly children: ReactNode;
  /** Whether the sidebar starts in the open state. Defaults to true. */
  readonly defaultOpen?: boolean;
}

const MainLayout = forwardRef<HTMLDivElement, MainLayoutProps>(
  ({ className, sidebar, topnav, subheader, children, defaultOpen = true, ...props }, ref) => (
    <SidebarProvider defaultOpen={defaultOpen}>
      <div ref={ref} className={cn('flex min-h-svh w-full flex-col', className)} {...props}>
        {topnav}

        <div className="flex flex-1 overflow-hidden">
          {sidebar}

          <SidebarInset className="min-h-0">
            {subheader}

            <ScrollArea className="flex-1">
              <main className="mx-auto w-full max-w-[var(--content-max-width)] p-[var(--content-padding)]">
                {children}
              </main>
            </ScrollArea>
          </SidebarInset>
        </div>
      </div>
    </SidebarProvider>
  ),
);

MainLayout.displayName = 'MainLayout';

export { MainLayout };
export type { MainLayoutProps };
