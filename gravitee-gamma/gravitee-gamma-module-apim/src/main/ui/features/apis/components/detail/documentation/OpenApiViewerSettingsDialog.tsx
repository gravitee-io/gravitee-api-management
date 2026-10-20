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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
} from '@gravitee/graphene-core';
import { useId, useState, type FormEvent, type ReactNode } from 'react';

import { RefusalAlert, ScrollableDialogBody, WITHIN_VIEWPORT } from './dialogLayout';
import { notify } from '../../../../../shared/notify';
import { extractErrorMessage } from '../../../../../shared/notify/extractErrorMessage';
import { useSaveApiDocumentationPageConfiguration } from '../../../hooks/useApiDocumentation';
import type { OpenApiDocExpansion, PortalPageOpenApiConfiguration } from '../../../types/apiDocumentation';

type Viewer = PortalPageOpenApiConfiguration['viewer'];

interface ViewerSettings {
    viewer: Viewer;
    tryItURL: string;
    entrypointsAsServers: boolean;
    contextPathAsServerPath: boolean;
    tryIt: boolean;
    disableSyntaxHighlight: boolean;
    tryItAnonymous: boolean;
    showURL: boolean;
    displayOperationId: boolean;
    usePkce: boolean;
    docExpansion: OpenApiDocExpansion;
    enableFiltering: boolean;
    showExtensions: boolean;
    showCommonExtensions: boolean;
    /** As typed: empty leaves the limit to the server. */
    maxDisplayedTags: string;
}

type ToggleKey = { [K in keyof ViewerSettings]: ViewerSettings[K] extends boolean ? K : never }[keyof ViewerSettings];

interface Toggle {
    key: ToggleKey;
    label: ReactNode;
    description?: string;
}

const BASE_URL_PATTERN = /^$|^https?:\/\/\S+$/;

// A select's options as wide as its field, which they would otherwise outgrow or fall short of.
const SELECT_CONTENT_STYLE = { width: 'var(--radix-select-trigger-width)', minWidth: 'unset' };

// The fields, labels, hints and defaults of the portal editor's Configure dialog, in its order.
const ENTRYPOINTS_TOGGLE: Toggle = {
    key: 'entrypointsAsServers',
    label: 'Use API entrypoints as server URLs',
    description: 'The base URL in the specification is replaced by the API entrypoints.',
};

const SERVER_AND_TRY_IT_TOGGLES: Toggle[] = [
    {
        key: 'contextPathAsServerPath',
        label: 'Use API context-path as server URL path',
        description: 'The context-path of the API will replace the path of the server URL.',
    },
    {
        key: 'tryIt',
        label: 'Enable "Try It!" mode',
        description:
            'Allows trying the API from the Developer Portal for authenticated users. You may have to configure CORS in the entrypoint section.',
    },
    {
        key: 'disableSyntaxHighlight',
        label: 'Disable response body styling for large JSON payloads',
        description: 'Displays API responses without Swagger UI styling to optimize performance when handling large payloads.',
    },
    {
        key: 'tryItAnonymous',
        label: 'Enable "Try It!" mode for anonymous users',
        description:
            'Allows users not logged into the Developer Portal to try the API from the OpenAPI spec when the page and API are public.',
    },
    { key: 'showURL', label: 'Show URL to download content' },
    {
        key: 'displayOperationId',
        label: (
            <>
                Display the <code>operationId</code> in the operation list
            </>
        ),
    },
    { key: 'usePkce', label: 'Use PKCE with OAuth', description: 'Uses PKCE when authenticating with OAuth authorization code flow.' },
];

const DISPLAY_TOGGLES: Toggle[] = [
    { key: 'enableFiltering', label: 'Add top bar to filter content' },
    {
        key: 'showExtensions',
        label: 'Display vendor extensions',
        description:
            'Determines whether vendor extension (X-) fields and values for operations, parameters, and schemas should be displayed.',
    },
    {
        key: 'showCommonExtensions',
        label: 'Display extension fields for Parameters',
        description: 'Determines whether pattern, maxLength, minLength, maximum, and minimum extensions should be shown.',
    },
];

const DOC_EXPANSIONS: { value: OpenApiDocExpansion; label: string }[] = [
    { value: 'list', label: 'Only tags' },
    { value: 'full', label: 'Tags and operations' },
    { value: 'none', label: 'Default' },
];

export function OpenApiViewerSettingsDialog({
    open,
    apiId,
    pageId,
    configuration,
    onClose,
}: Readonly<{
    open: boolean;
    apiId: string;
    pageId: string;
    configuration?: PortalPageOpenApiConfiguration;
    onClose: () => void;
}>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent style={{ maxWidth: '560px', ...WITHIN_VIEWPORT }}>
                <OpenApiViewerSettingsForm apiId={apiId} pageId={pageId} configuration={configuration} onClose={onClose} />
            </DialogContent>
        </Dialog>
    );
}

// Rendered inside the dialog content, so its state starts over from the saved configuration each time the dialog opens.
function OpenApiViewerSettingsForm({
    apiId,
    pageId,
    configuration,
    onClose,
}: Readonly<{
    apiId: string;
    pageId: string;
    configuration?: PortalPageOpenApiConfiguration;
    onClose: () => void;
}>) {
    const saveConfiguration = useSaveApiDocumentationPageConfiguration(apiId);
    const [settings, setSettings] = useState(() => withoutBaseUrlWhenUsingEntrypoints(toSettings(configuration)));
    const [isSaving, setIsSaving] = useState(false);
    const [refusal, setRefusal] = useState<string | null>(null);
    const ids = { viewer: useId(), baseUrl: useId(), docExpansion: useId(), maxDisplayedTags: useId() };

    const isSwagger = settings.viewer === 'SWAGGER';
    const usesEntrypoints = isSwagger && settings.entrypointsAsServers;
    const baseUrlValid = BASE_URL_PATTERN.test(settings.tryItURL);

    function change(changes: Partial<ViewerSettings>) {
        setSettings(current => withoutBaseUrlWhenUsingEntrypoints({ ...current, ...changes }));
        setRefusal(null);
    }

    async function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!baseUrlValid || isSaving) return;
        // Cleared first, so that the same refusal given again is shown, and brought into view, again.
        setRefusal(null);
        setIsSaving(true);
        try {
            await saveConfiguration.mutateAsync({ navId: pageId, configuration: toConfiguration(settings) });
            notify.success('OpenAPI viewer configuration saved');
            onClose();
        } catch (error) {
            setRefusal(extractErrorMessage(error));
            setIsSaving(false);
        }
    }

    function renderToggle(toggle: Toggle) {
        return (
            <ToggleField
                key={toggle.key}
                toggle={toggle}
                checked={settings[toggle.key]}
                onChange={checked => change({ [toggle.key]: checked })}
                disabled={isSaving}
            />
        );
    }

    return (
        <>
            <DialogHeader>
                <DialogTitle>Configure OpenAPI Viewer</DialogTitle>
                <DialogDescription>Changes apply in every portal that lists this API.</DialogDescription>
            </DialogHeader>

            <ScrollableDialogBody>
                <form id="openapi-viewer-settings-form" onSubmit={handleSubmit} className="flex flex-col gap-5">
                    <RefusalAlert refusal={refusal} />

                    {isSwagger ? renderToggle(ENTRYPOINTS_TOGGLE) : null}

                    <Field orientation="vertical" className="gap-1.5" data-invalid={!baseUrlValid || undefined}>
                        <FieldLabel htmlFor={ids.baseUrl}>Base URL</FieldLabel>
                        <Input
                            id={ids.baseUrl}
                            value={settings.tryItURL}
                            onChange={event => change({ tryItURL: event.target.value })}
                            aria-invalid={!baseUrlValid}
                            disabled={usesEntrypoints || isSaving}
                        />
                        {baseUrlValid ? null : <FieldError>Must be a valid URL starting with http:// or https://</FieldError>}
                        <FieldDescription>
                            Custom base URL to use as server URL. If empty and not using API entrypoints as a base URL, the server URL from
                            the specification will be used.
                        </FieldDescription>
                    </Field>

                    <Field orientation="vertical" className="gap-1.5">
                        <FieldLabel htmlFor={ids.viewer}>OpenAPI Documentation Viewer</FieldLabel>
                        <Select value={settings.viewer} onValueChange={viewer => change({ viewer: viewer as Viewer })} disabled={isSaving}>
                            <SelectTrigger id={ids.viewer} className="w-full">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent position="popper" style={SELECT_CONTENT_STYLE}>
                                <SelectItem value="SWAGGER">SwaggerUI</SelectItem>
                                <SelectItem value="REDOC">Redoc</SelectItem>
                            </SelectContent>
                        </Select>
                    </Field>

                    {isSwagger ? (
                        <>
                            {SERVER_AND_TRY_IT_TOGGLES.map(renderToggle)}

                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor={ids.docExpansion}>Expand content on the page</FieldLabel>
                                <Select
                                    value={settings.docExpansion}
                                    onValueChange={docExpansion => change({ docExpansion: docExpansion as OpenApiDocExpansion })}
                                    disabled={isSaving}
                                >
                                    <SelectTrigger id={ids.docExpansion} className="w-full">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent position="popper" style={SELECT_CONTENT_STYLE}>
                                        {DOC_EXPANSIONS.map(expansion => (
                                            <SelectItem key={expansion.value} value={expansion.value}>
                                                {expansion.label}
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                                <FieldDescription>
                                    Determines whether to expand operations, tags, or nothing by default when the page is opened.
                                </FieldDescription>
                            </Field>

                            {DISPLAY_TOGGLES.map(renderToggle)}

                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor={ids.maxDisplayedTags}>Max number of tagged operations displayed</FieldLabel>
                                <Input
                                    id={ids.maxDisplayedTags}
                                    type="number"
                                    min={-1}
                                    value={settings.maxDisplayedTags}
                                    onChange={event => change({ maxDisplayedTags: event.target.value })}
                                    disabled={isSaving}
                                />
                                <FieldDescription>
                                    Limits the number of tagged operations displayed to the specific value. -1 means to show all operations.
                                </FieldDescription>
                            </Field>
                        </>
                    ) : null}
                </form>
            </ScrollableDialogBody>

            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                    Cancel
                </Button>
                <Button type="submit" form="openapi-viewer-settings-form" disabled={!baseUrlValid || isSaving}>
                    {isSaving ? 'Saving…' : 'Save'}
                </Button>
            </DialogFooter>
        </>
    );
}

function ToggleField({
    toggle,
    checked,
    onChange,
    disabled,
}: Readonly<{ toggle: Toggle; checked: boolean; onChange: (checked: boolean) => void; disabled: boolean }>) {
    const id = useId();
    const descriptionId = useId();
    return (
        <div className="flex items-start gap-3">
            <Switch
                id={id}
                checked={checked}
                onCheckedChange={onChange}
                disabled={disabled}
                aria-describedby={toggle.description ? descriptionId : undefined}
                className="mt-0.5"
            />
            <div className="flex flex-col gap-1">
                <Label htmlFor={id} className="font-normal">
                    {toggle.label}
                </Label>
                {toggle.description ? <FieldDescription id={descriptionId}>{toggle.description}</FieldDescription> : null}
            </div>
        </div>
    );
}

function toSettings(configuration?: PortalPageOpenApiConfiguration): ViewerSettings {
    const swagger = configuration?.viewer === 'REDOC' ? undefined : configuration;
    const maxDisplayedTags = swagger?.maxDisplayedTags;
    return {
        viewer: configuration?.viewer ?? 'SWAGGER',
        tryItURL: configuration?.tryItURL ?? '',
        entrypointsAsServers: swagger?.entrypointsAsServers ?? false,
        contextPathAsServerPath: swagger?.contextPathAsServerPath ?? false,
        tryIt: swagger?.tryIt ?? false,
        disableSyntaxHighlight: swagger?.disableSyntaxHighlight ?? false,
        tryItAnonymous: swagger?.tryItAnonymous ?? false,
        showURL: swagger?.showURL ?? false,
        displayOperationId: swagger?.displayOperationId ?? false,
        usePkce: swagger?.usePkce ?? false,
        docExpansion: swagger?.docExpansion ?? 'none',
        enableFiltering: swagger?.enableFiltering ?? false,
        showExtensions: swagger?.showExtensions ?? false,
        showCommonExtensions: swagger?.showCommonExtensions ?? false,
        maxDisplayedTags: maxDisplayedTags === undefined ? '' : String(maxDisplayedTags),
    };
}

/** The portal editor's rule: Swagger UI using the API entrypoints as servers has no base URL. */
function withoutBaseUrlWhenUsingEntrypoints(settings: ViewerSettings): ViewerSettings {
    return settings.viewer === 'SWAGGER' && settings.entrypointsAsServers ? { ...settings, tryItURL: '' } : settings;
}

// The server keeps only the base URL of a Redoc configuration, so nothing else is sent for it.
function toConfiguration({ maxDisplayedTags, ...settings }: ViewerSettings): PortalPageOpenApiConfiguration {
    if (settings.viewer === 'REDOC') {
        return { viewer: 'REDOC', tryItURL: settings.tryItURL };
    }
    const swagger = { ...settings, viewer: 'SWAGGER' as const };
    return maxDisplayedTags === '' ? swagger : { ...swagger, maxDisplayedTags: Number(maxDisplayedTags) };
}
