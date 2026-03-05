import { LayoutDashboard, Globe, Settings } from 'lucide-react';
import { AppSidebar } from '@baros/components/layout/AppSidebar';
import type { NavItem } from '@baros/components/layout/AppSidebar';
import { SidebarProvider, SidebarInset, SidebarTrigger } from '@baros/components/ui/sidebar';
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator,
} from '@baros/components/ui/breadcrumb';
import { Separator } from '@baros/components/ui/separator';
import '../styles.css';

const NAV_ITEMS: NavItem[] = [
    { key: 'dashboard', title: 'Dashboard', url: '/app-alpha', icon: LayoutDashboard },
    { key: 'apis', title: 'APIs', url: '/app-alpha/apis', icon: Globe },
    { key: 'settings', title: 'Settings', url: '/app-alpha/settings', icon: Settings },
];

export function App() {
    return (
        <SidebarProvider>
            <AppSidebar navItems={NAV_ITEMS} activeItemKey="dashboard" />
            <SidebarInset>
                <header className="flex h-12 items-center gap-2 border-b px-4">
                    <SidebarTrigger />
                    <Separator orientation="vertical" className="h-4" />
                    <Breadcrumb>
                        <BreadcrumbList>
                            <BreadcrumbItem>
                                <BreadcrumbLink href="/app-alpha">Home</BreadcrumbLink>
                            </BreadcrumbItem>
                            <BreadcrumbSeparator />
                            <BreadcrumbItem>
                                <BreadcrumbPage>Dashboard</BreadcrumbPage>
                            </BreadcrumbItem>
                        </BreadcrumbList>
                    </Breadcrumb>
                </header>
                <main className="p-6">
                    <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
                    <p className="mt-2 text-muted-foreground">Welcome to App Alpha.</p>
                </main>
            </SidebarInset>
        </SidebarProvider>
    );
}

export default App;
