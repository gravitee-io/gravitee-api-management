import { createContext, use, useContext, type ReactNode } from 'react';
import { type BootstrapConfig, getBootstrapConfig } from './bootstrap.service';
import { setManagementBase } from '../auth/auth.api';

const BootstrapContext = createContext<BootstrapConfig | null>(null);

const configPromise = getBootstrapConfig();

export function BootstrapProvider({ children }: { readonly children: ReactNode }) {
    const config = use(configPromise);
    // Idempotent — safe to call on every render until the module-level state is replaced (see FIXME in auth.api.ts)
    setManagementBase(`${config.managementBaseURL}/organizations/${config.organizationId}`);
    return <BootstrapContext value={config}>{children}</BootstrapContext>;
}

export function useBootstrap(): BootstrapConfig {
    const ctx = useContext(BootstrapContext);
    if (!ctx) throw new Error('useBootstrap must be used within BootstrapProvider');
    return ctx;
}
