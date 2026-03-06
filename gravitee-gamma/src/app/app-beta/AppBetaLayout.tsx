import * as React from 'react';
import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Cloud, Settings } from 'lucide-react';
import { AppSidebar } from '@baros/components/layout/AppSidebar';
import type { NavItem } from '@baros/components/layout/AppSidebar';
import { SidebarProvider, SidebarInset } from '@baros/components/ui/sidebar';

const REMOTE_ENTRY_URL = 'http://localhost:4202/remoteEntry.mjs';

const NAV_ITEMS: NavItem[] = [
    { key: 'dashboard', title: 'Dashboard', url: '/app-beta', icon: LayoutDashboard },
    { key: 'apis', title: 'APIs', url: '/app-beta/apis', icon: Cloud },
    { key: 'settings', title: 'Settings', url: '/app-beta/settings', icon: Settings },
];

function useActiveItemKey(): string {
    const { pathname } = useLocation();
    if (pathname === '/app-beta' || pathname === '/app-beta/') return 'dashboard';
    if (pathname.includes('/app-beta/apis')) return 'apis';
    if (pathname.includes('/app-beta/settings')) return 'settings';
    return 'dashboard';
}

function AppBetaContent() {
    const mountPointRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        let destroyAngularApp: (() => void) | undefined;

        const loadAngular = async () => {
            if (!mountPointRef.current) return;

            try {
                await import('zone.js');

                const container = await import(
                    /* webpackIgnore: true */ REMOTE_ENTRY_URL
                );

                await __webpack_init_sharing__('default');

                await container.init(__webpack_share_scopes__.default);

                const factory = await container.get('./Module');
                const { mount } = factory();

                destroyAngularApp = await mount(mountPointRef.current, { basePath: '/app-beta' });
            } catch (error) {
                console.error('Failed to load Angular remote:', error);
            }
        };

        loadAngular();

        return () => {
            destroyAngularApp?.();
        };
    }, []);

    return (
        <div
            ref={mountPointRef}
            className="angular-micro-frontend-container h-full w-full flex-1 p-6"
        />
    );
}

export function AppBetaLayout() {
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
                    <AppBetaContent />
                </main>
            </SidebarInset>
        </SidebarProvider>
    );
}
