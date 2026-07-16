/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useEffect, type ReactNode } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

import type { DeveloperPortal } from '../../portals/types';
import { useConsumerAuth } from '../hooks/useConsumerAuth';
import { LoginPage } from './LoginPage';

interface ConsumerAuthGateProps {
    readonly loginPath: string;
    readonly children: ReactNode;
    /** When true, render login inside the preview frame instead of navigating away (editor preview). */
    readonly inlineAuth?: boolean;
    readonly portal?: DeveloperPortal;
    readonly signupPath?: string;
    readonly defaultRedirectPath?: string;
}

export function ConsumerAuthGate({
    loginPath,
    children,
    inlineAuth = false,
    portal,
    signupPath,
    defaultRedirectPath,
}: ConsumerAuthGateProps) {
    const { isAuthenticated, isLoading, consumerAuthGateEnabled, previewMode } = useConsumerAuth();
    const navigate = useNavigate();
    const location = useLocation();

    const shouldGate = previewMode && consumerAuthGateEnabled;

    useEffect(() => {
        if (!shouldGate || inlineAuth || isLoading || isAuthenticated) {
            return;
        }

        const redirect = `${location.pathname}${location.search}`;
        const separator = loginPath.includes('?') ? '&' : '?';
        navigate(`${loginPath}${separator}redirect=${encodeURIComponent(redirect)}`, { replace: true });
    }, [
        inlineAuth,
        isAuthenticated,
        isLoading,
        location.pathname,
        location.search,
        loginPath,
        navigate,
        shouldGate,
    ]);

    if (!shouldGate) {
        return <>{children}</>;
    }

    if (isLoading) {
        return <p className="p-6 text-sm text-muted-foreground">Checking session…</p>;
    }

    if (!isAuthenticated) {
        if (inlineAuth && portal && signupPath && defaultRedirectPath) {
            return (
                <LoginPage
                    portal={portal}
                    loginPath={loginPath}
                    signupPath={signupPath}
                    defaultRedirectPath={defaultRedirectPath}
                    embedded
                />
            );
        }

        return null;
    }

    return <>{children}</>;
}
