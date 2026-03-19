import { Component, type ErrorInfo, type ReactNode } from 'react';

interface Props {
    readonly fallback: (error: Error, retry: () => void) => ReactNode;
    readonly children: ReactNode;
}

interface State {
    error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
    state: State = { error: null };

    static getDerivedStateFromError(error: Error): State {
        return { error };
    }

    componentDidCatch(error: Error, info: ErrorInfo) {
        console.error('ErrorBoundary caught:', error, info);
    }

    render() {
        if (this.state.error) {
            return this.props.fallback(this.state.error, () => window.location.reload());
        }
        return this.props.children;
    }
}
