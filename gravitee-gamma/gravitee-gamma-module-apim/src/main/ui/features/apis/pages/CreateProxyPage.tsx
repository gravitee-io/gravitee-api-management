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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
import { Button, Card } from '@gravitee/graphene-core';
import { ArrowRightIcon, ChevronDownIcon, LayoutGridIcon, PencilIcon, ScrollTextIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { APIM_ROUTE_CONFIG } from '../../../config/routes';
import { WizardStarter } from '../components/wizard/ApiCreationWizard';
import { ApiCreationStoreProvider } from '../store/useApiCreationStore';
import { apiCreationTemplates } from '../templates';
import { TemplateCard } from '../templates/common/TemplateCard';

const DOCS_URL = 'https://documentation.gravitee.io/apim';

export function CreateProxyPage() {
    const navigate = useNavigate();
    const { rootPath } = useModuleRouting(APIM_ROUTE_CONFIG);
    const [templatesOpen, setTemplatesOpen] = useState(false);
    const [wizardOpen, setWizardOpen] = useState(false);
    const [selectedTemplateId, setSelectedTemplateId] = useState<string | undefined>(undefined);

    const handleExit = useCallback(() => {
        navigate(rootPath);
    }, [navigate, rootPath]);

    const closeWizard = useCallback(() => {
        setWizardOpen(false);
        setSelectedTemplateId(undefined);
    }, []);

    if (wizardOpen) {
        return (
            <ApiCreationStoreProvider>
                <WizardStarter templateId={selectedTemplateId} onExit={closeWizard} />
            </ApiCreationStoreProvider>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
                <div className="min-w-0 space-y-2">
                    <h1 className="text-2xl font-semibold tracking-tight">Create API Proxy</h1>
                    <p className="text-sm text-muted-foreground">Choose the full wizard or a preset, then add your API details.</p>
                </div>
                <Button type="button" variant="outline" size="sm" className="ml-auto h-9 shrink-0 gap-2 self-end sm:self-start" asChild>
                    <a href={DOCS_URL} target="_blank" rel="noopener noreferrer">
                        <ScrollTextIcon className="size-4" aria-hidden="true" />
                        Documentation
                    </a>
                </Button>
            </div>

            <div className="grid grid-cols-1 items-stretch gap-4 lg:grid-cols-2">
                <button
                    type="button"
                    onClick={() => {
                        setSelectedTemplateId(undefined);
                        setWizardOpen(true);
                    }}
                    className="group flex h-full min-h-[11rem] flex-col items-stretch gap-4 rounded-xl border-2 border-primary/30 bg-primary/5 p-6 text-left shadow-sm transition-all hover:border-primary/55 hover:bg-primary/10 hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40"
                >
                    <div className="flex items-start gap-4">
                        <div className="rounded-lg bg-muted p-3">
                            <PencilIcon className="size-6 text-primary" aria-hidden="true" />
                        </div>
                        <div className="min-w-0 flex-1 space-y-1">
                            <p className="text-base font-semibold text-foreground">Start from scratch</p>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                                Full four-step wizard: details, entrypoints, security, and review. No preset plans — you choose everything
                                explicitly.
                            </p>
                        </div>
                        <ArrowRightIcon
                            className="size-5 shrink-0 text-primary transition-transform group-hover:translate-x-0.5"
                            aria-hidden="true"
                        />
                    </div>
                </button>

                <button
                    type="button"
                    onClick={() => setTemplatesOpen(o => !o)}
                    className={`group flex h-full min-h-[11rem] flex-col items-stretch gap-4 rounded-xl border-2 bg-card p-6 text-left shadow-sm transition-all hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 ${
                        templatesOpen ? 'border-primary/50 bg-primary/5' : 'border-border hover:border-primary/35'
                    }`}
                >
                    <div className="flex items-start gap-4">
                        <div className="rounded-lg bg-muted p-3">
                            <LayoutGridIcon className="size-6 text-primary" aria-hidden="true" />
                        </div>
                        <div className="min-w-0 flex-1 space-y-1">
                            <p className="text-base font-semibold text-foreground">Quick-start templates</p>
                            <p className="text-sm text-muted-foreground leading-relaxed">
                                Presets with security and plans filled in. You mainly add name and upstream URL; adjust anything on review.
                            </p>
                        </div>
                        <ChevronDownIcon
                            className={`size-5 shrink-0 text-muted-foreground transition-transform duration-200 ${templatesOpen ? 'rotate-180' : ''}`}
                            aria-hidden="true"
                        />
                    </div>
                </button>
            </div>

            {templatesOpen ? (
                <Card className="rounded-xl p-4 sm:p-6">
                    <div className="space-y-4">
                        <p className="text-sm text-muted-foreground">
                            Pick a template to open essentials with the right auth pattern pre-selected.
                        </p>

                        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                            {apiCreationTemplates.map(template => (
                                <TemplateCard
                                    key={template.id}
                                    template={template}
                                    onClick={() => {
                                        setSelectedTemplateId(template.id);
                                        setWizardOpen(true);
                                    }}
                                />
                            ))}
                        </div>
                    </div>
                </Card>
            ) : null}

            <div className="flex items-center justify-between border-t pt-4">
                <Button type="button" variant="outline" size="sm" onClick={handleExit}>
                    Exit
                </Button>
                <span className="text-xs text-muted-foreground max-w-md text-right">
                    Pick Start from scratch or open Quick-start templates, then select a preset.
                </span>
            </div>
        </div>
    );
}
