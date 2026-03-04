import { forwardRef, type ComponentPropsWithRef, type ElementType, type ReactNode } from 'react';
import { ChevronRight, ChevronsUpDown } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import { Avatar, AvatarFallback, AvatarImage } from '@baros/components/ui/avatar';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@baros/components/ui/collapsible';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@baros/components/ui/dropdown-menu';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
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

interface UserInfo {
  readonly name: string;
  readonly email: string;
  readonly avatar?: string;
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
  /** User information shown in the sidebar footer. */
  readonly user?: UserInfo;
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

          return (
            <Collapsible
              key={item.key}
              asChild
              defaultOpen={isParentActive}
              className="group/collapsible"
            >
              <SidebarMenuItem>
                <CollapsibleTrigger asChild>
                  <SidebarMenuButton tooltip={item.title} isActive={isParentActive}>
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
/*  NavUser                                             */
/* ──────────────────────────────────────────────────── */

function NavUser({ user }: { readonly user: UserInfo }) {
  const { isMobile } = useSidebar();
  const initials = user.name
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size="lg"
              className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
            >
              <Avatar className="h-8 w-8 rounded-lg">
                {user.avatar && <AvatarImage src={user.avatar} alt={user.name} />}
                <AvatarFallback className="rounded-lg">{initials}</AvatarFallback>
              </Avatar>
              <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-medium">{user.name}</span>
                <span className="truncate text-xs">{user.email}</span>
              </div>
              <ChevronsUpDown className="ml-auto size-4" />
            </SidebarMenuButton>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
            side={isMobile ? 'bottom' : 'right'}
            align="end"
            sideOffset={4}
          >
            <DropdownMenuGroup>
              <DropdownMenuLabel className="p-0 font-normal">
                <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                  <Avatar className="h-8 w-8 rounded-lg">
                    {user.avatar && <AvatarImage src={user.avatar} alt={user.name} />}
                    <AvatarFallback className="rounded-lg">{initials}</AvatarFallback>
                  </Avatar>
                  <div className="grid flex-1 text-left text-sm leading-tight">
                    <span className="truncate font-medium">{user.name}</span>
                    <span className="truncate text-xs">{user.email}</span>
                  </div>
                </div>
              </DropdownMenuLabel>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem>Account</DropdownMenuItem>
              <DropdownMenuItem>Settings</DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem>Log out</DropdownMenuItem>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
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
      user,
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

      {user && (
        <SidebarFooter>
          <NavUser user={user} />
        </SidebarFooter>
      )}

      <SidebarRail />
    </Sidebar>
  ),
);

AppSidebar.displayName = 'AppSidebar';

export { AppSidebar };
export type { AppSidebarProps, NavItem, NavSubItem, UserInfo };
