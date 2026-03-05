import * as React from 'react';
import NxWelcome from './nx-welcome';
import { Link, Route, Routes } from 'react-router-dom';
import { AngularWrapper } from './angular-wrapper';

const AppAlpha = React.lazy(() => import('app_alpha/Module'));

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
