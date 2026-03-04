import { forwardRef, useCallback, useRef, useState, type ComponentPropsWithRef, type ElementType, type ReactNode } from 'react';
import { ChevronRight } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@baros/components/ui/collapsible';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@baros/components/ui/dropdown-menu';
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarRail,
  useSidebar,
} from '@baros/components/ui/sidebar';

/* ──────────────────────────────────────────────────── */
/*  Types                                               */
/* ──────────────────────────────────────────────────── */

interface NavSubItem {
  /** Unique key for the sub-item, used for active matching. */
  readonly key: string;
  /** Display label. */
  readonly title: string;
  /** URL to navigate to. */
  readonly url: string;
}

interface NavItem {
  /** Unique key for the parent item. */
  readonly key: string;
  /** Display label. */
  readonly title: string;
  /** URL to navigate to (used when there are no sub-items). */
  readonly url: string;
  /** Icon component rendered before the title. */
  readonly icon?: ElementType;
  /** Child navigation items shown when the parent is expanded. */
  readonly items?: NavSubItem[];
}

interface AppSidebarProps extends Omit<ComponentPropsWithRef<typeof Sidebar>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Branding element shown when the sidebar is expanded. */
  readonly logo?: ReactNode;
  /** Branding element shown when the sidebar is collapsed to icon mode. */
  readonly collapsedLogo?: ReactNode;
  /** Main navigation items. */
  readonly navItems?: NavItem[];
  /** Key of the currently active sub-item (or parent item if no sub-items). */
  readonly activeItemKey?: string;
  /** Callback when a nav item or sub-item is clicked. */
  readonly onNavItemClick?: (key: string) => void;
}

/* ──────────────────────────────────────────────────── */
/*  CollapsedNavItem                                    */
/* ──────────────────────────────────────────────────── */

const FLYOUT_CLOSE_DELAY = 150;

function CollapsedNavItem({
  item,
  isParentActive,
  activeItemKey,
  onNavItemClick,
}: {
  readonly item: NavItem;
  readonly isParentActive: boolean;
  readonly activeItemKey?: string;
  readonly onNavItemClick?: (key: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const closeTimeout = useRef<ReturnType<typeof setTimeout>>(null);

  const scheduleOpen = useCallback(() => {
    if (closeTimeout.current) clearTimeout(closeTimeout.current);
    setOpen(true);
  }, []);

  const scheduleClose = useCallback(() => {
    closeTimeout.current = setTimeout(() => setOpen(false), FLYOUT_CLOSE_DELAY);
  }, []);

  return (
    <SidebarMenuItem onMouseEnter={scheduleOpen} onMouseLeave={scheduleClose}>
      <DropdownMenu open={open} onOpenChange={setOpen} modal={false}>
        <DropdownMenuTrigger asChild>
          <SidebarMenuButton isActive={isParentActive}>
            {item.icon && <item.icon />}
            <span>{item.title}</span>
          </SidebarMenuButton>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          side="right"
          align="start"
          sideOffset={4}
          onMouseEnter={scheduleOpen}
          onMouseLeave={scheduleClose}
          onCloseAutoFocus={e => e.preventDefault()}
        >
          <DropdownMenuLabel>{item.title}</DropdownMenuLabel>
          <DropdownMenuSeparator />
          {item.items!.map(sub => (
            <DropdownMenuItem
              key={sub.key}
              className={cn(sub.key === activeItemKey && 'font-medium bg-accent text-accent-foreground')}
              onSelect={() => onNavItemClick?.(sub.key)}
            >
              {sub.title}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </SidebarMenuItem>
  );
}

/* ──────────────────────────────────────────────────── */
/*  NavMain                                             */
/* ──────────────────────────────────────────────────── */

function NavMain({
  items,
  activeItemKey,
  onNavItemClick,
}: {
  readonly items: NavItem[];
  readonly activeItemKey?: string;
  readonly onNavItemClick?: (key: string) => void;
}) {
  const { state } = useSidebar();
  const isCollapsed = state === 'collapsed';

  return (
    <SidebarGroup>
      <SidebarMenu>
        {items.map(item => {
          const hasChildren = item.items && item.items.length > 0;
          const isParentActive = hasChildren && item.items!.some(sub => sub.key === activeItemKey);

          if (!hasChildren) {
            return (
              <SidebarMenuItem key={item.key}>
                <SidebarMenuButton
                  asChild
                  isActive={item.key === activeItemKey}
                  tooltip={item.title}
                >
                  <a href={item.url} onClick={e => { e.preventDefault(); onNavItemClick?.(item.key); }}>
                    {item.icon && <item.icon />}
                    <span>{item.title}</span>
                  </a>
                </SidebarMenuButton>
              </SidebarMenuItem>
            );
          }

          if (isCollapsed) {
            return (
              <CollapsedNavItem
                key={item.key}
                item={item}
                isParentActive={!!isParentActive}
                activeItemKey={activeItemKey}
                onNavItemClick={onNavItemClick}
              />
            );
          }

          return (
            <Collapsible
              key={item.key}
              asChild
              defaultOpen={isParentActive}
              className="group/collapsible"
            >
              <SidebarMenuItem>
                <CollapsibleTrigger asChild>
                  <SidebarMenuButton
                    tooltip={item.title}
                    className={cn(isParentActive && 'font-medium text-sidebar-primary')}
                  >
                    {item.icon && <item.icon />}
                    <span>{item.title}</span>
                    <ChevronRight className="ml-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90" />
                  </SidebarMenuButton>
                </CollapsibleTrigger>
                <CollapsibleContent>
                  <SidebarMenuSub>
                    {item.items!.map(sub => (
                      <SidebarMenuSubItem key={sub.key}>
                        <SidebarMenuSubButton
                          asChild
                          isActive={sub.key === activeItemKey}
                        >
                          <a href={sub.url} onClick={e => { e.preventDefault(); onNavItemClick?.(sub.key); }}>
                            <span>{sub.title}</span>
                          </a>
                        </SidebarMenuSubButton>
                      </SidebarMenuSubItem>
                    ))}
                  </SidebarMenuSub>
                </CollapsibleContent>
              </SidebarMenuItem>
            </Collapsible>
          );
        })}
      </SidebarMenu>
    </SidebarGroup>
  );
}

/* ──────────────────────────────────────────────────── */
/*  SidebarLogo                                         */
/* ──────────────────────────────────────────────────── */

function SidebarLogo({
  logo,
  collapsedLogo,
}: {
  readonly logo?: ReactNode;
  readonly collapsedLogo?: ReactNode;
}) {
  const { state } = useSidebar();

  if (!logo && !collapsedLogo) return null;

  return (
    <div 
    className={cn("flex items-center gap-2 px-2 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0", state === 'collapsed' ? 'py-0' : 'py-2')}>
      {state === 'collapsed' ? (collapsedLogo ?? logo) : logo}
    </div>
  );
}

/* ──────────────────────────────────────────────────── */
/*  AppSidebar                                          */
/* ──────────────────────────────────────────────────── */

const AppSidebar = forwardRef<HTMLDivElement, AppSidebarProps>(
  (
    {
      className,
      logo,
      collapsedLogo,
      navItems = [],
      activeItemKey,
      onNavItemClick,
      ...props
    },
    ref,
  ) => (
    <Sidebar ref={ref} collapsible="icon" className={cn(className)} {...props}>
      <SidebarHeader>
        <SidebarLogo logo={logo} collapsedLogo={collapsedLogo} />
      </SidebarHeader>

      <SidebarContent>
        {navItems.length > 0 && (
          <NavMain items={navItems} activeItemKey={activeItemKey} onNavItemClick={onNavItemClick} />
        )}
      </SidebarContent>

      <SidebarRail />
    </Sidebar>
  ),
);

AppSidebar.displayName = 'AppSidebar';

export { AppSidebar };
export type { AppSidebarProps, NavItem, NavSubItem };
