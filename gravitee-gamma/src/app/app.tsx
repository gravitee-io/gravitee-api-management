import * as React from 'react';
import NxWelcome from './nx-welcome';
import { Link, Route, Routes } from 'react-router-dom';
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

export function App() {
    return (
        <React.Suspense fallback={null}>
            <ul>
                <li>
                    <Link to="/">Home</Link>
                </li>
                <li>
                    <Link to="/app-alpha">AppAlpha</Link>
                </li>
                <li>
                    <Link to="/app-beta">AppBeta</Link>
                </li>
            </ul>
            <Routes>
                <Route path="/" element={<NxWelcome title="gravitee-gamma" />} />
                <Route path="/app-alpha" element={<AppAlpha />} />
                <Route path="/app-beta" element={<AngularWrapper />} />
            </Routes>
        </React.Suspense>
    );
}

export default App;
