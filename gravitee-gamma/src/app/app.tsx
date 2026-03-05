import * as React from 'react';
import NxWelcome from './nx-welcome';
import { Route, Routes } from 'react-router-dom';
import { AngularWrapper } from './angular-wrapper';
import { TopNav } from '@baros/components/layout/TopNav';
import { TopNavUser } from '@baros/components/layout/TopNavUser';
import { GraviteeLogo } from '@baros/components/layout/GraviteeLogo';

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

export function App() {
    return (
        <div className="flex min-h-svh w-full flex-col">
            <TopNav
                leading={<GraviteeLogo />}
                center={
                    <input
                        type="text"
                        placeholder="Search..."
                        className="h-7 w-64 rounded-md border border-input bg-background px-3 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring"
                    />
                }
                trailing={<TopNavUser user={{ name: 'Jane Doe', email: 'jane@gravitee.io' }} />}
            />
            <div className="flex flex-1 overflow-hidden">
                <React.Suspense fallback={null}>
                    <Routes>
                        <Route path="/" element={<NxWelcome title="gravitee-gamma" />} />
                        <Route path="/app-alpha/*" element={<AppAlpha />} />
                        <Route path="/app-beta/*" element={<AngularWrapper />} />
                    </Routes>
                </React.Suspense>
            </div>
        </div>
    );
}

export default App;
