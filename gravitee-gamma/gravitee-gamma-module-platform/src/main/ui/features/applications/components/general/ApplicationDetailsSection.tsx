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
import {
    Button,
    Card,
    CardContent,
    Input,
    Label,
    Separator,
    Textarea,
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@gravitee/graphene-core';
import { ClockIcon, GlobeIcon, InfoIcon, KeyRoundIcon, SettingsIcon, UserIcon } from '@gravitee/graphene-core/icons';
import type { CSSProperties } from 'react';
import { useNavigate } from 'react-router-dom';

import { ApplicationImagePickers } from './ApplicationImagePickers';
import { DetailRow } from './DetailRow';
import type { ApplicationListItem } from '../../types/application';
import {
    formatApplicationApiKeyMode,
    formatApplicationDateTime,
    formatApplicationOwnerLabel,
    formatApplicationSecurityTypeLabel,
} from '../../utils/applicationFormatters';
import type { ApplicationGeneralForm, ApplicationGeneralValidation } from '../../utils/applicationGeneralMapper';

export interface ApplicationDetailsSectionProps {
    readonly application: ApplicationListItem;
    readonly form: ApplicationGeneralForm;
    readonly validation: ApplicationGeneralValidation;
    readonly isFormDisabled: boolean;
    readonly showSubscribeToApis: boolean;
    readonly subscriptionsPath: string;
    readonly onFieldChange: <K extends keyof ApplicationGeneralForm>(key: K, value: ApplicationGeneralForm[K]) => void;
    readonly onPictureChange: (value: string | null) => void;
    readonly onBackgroundChange: (value: string | null) => void;
}

export function ApplicationDetailsSection({
    application,
    form,
    validation,
    isFormDisabled,
    showSubscribeToApis,
    subscriptionsPath,
    onFieldChange,
    onPictureChange,
    onBackgroundChange,
}: ApplicationDetailsSectionProps) {
    const navigate = useNavigate();

    return (
        <Card>
            <CardContent className="pt-6">
                <div className="flex flex-row gap-8">
                    <div className="min-w-0 flex-1 space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="app-name">
                                Name <span className="text-destructive">*</span>
                            </Label>
                            <Input
                                id="app-name"
                                className="w-full min-w-0"
                                value={form.name}
                                maxLength={512}
                                onChange={e => onFieldChange('name', e.target.value)}
                                disabled={isFormDisabled}
                                placeholder="Application name"
                            />
                            {validation.name ? <p className="text-xs text-destructive">{validation.name}</p> : null}
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="app-desc">
                                Description <span className="text-destructive">*</span>
                            </Label>
                            <Textarea
                                id="app-desc"
                                className="w-full min-w-0"
                                rows={3}
                                style={{ fieldSizing: 'fixed' } as CSSProperties}
                                value={form.description}
                                onChange={e => onFieldChange('description', e.target.value)}
                                disabled={isFormDisabled}
                                placeholder="Describe this application"
                            />
                            <p className="text-xs text-muted-foreground">
                                A brief description of your application shown to API publishers.
                            </p>
                            {validation.description ? <p className="text-xs text-destructive">{validation.description}</p> : null}
                        </div>
                        <div className="space-y-2">
                            <div className="flex items-center gap-1">
                                <Label htmlFor="app-domain">Domain</Label>
                                <TooltipProvider delayDuration={200}>
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <button
                                                type="button"
                                                className="text-muted-foreground/60 transition-colors hover:text-muted-foreground"
                                                aria-label="Domain field information"
                                            >
                                                <InfoIcon className="size-3.5" aria-hidden />
                                            </button>
                                        </TooltipTrigger>
                                        <TooltipContent side="right" className="max-w-xs text-xs">
                                            Optional domain used to group applications in the developer portal.
                                        </TooltipContent>
                                    </Tooltip>
                                </TooltipProvider>
                            </div>
                            <Input
                                id="app-domain"
                                className="w-full min-w-0"
                                value={form.domain}
                                onChange={e => onFieldChange('domain', e.target.value)}
                                disabled={isFormDisabled}
                                placeholder="e.g. operations.example.com"
                            />
                        </div>
                    </div>

                    <div className="w-[280px] shrink-0 space-y-5 border-l pl-8">
                        <ApplicationImagePickers
                            picture={form.picture}
                            background={form.background}
                            disabled={isFormDisabled}
                            onPictureChange={onPictureChange}
                            onBackgroundChange={onBackgroundChange}
                        />
                        <Separator />
                        <div className="space-y-3">
                            <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">DETAILS</p>
                            <dl className="space-y-2.5">
                                <DetailRow
                                    label={
                                        <>
                                            <UserIcon className="size-3" aria-hidden />
                                            Owner
                                        </>
                                    }
                                    value={formatApplicationOwnerLabel(application.owner) ?? '—'}
                                />
                                <DetailRow
                                    label={
                                        <>
                                            <ClockIcon className="size-3" aria-hidden />
                                            Created
                                        </>
                                    }
                                    value={formatApplicationDateTime(application.created_at)}
                                />
                                <DetailRow
                                    label={
                                        <>
                                            <SettingsIcon className="size-3" aria-hidden />
                                            Type
                                        </>
                                    }
                                    value={formatApplicationSecurityTypeLabel(application)}
                                />
                                <DetailRow
                                    label={
                                        <>
                                            <KeyRoundIcon className="size-3" aria-hidden />
                                            API key mode
                                        </>
                                    }
                                    value={formatApplicationApiKeyMode(application.api_key_mode)}
                                />
                            </dl>
                        </div>
                    </div>
                </div>

                {showSubscribeToApis ? (
                    <>
                        <Separator className="my-5" />
                        <Button type="button" variant="outline" size="sm" onClick={() => navigate(subscriptionsPath)}>
                            <GlobeIcon className="size-3.5" aria-hidden />
                            Subscribe to APIs
                        </Button>
                    </>
                ) : null}
            </CardContent>
        </Card>
    );
}
