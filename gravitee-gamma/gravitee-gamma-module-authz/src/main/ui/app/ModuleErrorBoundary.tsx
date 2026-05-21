import { Alert, AlertDescription, AlertTitle } from '@gravitee/graphene-core';
import { Component, type ErrorInfo, type ReactNode } from 'react';

interface State {
    readonly error: Error | null;
}

export class ModuleErrorBoundary extends Component<{ readonly children: ReactNode }, State> {
    state: State = { error: null };

    static getDerivedStateFromError(error: Error): State {
        return { error };
    }

    componentDidCatch(error: Error, info: ErrorInfo): void {
         
        console.error('authz UI error boundary caught:', error, info);
    }

    render(): ReactNode {
        if (this.state.error !== null) {
            return (
                <div style={{ padding: '1.5rem' }}>
                    <Alert variant="destructive">
                        <AlertTitle>Something went wrong</AlertTitle>
                        <AlertDescription>{this.state.error.message}</AlertDescription>
                    </Alert>
                </div>
            );
        }
        return this.props.children;
    }
}
