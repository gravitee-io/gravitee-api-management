import * as React from 'react';
import { Fragment } from 'react';
import { Route, Routes, useLocation, Link } from 'react-router-dom';
import { Home, Box, Settings, Search } from 'lucide-react';
import { MainLayout } from '@baros/components/layout/MainLayout';
import { TopNav } from '@baros/components/layout/TopNav';
import { TopNavUser } from '@baros/components/layout/TopNavUser';
import { GraviteeLogo } from '@baros/components/layout/GraviteeLogo';
import { ThemeToggle } from '@baros/components/layout/ThemeToggle';
import { AppSidebar } from '@baros/components/layout/AppSidebar';
import type { NavItem } from '@baros/components/layout/AppSidebar';
import { SidebarTrigger } from '@baros/components/ui/sidebar';
import { Separator } from '@baros/components/ui/separator';
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbPage,
    BreadcrumbSeparator,
} from '@baros/components/ui/breadcrumb';
import { AngularWrapper } from './angular-wrapper';

const APP_ALPHA_ENTRY_URL = 'http://localhost:4201/remoteEntry.js';

function loadRemoteScript(url: string): Promise<void> {
    return new Promise((resolve, reject) => {
        if (document.querySelector(`script[src="${url}"]`)) {
            resolve();
            return;
        }
        const script = document.createElement('script');
        script.src = url;
        script.type = 'text/javascript';
        script.onload = () => resolve();
        script.onerror = () => reject(new Error(`Failed to load remote entry: ${url}`));
        document.head.appendChild(script);
    });
}

const AppAlpha = React.lazy(async () => {
    await loadRemoteScript(APP_ALPHA_ENTRY_URL);
    await __webpack_init_sharing__('default');
    const container = (window as Record<string, any>)['app_alpha'];
    if (!container) throw new Error('app_alpha container not found on window');
    await container.init(__webpack_share_scopes__.default);
    const factory = await container.get('./Module');
    return factory();
});

const NAV_ITEMS: NavItem[] = [
    { key: 'home', title: 'Home', url: '/', icon: Home },
    { key: 'app-alpha', title: 'App Alpha', url: '/app-alpha', icon: Box },
    { key: 'app-beta', title: 'App Beta', url: '/app-beta', icon: Settings },
];

interface BreadcrumbEntry {
    readonly label: string;
    readonly href?: string;
}

function useBreadcrumbs(): BreadcrumbEntry[] {
    const { pathname } = useLocation();

    if (pathname.startsWith('/app-alpha')) {
        return [{ label: 'Home', href: '/' }, { label: 'App Alpha' }];
    }
    if (pathname.startsWith('/app-beta')) {
        return [{ label: 'Home', href: '/' }, { label: 'App Beta' }];
    }
    return [{ label: 'Home' }];
}

function useActiveNavKey(): string {
    const { pathname } = useLocation();

    if (pathname.startsWith('/app-alpha')) return 'app-alpha';
    if (pathname.startsWith('/app-beta')) return 'app-beta';
    return 'home';
}

function SubHeader() {
    const breadcrumbs = useBreadcrumbs();

    return (
        <div className="flex items-center gap-2 border-b border-border px-4 py-2">
            <SidebarTrigger />
            {breadcrumbs.length > 0 && (
                <>
                    <Separator orientation="vertical" className="mx-1 h-4" />
                    <Breadcrumb>
                        <BreadcrumbList>
                            {breadcrumbs.map((crumb, index) => {
                                const isLast = index === breadcrumbs.length - 1;
                                return (
                                    <Fragment key={crumb.label}>
                                        <BreadcrumbItem>
                                            {isLast ? (
                                                <BreadcrumbPage>{crumb.label}</BreadcrumbPage>
                                            ) : (
                                                <BreadcrumbLink asChild>
                                                    <Link to={crumb.href ?? '/'}>{crumb.label}</Link>
                                                </BreadcrumbLink>
                                            )}
                                        </BreadcrumbItem>
                                        {!isLast && <BreadcrumbSeparator />}
                                    </Fragment>
                                );
                            })}
                        </BreadcrumbList>
                    </Breadcrumb>
                </>
            )}
        </div>
    );
}

function WelcomePage() {
    return (
        <div className="space-y-4">
            <h1 className="text-3xl font-extrabold tracking-tight">Welcome</h1>
            <p className="text-muted-foreground">
                Select a section from the sidebar to get started.
            </p>
        </div>
    );
}

export function App() {
    const activeKey = useActiveNavKey();

    return (
        <MainLayout
            sidebar={<AppSidebar navItems={NAV_ITEMS} activeItemKey={activeKey} />}
            topnav={
                <TopNav
                    leading={<GraviteeLogo />}
                    trailing={
                        <div className="flex items-center gap-1">
                            <div className="relative">
                                <Search className="absolute left-2 top-1/2 size-3.5 -translate-y-1/2 text-muted-foreground" />
                                <input
                                    type="search"
                                    placeholder="Search..."
                                    className="h-7 w-48 rounded-md border border-input bg-background pl-8 pr-2 text-xs placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                                    aria-label="Global search"
                                />
                            </div>
                            <ThemeToggle />
                            <TopNavUser user={{ name: 'Jane Doe', email: 'jane@gravitee.io' }} />
                        </div>
                    }
                />
            }
            subheader={<SubHeader />}
        >
            <React.Suspense fallback={null}>
                <Routes>
                    <Route path="/" element={<WelcomePage />} />
                    <Route path="/app-alpha/*" element={<AppAlpha />} />
                    <Route path="/app-beta/*" element={<AngularWrapper />} />
                </Routes>
            </React.Suspense>
        </MainLayout>
    );
}

export default App;
