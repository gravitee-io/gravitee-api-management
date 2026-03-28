import React from 'react';
import { Link, Route, Routes } from 'react-router-dom';
import { LoginPage } from '../auth/login-page';
import { ProtectedRoute, PublicOnlyRoute } from '../auth/protected-route';
import { GammaModule, RemoteModuleRoute, useGammaModules } from '../gamma-modules';

function Home({ modules, loading, error }: { readonly modules: GammaModule[]; readonly loading: boolean; readonly error: Error | null }) {
    return (
        <div>
            <h1>Welcome to Gravitee Gamma</h1>
            <h2>Shell</h2>
            <ul>
                <li>
                    <Link to="/about">About</Link>
                </li>
            </ul>
            <h2>Modules</h2>
            {loading && <p>Loading modules…</p>}
            {error && <p>Error loading modules: {error.message}</p>}
            {!loading && !error && modules.length === 0 && <p>No modules available.</p>}
            {modules.length > 0 && (
                <ul>
                    {modules.map(m => (
                        <li key={m.id}>
                            <Link to={`/${m.id}`}>
                                <strong>{m.name}</strong>
                            </Link>{' '}
                            (v{m.version})
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}

function About() {
    return (
        <div>
            <h1>About</h1>
            <p>
                <Link to="/">Home</Link>
            </p>
        </div>
    );
}

export function App() {
    const { modules, loading, error } = useGammaModules();

    return (
        <React.Suspense fallback={<p>Loading…</p>}>
            <Routes>
                <Route element={<PublicOnlyRoute />}>
                    <Route path="/login" element={<LoginPage />} />
                </Route>
                <Route element={<ProtectedRoute />}>
                    <Route path="/" element={<Home modules={modules} loading={loading} error={error} />} />
                    <Route path="/about" element={<About />} />
                    {modules.map(m => (
                        <Route key={m.id} path={`/${m.id}/*`} element={<RemoteModuleRoute module={m} />} />
                    ))}
                </Route>
            </Routes>
        </React.Suspense>
    );
}

export default App;
