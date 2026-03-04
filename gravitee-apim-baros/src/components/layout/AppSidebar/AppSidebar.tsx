import { forwardRef, type ComponentPropsWithRef, type ElementType, type ReactNode } from 'react';
import { ChevronsUpDown } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import { Avatar, AvatarFallback, AvatarImage } from '@baros/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuTrigger,
} from '@baros/components/ui/dropdown-menu';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  useSidebar,
} from '@baros/components/ui/sidebar';

/* ──────────────────────────────────────────────────── */
/*  Types                                               */
/* ──────────────────────────────────────────────────── */

interface Organization {
  /** Unique identifier for the organization. */
  readonly name: string;
  /** Icon component rendered next to the name. */
  readonly logo: ElementType;
  /** Subscription tier or label shown under the name. */
  readonly plan: string;
}

interface NavItem {
  /** Unique key for the item, used for active matching. */
  readonly key: string;
  /** Display label. */
  readonly title: string;
  /** URL to navigate to. */
  readonly url: string;
  /** Icon component rendered before the title. */
  readonly icon?: ElementType;
}

interface UserInfo {
  readonly name: string;
  readonly email: string;
  readonly avatar?: string;
}

interface AppSidebarProps extends Omit<ComponentPropsWithRef<typeof Sidebar>, 'className'> {
  /** Additional CSS classes. */
  readonly className?: string;
  /** Branding element rendered above the organization switcher. */
  readonly logo?: ReactNode;
  /** List of organizations available to switch between. */
  readonly organizations?: Organization[];
  /** Currently selected organization. */
  readonly activeOrganization?: Organization;
  /** Callback when an organization is selected. */
  readonly onOrganizationChange?: (org: Organization) => void;
  /** Main navigation items. */
  readonly navItems?: NavItem[];
  /** Key of the currently active nav item. */
  readonly activeItemKey?: string;
  /** Callback when a nav item is clicked. */
  readonly onNavItemClick?: (key: string) => void;
  /** User information shown in the sidebar footer. */
  readonly user?: UserInfo;
}

/* ──────────────────────────────────────────────────── */
/*  OrganizationSwitcher                                */
/* ──────────────────────────────────────────────────── */

function OrganizationSwitcher({
  organizations,
  activeOrganization,
  onOrganizationChange,
}: {
  readonly organizations: Organization[];
  readonly activeOrganization: Organization;
  readonly onOrganizationChange?: (org: Organization) => void;
}) {
  const { isMobile } = useSidebar();

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size="lg"
              className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
            >
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-sidebar-primary text-sidebar-primary-foreground">
                <activeOrganization.logo className="size-4" />
              </div>
              <div className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-medium">{activeOrganization.name}</span>
                <span className="truncate text-xs">{activeOrganization.plan}</span>
              </div>
              <ChevronsUpDown className="ml-auto" />
            </SidebarMenuButton>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
            align="start"
            side={isMobile ? 'bottom' : 'right'}
            sideOffset={4}
          >
            <DropdownMenuGroup>
              <DropdownMenuLabel className="text-xs text-muted-foreground">Organizations</DropdownMenuLabel>
              {organizations.map((org, index) => (
                <DropdownMenuItem key={org.name} onClick={() => onOrganizationChange?.(org)} className="gap-2 p-2">
                  <div className="flex size-6 items-center justify-center rounded-md border">
                    <org.logo className="size-3.5 shrink-0" />
                  </div>
                  {org.name}
                  <DropdownMenuShortcut>{'\u2318'}{index + 1}</DropdownMenuShortcut>
                </DropdownMenuItem>
              ))}
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
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
  return (
    <SidebarGroup>
      <SidebarGroupLabel>Platform</SidebarGroupLabel>
      <SidebarMenu>
        {items.map(item => (
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
        ))}
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
/*  AppSidebar                                          */
/* ──────────────────────────────────────────────────── */

const AppSidebar = forwardRef<HTMLDivElement, AppSidebarProps>(
  (
    {
      className,
      logo,
      organizations = [],
      activeOrganization,
      onOrganizationChange,
      navItems = [],
      activeItemKey,
      onNavItemClick,
      user,
      ...props
    },
    ref,
  ) => {
    const resolvedActiveOrg = activeOrganization ?? organizations[0];

    return (
      <Sidebar ref={ref} collapsible="icon" className={cn(className)} {...props}>
        <SidebarHeader>
          {logo && (
            <div className="flex items-center gap-2 px-2 py-1 group-data-[collapsible=icon]:justify-center">
              {logo}
            </div>
          )}
          {resolvedActiveOrg && organizations.length > 0 && (
            <OrganizationSwitcher
              organizations={organizations}
              activeOrganization={resolvedActiveOrg}
              onOrganizationChange={onOrganizationChange}
            />
          )}
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
    );
  },
);

AppSidebar.displayName = 'AppSidebar';

export { AppSidebar };
export type { AppSidebarProps, Organization, NavItem, UserInfo };
