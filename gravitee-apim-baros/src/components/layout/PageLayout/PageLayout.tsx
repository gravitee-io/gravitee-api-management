import { forwardRef, type ComponentPropsWithRef, type ReactNode } from 'react';
import { cn } from '@baros/lib/utils';
import { Separator } from '@baros/components/ui/separator';

interface TabItem {
  /** Unique identifier used to match against `activeTab`. */
  readonly key: string;
  /** Display label for the tab. */
  readonly label: string;
  /** Navigation URL for the tab link. */
  readonly href: string;
}

interface PageLayoutProps extends Omit<ComponentPropsWithRef<'div'>, 'className' | 'title'> {
  /** Additional CSS classes to merge with component styles. */
  readonly className?: string;
  /** Page heading (rendered as h1). */
  readonly title: string;
  /** Optional subtitle displayed below the title. */
  readonly description?: string;
  /** Navigation tabs rendered below the header. When absent, a separator is shown instead. */
  readonly tabs?: TabItem[];
  /** Key of the currently active tab. */
  readonly activeTab?: string;
  /** Called when a tab link is clicked. Use `event.preventDefault()` for SPA navigation. */
  readonly onTabClick?: (tab: TabItem, event: React.MouseEvent<HTMLAnchorElement>) => void;
  /** Slot for page-level actions (buttons, menus) aligned to the right of the title. */
  readonly actions?: ReactNode;
  /** Main page content rendered below the tabs or separator. */
  readonly children?: ReactNode;
}

const PageLayout = forwardRef<HTMLDivElement, PageLayoutProps>(
  ({ className, title, description, tabs, activeTab, onTabClick, actions, children, ...props }, ref) => {
    const hasTabs = tabs != null && tabs.length > 0;

    return (
      <div ref={ref} className={cn('flex flex-col gap-4', className)} {...props}>
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold tracking-tight text-foreground">{title}</h1>
            {description && <p className="text-sm text-muted-foreground">{description}</p>}
          </div>
          {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
        </div>

        <div>
          {hasTabs ? (
            <nav aria-label="Page sections" className="flex gap-4 border-b border-border">
              {tabs.map((tab) => (
                <a
                  key={tab.key}
                  href={tab.href}
                  aria-current={tab.key === activeTab ? 'page' : undefined}
                  className={cn(
                    'inline-flex items-center border-b-2 px-1 pb-2 text-sm font-medium transition-colors',
                    tab.key === activeTab
                      ? 'border-primary text-foreground'
                      : 'border-transparent text-muted-foreground hover:border-border hover:text-foreground',
                  )}
                  onClick={(e) => onTabClick?.(tab, e)}
                >
                  {tab.label}
                </a>
              ))}
            </nav>
          ) : (
            <Separator />
          )}
        </div>

        {children && <div className="space-y-6">{children}</div>}
      </div>
    );
  },
);

PageLayout.displayName = 'PageLayout';

export { PageLayout };
export type { PageLayoutProps, TabItem };
