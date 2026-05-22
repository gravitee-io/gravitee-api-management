/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
