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

import { Button, PageFocused } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useRef, useState, type RefObject } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { CreateGatewayIntegration } from '../features/integrations/components/CreateGatewayIntegration';
import { IntegrationProviderSelector } from '../features/integrations/components/IntegrationProviderSelector';
import { SelectedProviderHeader } from '../features/integrations/components/SelectedProviderHeader';
import { findProvider, type ProviderCatalogEntry } from '../features/integrations/utils/providerLabels';

const PROVIDER_SEARCH_PARAM = 'provider';

function useFocusOnChange(value: string | undefined, targetRef: RefObject<HTMLElement | null>): void {
    const previousValue = useRef(value);
    useEffect(() => {
        if (previousValue.current === value) {
            return;
        }
        previousValue.current = value;
        targetRef.current?.focus();
    }, [value, targetRef]);
}

export function CreateIntegrationPage() {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const providerToken = searchParams.get(PROVIDER_SEARCH_PARAM);
    const provider = providerToken === null ? undefined : findProvider(providerToken);
    const [submitting, setSubmitting] = useState(false);
    const headingRef = useRef<HTMLHeadingElement>(null);
    useFocusOnChange(provider?.token, headingRef);

    function leaveToList() {
        navigate('..');
    }

    function selectProvider(selected: ProviderCatalogEntry) {
        setSearchParams(prev => {
            const next = new URLSearchParams(prev);
            next.set(PROVIDER_SEARCH_PARAM, selected.token);
            return next;
        });
    }

    function backToProviders() {
        setSearchParams(prev => {
            const next = new URLSearchParams(prev);
            next.delete(PROVIDER_SEARCH_PARAM);
            return next;
        });
    }

    return (
        <PageFocused>
            <div className="space-y-6">
                <div className="space-y-2">
                    {provider === undefined ? (
                        <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" onClick={leaveToList}>
                            <ArrowLeftIcon className="size-4" aria-hidden />
                            Back to Integrations
                        </Button>
                    ) : (
                        <Button
                            type="button"
                            variant="ghost"
                            className="gap-1.5 px-0 text-muted-foreground"
                            disabled={submitting}
                            onClick={backToProviders}
                        >
                            <ArrowLeftIcon className="size-4" aria-hidden />
                            Back to providers
                        </Button>
                    )}
                    <h1 ref={headingRef} tabIndex={-1} className="text-2xl font-semibold tracking-tight outline-none">
                        Create a new integration
                    </h1>
                    {provider === undefined && (
                        <p className="text-sm text-muted-foreground">
                            Choose the provider to connect. You&apos;ll set up the connection on the next step.
                        </p>
                    )}
                </div>
                {provider === undefined ? (
                    <IntegrationProviderSelector onSelect={selectProvider} />
                ) : (
                    <>
                        <SelectedProviderHeader provider={provider} onChange={backToProviders} disabled={submitting} />
                        <CreateGatewayIntegration provider={provider.token} onCancel={leaveToList} onSubmittingChange={setSubmitting} />
                    </>
                )}
            </div>
        </PageFocused>
    );
}
