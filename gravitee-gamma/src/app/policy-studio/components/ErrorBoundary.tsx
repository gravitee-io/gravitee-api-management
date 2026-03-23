import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Button } from '@baros/components/ui/button';

interface ErrorBoundaryProps {
  readonly fallbackLabel?: string;
  readonly children: ReactNode;
}

interface ErrorBoundaryState {
  readonly error: Error | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error(`ErrorBoundary caught:`, error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="flex flex-col items-center justify-center gap-2 p-4 text-center">
          <div className="text-sm text-destructive">
            {this.props.fallbackLabel ?? 'Something went wrong'}
          </div>
          <div className="text-xs text-muted-foreground">
            {this.state.error.message}
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => this.setState({ error: null })}
          >
            Retry
          </Button>
        </div>
      );
    }

    return this.props.children;
  }
}
