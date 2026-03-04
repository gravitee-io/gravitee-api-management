// ── Utils ────────────────────────────────────────────
export { cn } from './lib/utils';

// ── UI: Button ───────────────────────────────────────
export { Button, buttonVariants } from './components/ui/button';
export type { ButtonProps } from './components/ui/button';

// ── UI: Separator ────────────────────────────────────
export { Separator } from './components/ui/separator';

// ── UI: ScrollArea ───────────────────────────────────
export { ScrollArea, ScrollBar } from './components/ui/scroll-area';

// ── UI: Sheet ────────────────────────────────────────
export {
  Sheet,
  SheetPortal,
  SheetOverlay,
  SheetTrigger,
  SheetClose,
  SheetContent,
  SheetHeader,
  SheetFooter,
  SheetTitle,
  SheetDescription,
} from './components/ui/sheet';

// ── UI: Tooltip ──────────────────────────────────────
export { Tooltip, TooltipTrigger, TooltipContent, TooltipProvider } from './components/ui/tooltip';

// ── UI: Avatar ───────────────────────────────────────
export { Avatar, AvatarImage, AvatarFallback } from './components/ui/avatar';

// ── UI: Collapsible ──────────────────────────────────
export { Collapsible, CollapsibleTrigger, CollapsibleContent } from './components/ui/collapsible';

// ── UI: DropdownMenu ─────────────────────────────────
export {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuCheckboxItem,
  DropdownMenuRadioItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuGroup,
  DropdownMenuPortal,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuRadioGroup,
} from './components/ui/dropdown-menu';

// ── UI: Sidebar ──────────────────────────────────────
export {
  Sidebar,
  SidebarContent,
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
} from './components/ui/sidebar';

// ── Layout: TopNav ───────────────────────────────────
export { TopNav } from './components/layout/TopNav';
export type { TopNavProps } from './components/layout/TopNav';

// ── Layout: ThemeToggle ──────────────────────────────
export { ThemeToggle } from './components/layout/ThemeToggle';
export type { ThemeToggleProps } from './components/layout/ThemeToggle';

// ── Layout: AppSidebar ───────────────────────────────
export { AppSidebar } from './components/layout/AppSidebar';
export type { AppSidebarProps, Organization, NavItem, UserInfo } from './components/layout/AppSidebar';

// ── Layout: MainLayout ──────────────────────────────
export { MainLayout } from './components/layout/MainLayout';
export type { MainLayoutProps } from './components/layout/MainLayout';
