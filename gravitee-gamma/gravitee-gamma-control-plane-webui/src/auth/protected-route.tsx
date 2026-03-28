import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './auth-context';

export function ProtectedRoute() {
    const { user } = useAuth();
    const location = useLocation();

    if (!user) {
        const redirect = location.pathname !== '/' ? `?redirect=${encodeURIComponent(location.pathname)}` : '';
        return <Navigate to={`/login${redirect}`} replace />;
    }

    return (
        <div style={{ fontFamily: 'system-ui, sans-serif', padding: '2rem' }}>
            <Outlet />
        </div>
    );
}

export function PublicOnlyRoute() {
    const { user } = useAuth();

    if (user) {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
}
