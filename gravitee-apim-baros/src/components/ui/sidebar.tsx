import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ComponentPropsWithRef,
  type CSSProperties,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import { PanelLeft } from 'lucide-react';
import { cn } from '@baros/lib/utils';
import { Button } from '@baros/components/ui/button';
import { Separator } from '@baros/components/ui/separator';
import { Sheet, SheetContent, SheetTitle } from '@baros/components/ui/sheet';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@baros/components/ui/tooltip';

const SIDEBAR_COOKIE_NAME = 'sidebar_state';
const SIDEBAR_COOKIE_MAX_AGE = 60 * 60 * 24 * 7;
const SIDEBAR_WIDTH = '16rem';
const SIDEBAR_WIDTH_MOBILE = '18rem';
const SIDEBAR_WIDTH_ICON = '3rem';
const SIDEBAR_KEYBOARD_SHORTCUT = 'b';

interface SidebarContextValue {
  state: 'expanded' | 'collapsed';
  open: boolean;
  setOpen: (open: boolean) => void;
  openMobile: boolean;
  setOpenMobile: (open: boolean) => void;
  isMobile: boolean;
  toggleSidebar: () => void;
}

const SidebarContext = createContext<SidebarContextValue | null>(null);

function useSidebar() {
  const context = useContext(SidebarContext);
  if (!context) {
    throw new Error('useSidebar must be used within a SidebarProvider.');
  }
  return context;
}

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const mql = window.matchMedia('(max-width: 767px)');
    const onChange = () => setIsMobile(mql.matches);
    mql.addEventListener('change', onChange);
    setIsMobile(mql.matches);
    return () => mql.removeEventListener('change', onChange);
  }, []);

  return isMobile;
}

interface SidebarProviderProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  readonly className?: string;
  readonly defaultOpen?: boolean;
  readonly open?: boolean;
  readonly onOpenChange?: (open: boolean) => void;
}

const SidebarProvider = forwardRef<HTMLDivElement, SidebarProviderProps>(
  ({ defaultOpen = true, open: openProp, onOpenChange: setOpenProp, className, style, children, ...props }, ref) => {
    const isMobile = useIsMobile();
    const [openMobile, setOpenMobile] = useState(false);
    const [_open, _setOpen] = useState(defaultOpen);
    const open = openProp ?? _open;

    const setOpen = useCallback(
      (value: boolean | ((value: boolean) => boolean)) => {
        const openState = typeof value === 'function' ? value(open) : value;
        if (setOpenProp) {
          setOpenProp(openState);
        } else {
          _setOpen(openState);
        }
        document.cookie = `${SIDEBAR_COOKIE_NAME}=${openState}; path=/; max-age=${SIDEBAR_COOKIE_MAX_AGE}`;
      },
      [setOpenProp, open],
    );

    const toggleSidebar = useCallback(() => {
      if (isMobile) {
        setOpenMobile(prev => !prev);
      } else {
        setOpen(prev => !prev);
      }
    }, [isMobile, setOpen]);

    useEffect(() => {
      const handleKeyDown = (event: KeyboardEvent) => {
        if (event.key === SIDEBAR_KEYBOARD_SHORTCUT && (event.metaKey || event.ctrlKey)) {
          event.preventDefault();
          toggleSidebar();
        }
      };
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }, [toggleSidebar]);

    const state = open ? 'expanded' : 'collapsed';

    const contextValue = useMemo<SidebarContextValue>(
      () => ({ state, open, setOpen, isMobile, openMobile, setOpenMobile, toggleSidebar }),
      [state, open, setOpen, isMobile, openMobile, setOpenMobile, toggleSidebar],
    );

    return (
      <SidebarContext.Provider value={contextValue}>
        <TooltipProvider delayDuration={0}>
          <div
            ref={ref}
            data-slot="sidebar-wrapper"
            style={
              {
                '--sidebar-width': SIDEBAR_WIDTH,
                '--sidebar-width-icon': SIDEBAR_WIDTH_ICON,
                ...style,
              } as CSSProperties
            }
            className={cn(
              'group/sidebar-wrapper flex min-h-svh w-full has-data-[variant=inset]:bg-sidebar',
              className,
            )}
            {...props}
          >
            {children}
          </div>
        </TooltipProvider>
      </SidebarContext.Provider>
    );
  },
);

SidebarProvider.displayName = 'SidebarProvider';

interface SidebarProps extends Omit<ComponentPropsWithRef<'div'>, 'className'> {
  readonly className?: string;
  readonly side?: 'left' | 'right';
  readonly variant?: 'sidebar' | 'floating' | 'inset';
  readonly collapsible?: 'offcanvas' | 'icon' | 'none';
}

const Sidebar = forwardRef<HTMLDivElement, SidebarProps>(
  ({ side = 'left', variant = 'sidebar', collapsible = 'offcanvas', className, children, ...props }, ref) => {
    const { isMobile, state, openMobile, setOpenMobile } = useSidebar();

    if (collapsible === 'none') {
      return (
        <div
          ref={ref}
          className={cn('flex h-full w-(--sidebar-width) flex-col bg-sidebar text-sidebar-foreground', className)}
          {...props}
        >
          {children}
        </div>
      );
    }

    if (isMobile) {
      return (
        <Sheet open={openMobile} onOpenChange={setOpenMobile} {...props}>
          <SheetContent
            data-sidebar="sidebar"
            data-mobile="true"
            className="w-(--sidebar-width) bg-sidebar p-0 text-sidebar-foreground [&>button]:hidden"
            style={{ '--sidebar-width': SIDEBAR_WIDTH_MOBILE } as CSSProperties}
            side={side}
          >
            <SheetTitle className="sr-only">Sidebar</SheetTitle>
            <div className="flex h-full w-full flex-col">{children}</div>
          </SheetContent>
        </Sheet>
      );
    }

    return (
      <div
        ref={ref}
        className="group peer hidden md:block text-sidebar-foreground"
        data-state={state}
        data-collapsible={state === 'collapsed' ? collapsible : ''}
        data-variant={variant}
        data-side={side}
      >
        <div
          className={cn(
            'relative h-svh w-(--sidebar-width) bg-transparent transition-[width] duration-200 ease-linear',
            'group-data-[collapsible=offcanvas]:w-0',
            'group-data-[side=right]:rotate-180',
            variant === 'floating' || variant === 'inset'
              ? 'group-data-[collapsible=icon]:w-[calc(var(--sidebar-width-icon)_+_theme(spacing.4))]'
              : 'group-data-[collapsible=icon]:w-(--sidebar-width-icon)',
          )}
        />
        <div
          className={cn(
            'fixed inset-y-0 z-10 hidden h-svh w-(--sidebar-width) transition-[left,right,width] duration-200 ease-linear md:flex',
            side === 'left'
              ? 'left-0 group-data-[collapsible=offcanvas]:left-[calc(var(--sidebar-width)*-1)]'
              : 'right-0 group-data-[collapsible=offcanvas]:right-[calc(var(--sidebar-width)*-1)]',
            variant === 'floating' || variant === 'inset'
              ? 'p-2 group-data-[collapsible=icon]:w-[calc(var(--sidebar-width-icon)_+_theme(spacing.4)_+2px)]'
              : 'group-data-[collapsible=icon]:w-(--sidebar-width-icon) group-data-[side=left]:border-r group-data-[side=right]:border-l',
            className,
          )}
          {...props}
        >
          <div
            data-sidebar="sidebar"
            className={cn(
              'flex h-full w-full flex-col bg-sidebar group-data-[variant=floating]:rounded-lg group-data-[variant=floating]:border group-data-[variant=floating]:shadow',
            )}
          >
            {children}
          </div>
        </div>
      </div>
    );
  },
);

Sidebar.displayName = 'Sidebar';

const SidebarTrigger = forwardRef<HTMLButtonElement, ComponentPropsWithRef<typeof Button>>(
  ({ className, onClick, ...props }, ref) => {
    const { toggleSidebar } = useSidebar();
    return (
      <Button
        ref={ref}
        data-sidebar="trigger"
        variant="ghost"
        size="icon"
        className={cn('h-7 w-7', className)}
        onClick={(event: MouseEvent<HTMLButtonElement>) => {
          onClick?.(event);
          toggleSidebar();
        }}
        {...props}
      >
        <PanelLeft />
        <span className="sr-only">Toggle Sidebar</span>
      </Button>
    );
  },
);

SidebarTrigger.displayName = 'SidebarTrigger';

const SidebarRail = forwardRef<HTMLButtonElement, ComponentPropsWithRef<'button'>>(
  ({ className, ...props }, ref) => {
    const { toggleSidebar } = useSidebar();
    return (
      <button
        ref={ref}
        data-sidebar="rail"
        aria-label="Toggle Sidebar"
        tabIndex={-1}
        onClick={toggleSidebar}
        title="Toggle Sidebar"
        type="button"
        className={cn(
          'hover:after:bg-sidebar-border absolute inset-y-0 z-20 hidden w-4 -translate-x-1/2 transition-all ease-linear group-data-[side=left]:-right-4 group-data-[side=right]:left-0 after:absolute after:inset-y-0 after:left-1/2 after:w-[2px] sm:flex',
          '[[data-side=left]_&]:cursor-w-resize [[data-side=right]_&]:cursor-e-resize',
          '[[data-side=left][data-state=collapsed]_&]:cursor-e-resize [[data-side=right][data-state=collapsed]_&]:cursor-w-resize',
          'group-data-[collapsible=offcanvas]:translate-x-0 group-data-[collapsible=offcanvas]:after:left-full group-data-[collapsible=offcanvas]:hover:bg-sidebar',
          '[[data-side=left][data-collapsible=offcanvas]_&]:-right-2',
          '[[data-side=right][data-collapsible=offcanvas]_&]:-left-2',
          className,
        )}
        {...props}
      />
    );
  },
);

SidebarRail.displayName = 'SidebarRail';

const SidebarInset = forwardRef<HTMLDivElement, ComponentPropsWithRef<'main'>>(({ className, ...props }, ref) => (
  <main
    ref={ref}
    data-slot="sidebar-inset"
    className={cn(
      'relative flex min-h-svh flex-1 flex-col bg-background',
      'peer-data-[variant=inset]:min-h-[calc(100svh-theme(spacing.4))] md:peer-data-[variant=inset]:m-2 md:peer-data-[state=collapsed]:peer-data-[variant=inset]:ml-2 md:peer-data-[variant=inset]:ml-0 md:peer-data-[variant=inset]:rounded-xl md:peer-data-[variant=inset]:shadow',
      className,
    )}
    {...props}
  />
));

SidebarInset.displayName = 'SidebarInset';

const SidebarInput = forwardRef<HTMLInputElement, ComponentPropsWithRef<'input'>>(({ className, ...props }, ref) => (
  <input
    ref={ref}
    data-sidebar="input"
    className={cn(
      'h-8 w-full bg-background shadow-none focus-visible:ring-2 focus-visible:ring-sidebar-ring rounded-md border border-sidebar-border px-2 text-sm',
      className,
    )}
    {...props}
  />
));

SidebarInput.displayName = 'SidebarInput';

const SidebarHeader = forwardRef<HTMLDivElement, ComponentPropsWithRef<'div'>>(({ className, ...props }, ref) => (
  <div ref={ref} data-sidebar="header" className={cn('flex flex-col gap-2 p-2', className)} {...props} />
));

SidebarHeader.displayName = 'SidebarHeader';

const SidebarFooter = forwardRef<HTMLDivElement, ComponentPropsWithRef<'div'>>(({ className, ...props }, ref) => (
  <div ref={ref} data-sidebar="footer" className={cn('flex flex-col gap-2 p-2', className)} {...props} />
));

SidebarFooter.displayName = 'SidebarFooter';

const SidebarSeparator = forwardRef<
  HTMLHRElement,
  ComponentPropsWithRef<typeof Separator>
>(({ className, ...props }, ref) => (
  <Separator ref={ref} data-sidebar="separator" className={cn('mx-2 w-auto bg-sidebar-border', className)} {...props} />
));

SidebarSeparator.displayName = 'SidebarSeparator';

const SidebarContent = forwardRef<HTMLDivElement, ComponentPropsWithRef<'div'>>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    data-sidebar="content"
    className={cn(
      'flex min-h-0 flex-1 flex-col gap-2 overflow-auto group-data-[collapsible=icon]:overflow-hidden',
      className,
    )}
    {...props}
  />
));

SidebarContent.displayName = 'SidebarContent';

const SidebarGroup = forwardRef<HTMLDivElement, ComponentPropsWithRef<'div'>>(({ className, ...props }, ref) => (
  <div ref={ref} data-sidebar="group" className={cn('relative flex w-full min-w-0 flex-col p-2', className)} {...props} />
));

SidebarGroup.displayName = 'SidebarGroup';

const SidebarGroupLabel = forwardRef<HTMLDivElement, ComponentPropsWithRef<'div'> & { readonly asChild?: boolean }>(
  ({ className, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'div';
    return (
      <Comp
        ref={ref}
        data-sidebar="group-label"
        className={cn(
          'flex h-8 shrink-0 items-center rounded-md px-2 text-xs font-medium text-sidebar-foreground/70 outline-none ring-sidebar-ring transition-[margin,opa] duration-200 ease-linear focus-visible:ring-2 [&>svg]:size-4 [&>svg]:shrink-0',
          'group-data-[collapsible=icon]:-mt-8 group-data-[collapsible=icon]:opacity-0',
          className,
        )}
        {...props}
      />
    );
  },
);

SidebarGroupLabel.displayName = 'SidebarGroupLabel';

const SidebarGroupAction = forwardRef<HTMLButtonElement, ComponentPropsWithRef<'button'> & { readonly asChild?: boolean }>(
  ({ className, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button';
    return (
      <Comp
        ref={ref}
        data-sidebar="group-action"
        className={cn(
          'absolute right-3 top-3.5 flex aspect-square w-5 items-center justify-center rounded-md p-0 text-sidebar-foreground outline-none ring-sidebar-ring transition-transform hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 [&>svg]:size-4 [&>svg]:shrink-0',
          'after:absolute after:-inset-2 after:md:hidden',
          'group-data-[collapsible=icon]:hidden',
          className,
        )}
        {...props}
      />
    );
  },
);

SidebarGroupAction.displayName = 'SidebarGroupAction';

const SidebarGroupContent = forwardRef<HTMLDivElement, ComponentPropsWithRef<'div'>>(
  ({ className, ...props }, ref) => (
    <div ref={ref} data-sidebar="group-content" className={cn('w-full text-sm', className)} {...props} />
  ),
);

SidebarGroupContent.displayName = 'SidebarGroupContent';

const SidebarMenu = forwardRef<HTMLUListElement, ComponentPropsWithRef<'ul'>>(({ className, ...props }, ref) => (
  <ul ref={ref} data-sidebar="menu" className={cn('flex w-full min-w-0 flex-col gap-1', className)} {...props} />
));

SidebarMenu.displayName = 'SidebarMenu';

const SidebarMenuItem = forwardRef<HTMLLIElement, ComponentPropsWithRef<'li'>>(({ className, ...props }, ref) => (
  <li ref={ref} data-sidebar="menu-item" className={cn('group/menu-item relative', className)} {...props} />
));

SidebarMenuItem.displayName = 'SidebarMenuItem';

const sidebarMenuButtonVariants = cva(
  'peer/menu-button flex w-full items-center gap-2 overflow-hidden rounded-md p-2 text-left text-sm outline-none ring-sidebar-ring transition-[width,height,padding] hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 active:bg-sidebar-accent active:text-sidebar-accent-foreground disabled:pointer-events-none disabled:opacity-50 group-has-data-[sidebar=menu-action]/menu-item:pr-8 aria-disabled:pointer-events-none aria-disabled:opacity-50 data-[active=true]:bg-sidebar-accent data-[active=true]:font-medium data-[active=true]:text-sidebar-accent-foreground data-[state=open]:hover:bg-sidebar-accent data-[state=open]:hover:text-sidebar-accent-foreground group-data-[collapsible=icon]:!size-8 group-data-[collapsible=icon]:!p-2 [&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0',
  {
    variants: {
      variant: {
        default: 'hover:bg-sidebar-accent hover:text-sidebar-accent-foreground',
        outline:
          'bg-background shadow-[0_0_0_1px_hsl(var(--sidebar-border))] hover:bg-sidebar-accent hover:text-sidebar-accent-foreground hover:shadow-[0_0_0_1px_hsl(var(--sidebar-accent))]',
      },
      size: {
        default: 'h-8 text-sm',
        sm: 'h-7 text-xs',
        lg: 'h-12 text-sm group-data-[collapsible=icon]:!p-0',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  },
);

interface SidebarMenuButtonProps
  extends Omit<ComponentPropsWithRef<'button'>, 'className'>,
    VariantProps<typeof sidebarMenuButtonVariants> {
  readonly className?: string;
  readonly asChild?: boolean;
  readonly isActive?: boolean;
  readonly tooltip?: string | ReactNode;
}

const SidebarMenuButton = forwardRef<HTMLButtonElement, SidebarMenuButtonProps>(
  ({ asChild = false, isActive = false, variant = 'default', size = 'default', tooltip, className, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button';
    const { isMobile, state } = useSidebar();

    const button = (
      <Comp
        ref={ref}
        data-sidebar="menu-button"
        data-size={size}
        data-active={isActive}
        className={cn(sidebarMenuButtonVariants({ variant, size }), className)}
        {...props}
      />
    );

    if (!tooltip) return button;

    const tooltipContent = typeof tooltip === 'string' ? tooltip : undefined;

    return (
      <Tooltip>
        <TooltipTrigger asChild>{button}</TooltipTrigger>
        <TooltipContent
          side="right"
          align="center"
          hidden={state !== 'collapsed' || isMobile}
        >
          {tooltipContent ?? tooltip}
        </TooltipContent>
      </Tooltip>
    );
  },
);

SidebarMenuButton.displayName = 'SidebarMenuButton';

const SidebarMenuAction = forwardRef<
  HTMLButtonElement,
  ComponentPropsWithRef<'button'> & { readonly asChild?: boolean; readonly showOnHover?: boolean }
>(({ className, asChild = false, showOnHover = false, ...props }, ref) => {
  const Comp = asChild ? Slot : 'button';
  return (
    <Comp
      ref={ref}
      data-sidebar="menu-action"
      className={cn(
        'absolute right-1 top-1.5 flex aspect-square w-5 items-center justify-center rounded-md p-0 text-sidebar-foreground outline-none ring-sidebar-ring transition-transform hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 peer-hover/menu-button:text-sidebar-accent-foreground [&>svg]:size-4 [&>svg]:shrink-0',
        'after:absolute after:-inset-2 after:md:hidden',
        'group-data-[collapsible=icon]:hidden',
        showOnHover &&
          'group-focus-within/menu-item:opacity-100 group-hover/menu-item:opacity-100 data-[state=open]:opacity-100 peer-data-[active=true]/menu-button:text-sidebar-accent-foreground md:opacity-0',
        className,
      )}
      {...props}
    />
  );
});

SidebarMenuAction.displayName = 'SidebarMenuAction';

const SidebarMenuBadge = forwardRef<HTMLDivElement, ComponentPropsWithRef<'div'>>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    data-sidebar="menu-badge"
    className={cn(
      'pointer-events-none absolute right-1 flex h-5 min-w-5 select-none items-center justify-center rounded-md px-1 text-xs font-medium tabular-nums text-sidebar-foreground',
      'peer-hover/menu-button:text-sidebar-accent-foreground peer-data-[active=true]/menu-button:text-sidebar-accent-foreground',
      'group-data-[collapsible=icon]:hidden',
      className,
    )}
    {...props}
  />
));

SidebarMenuBadge.displayName = 'SidebarMenuBadge';

const SidebarMenuSkeleton = forwardRef<HTMLDivElement, ComponentPropsWithRef<'div'> & { readonly showIcon?: boolean }>(
  ({ className, showIcon = false, ...props }, ref) => {
    const width = useMemo(() => `${Math.floor(Math.random() * 40) + 50}%`, []);
    return (
      <div
        ref={ref}
        data-sidebar="menu-skeleton"
        className={cn('flex h-8 items-center gap-2 rounded-md px-2', className)}
        {...props}
      >
        {showIcon && <div className="flex size-4 animate-pulse rounded-md bg-sidebar-accent" />}
        <div className="h-4 max-w-(--skeleton-width) flex-1 animate-pulse rounded-md bg-sidebar-accent" style={{ '--skeleton-width': width } as CSSProperties} />
      </div>
    );
  },
);

SidebarMenuSkeleton.displayName = 'SidebarMenuSkeleton';

const SidebarMenuSub = forwardRef<HTMLUListElement, ComponentPropsWithRef<'ul'>>(({ className, ...props }, ref) => (
  <ul
    ref={ref}
    data-sidebar="menu-sub"
    className={cn(
      'mx-3.5 flex min-w-0 translate-x-px flex-col gap-1 border-l border-sidebar-border px-2.5 py-0.5',
      'group-data-[collapsible=icon]:hidden',
      className,
    )}
    {...props}
  />
));

SidebarMenuSub.displayName = 'SidebarMenuSub';

const SidebarMenuSubItem = forwardRef<HTMLLIElement, ComponentPropsWithRef<'li'>>(({ ...props }, ref) => (
  <li ref={ref} {...props} />
));

SidebarMenuSubItem.displayName = 'SidebarMenuSubItem';

const SidebarMenuSubButton = forwardRef<
  HTMLAnchorElement,
  ComponentPropsWithRef<'a'> & { readonly asChild?: boolean; readonly size?: 'sm' | 'md'; readonly isActive?: boolean }
>(({ asChild = false, size = 'md', isActive, className, ...props }, ref) => {
  const Comp = asChild ? Slot : 'a';
  return (
    <Comp
      ref={ref}
      data-sidebar="menu-sub-button"
      data-size={size}
      data-active={isActive}
      className={cn(
        'flex h-7 min-w-0 -translate-x-px items-center gap-2 overflow-hidden rounded-md px-2 text-sidebar-foreground outline-none ring-sidebar-ring hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-2 active:bg-sidebar-accent active:text-sidebar-accent-foreground disabled:pointer-events-none disabled:opacity-50 aria-disabled:pointer-events-none aria-disabled:opacity-50 [&>span:last-child]:truncate [&>svg]:size-4 [&>svg]:shrink-0',
        'data-[active=true]:bg-sidebar-accent data-[active=true]:text-sidebar-accent-foreground',
        size === 'sm' && 'text-xs',
        size === 'md' && 'text-sm',
        'group-data-[collapsible=icon]:hidden',
        className,
      )}
      {...props}
    />
  );
});

SidebarMenuSubButton.displayName = 'SidebarMenuSubButton';

export {
  Sidebar,
  SidebarContent,
  SidebarContext,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupAction,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInput,
  SidebarInset,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSkeleton,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarProvider,
  SidebarRail,
  SidebarSeparator,
  SidebarTrigger,
  useSidebar,
};
