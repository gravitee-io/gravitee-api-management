import { createContext, useContext, useState, type ReactNode } from 'react';
import { useBootstrap } from './bootstrap-context';

interface EnvironmentState {
    organizationId: string;
    environmentId: string;
}

interface EnvironmentContextValue extends EnvironmentState {
    setEnvironment: (org: string, env: string) => void;
}

const EnvironmentContext = createContext<EnvironmentContextValue | null>(null);

export function EnvironmentProvider({ children }: { readonly children: ReactNode }) {
    const { organizationId: bootstrapOrgId } = useBootstrap();
    const [state, setState] = useState<EnvironmentState>({
        organizationId: bootstrapOrgId,
        environmentId: 'DEFAULT',
    });

    const setEnvironment = (org: string, env: string) => {
        setState({ organizationId: org, environmentId: env });
    };

    return <EnvironmentContext.Provider value={{ ...state, setEnvironment }}>{children}</EnvironmentContext.Provider>;
}

export function useEnvironment(): EnvironmentContextValue {
    const ctx = useContext(EnvironmentContext);
    if (!ctx) throw new Error('useEnvironment must be used within EnvironmentProvider');
    return ctx;
}
