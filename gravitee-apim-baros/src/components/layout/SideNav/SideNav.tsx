import { forwardRef, type ComponentPropsWithRef, type ReactNode } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@baros/lib/utils';
import { ScrollArea } from '@baros/components/ui/scroll-area';
import { Separator } from '@baros/components/ui/separator';

const sideNavVariants = cva('flex h-full flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground', {
  variants: {
    collapsed: {
      true: 'w-[var(--sidebar-width-collapsed)]',
      false: 'w-[var(--sidebar-width)]',
    },
  },
  defaultVariants: { collapsed: false },
});

interface SideNavItemProps {
  /** Icon element rendered before the label. */
  readonly icon?: ReactNode;
  /** Navigation item label text. */
  readonly label: string;
  /** Whether this item is currently active. */
  readonly active?: boolean;
  /** Click handler. */
  readonly onClick?: () => void;
  /** Whether the sidebar is collapsed (icon-only mode). */
  readonly collapsed?: boolean;
}

function SideNavItem({ icon, label, active = false, onClick, collapsed = false }: SideNavItemProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'flex w-full items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
        'hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
        active && 'bg-sidebar-accent text-sidebar-accent-foreground',
        collapsed && 'justify-center px-2',
      )}
      title={collapsed ? label : undefined}
      aria-current={active ? 'page' : undefined}
    >
      {icon && <span className="flex h-5 w-5 shrink-0 items-center justify-center">{icon}</span>}
      {!collapsed && <span className="truncate">{label}</span>}
    </button>
  );
}

interface SideNavProps extends Omit<ComponentPropsWithRef<'aside'>, 'className'>, VariantProps<typeof sideNavVariants> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Brand or logo element rendered at the top. */
  readonly header?: ReactNode;
  /** Footer content rendered at the bottom. */
  readonly footer?: ReactNode;
  /** Navigation items to render. */
  readonly items?: SideNavItemProps[];
}

const SideNav = forwardRef<HTMLElement, SideNavProps>(
  ({ className, collapsed = false, header, footer, items = [], ...props }, ref) => (
    <aside ref={ref} className={cn(sideNavVariants({ collapsed }), className)} {...props}>
      {header && (
        <>
          <div className={cn('flex h-[var(--topnav-height)] shrink-0 items-center px-4', collapsed && 'justify-center px-2')}>
            {header}
          </div>
          <Separator />
        </>
      )}

      <ScrollArea className="flex-1">
        <nav aria-label="Main navigation" className="flex flex-col gap-1 p-2">
          {items.map(item => (
            <SideNavItem key={item.label} {...item} collapsed={collapsed ?? false} />
          ))}
        </nav>
      </ScrollArea>

      {footer && (
        <>
          <Separator />
          <div className={cn('shrink-0 p-2', collapsed && 'flex justify-center')}>{footer}</div>
        </>
      )}
    </aside>
  ),
);

SideNav.displayName = 'SideNav';

export { SideNav, SideNavItem };
export type { SideNavProps, SideNavItemProps };
