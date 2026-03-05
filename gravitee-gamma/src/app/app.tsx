import * as React from 'react';
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { Box, Settings, Search } from 'lucide-react';
import { TopNav } from '@baros/components/layout/TopNav';
import { TopNavUser } from '@baros/components/layout/TopNavUser';
import { GraviteeLogo } from '@baros/components/layout/GraviteeLogo';
import { ThemeToggle } from '@baros/components/layout/ThemeToggle';
import { AppDropdown } from '@baros/components/layout/AppDropdown';
import type { AppOption } from '@baros/components/layout/AppDropdown';
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

const APPS: AppOption[] = [
    { key: 'app-alpha', name: 'App Alpha', icon: Box },
    { key: 'app-beta', name: 'App Beta', icon: Settings },
];

function useActiveAppKey(): string {
    const { pathname } = useLocation();

    if (pathname.startsWith('/app-alpha')) return 'app-alpha';
    if (pathname.startsWith('/app-beta')) return 'app-beta';
    return '';
}

function WelcomePage() {
    return (
        <div className="mx-auto max-w-xl space-y-4 p-6">
            <h1 className="text-3xl font-extrabold tracking-tight">Welcome</h1>
            <p className="text-muted-foreground">
                Select an application from the dropdown to get started.
            </p>
        </div>
    );
}

export function App() {
    const navigate = useNavigate();
    const activeAppKey = useActiveAppKey();

    const handleAppChange = (key: string) => {
        navigate(`/${key}`);
    };

    return (
        <div className="flex min-h-svh w-full flex-col">
            <TopNav
                leading={
                    <div className="flex items-center gap-2">
                        <GraviteeLogo />
                        <AppDropdown
                            apps={APPS}
                            activeAppKey={activeAppKey}
                            onAppChange={handleAppChange}
                        />
                    </div>
                }
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
            <div className="flex flex-1 overflow-hidden">
                <React.Suspense fallback={null}>
                    <Routes>
                        <Route path="/" element={<WelcomePage />} />
                        <Route path="/app-alpha/*" element={<AppAlpha />} />
                        <Route path="/app-beta/*" element={<AngularWrapper />} />
                    </Routes>
                </React.Suspense>
            </div>
        </div>
    );
}

export default App;
