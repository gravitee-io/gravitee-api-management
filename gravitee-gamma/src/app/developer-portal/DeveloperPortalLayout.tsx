import * as React from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Home } from 'lucide-react';
import { AppSidebar } from '@baros/components/layout/AppSidebar';
import type { NavItem } from '@baros/components/layout/AppSidebar';
import { SidebarProvider, SidebarInset } from '@baros/components/ui/sidebar';

const NAV_ITEMS: NavItem[] = [
    { key: 'portal-homepage', title: 'Portal Homepage', url: '/developer-portal/homepage', icon: Home },
];

function useActiveItemKey(): string {
    const { pathname } = useLocation();
    if (pathname.includes('/developer-portal/homepage')) return 'portal-homepage';
    return 'portal-homepage';
}

export function DeveloperPortalLayout() {
    const navigate = useNavigate();
    const activeItemKey = useActiveItemKey();

    const handleNavItemClick = React.useCallback(
        (key: string) => {
            const item = NAV_ITEMS.find(i => i.key === key);
            if (item) navigate(item.url);
        },
        [navigate],
    );

    return (
        <SidebarProvider>
            <AppSidebar
                navItems={NAV_ITEMS}
                activeItemKey={activeItemKey}
                onNavItemClick={handleNavItemClick}
            />
            <SidebarInset className="flex flex-1 flex-col overflow-hidden">
                <main className="flex flex-1 flex-col overflow-hidden">
                    <Outlet />
                </main>
            </SidebarInset>
        </SidebarProvider>
    );
}
