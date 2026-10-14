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
    Alert,
    AlertDescription,
    extractDefaults,
    type JsonSchema,
    JsonSchemaForm,
    jsonSchemaResolver,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
    Switch,
} from '@gravitee/graphene-core';
import { CodeEditor } from '@gravitee/graphene-core/code-editor';
import { AlertCircleIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react';
import { type FieldValues, useForm } from 'react-hook-form';

import { useFetchers } from '../../../hooks/useFetchers';
import type { PortalNavigationItemSource } from '../../../types/apiDocumentation';
import { fetcherLabel } from '../../../utils/fetcherSchema';
import { isSameSource } from '../../../utils/sourceChange';
import { isAddressField, MASKED_SECRET, maskedSecretKeys, sameSourceAddress } from '../../../utils/sourceSecrets';
import { CronScheduleInput, validateCron } from '../../CronScheduleInput';

/** The source as edited so far, and whether it can be saved as it is. */
export interface ExternalSourceState {
    source: PortalNavigationItemSource | null;
    valid: boolean;
    /** Given a stored source: whether the edited one is that source, the defaults of its fetcher's settings aside. */
    unchanged?: boolean;
}

interface FetcherSettings {
    configuration: Record<string, unknown>;
    valid: boolean;
}

/**
 * The fetcher of a source, its settings and its schedule. The settings form comes from the fetcher plugin's schema, with
 * a JSON editor for any schema the form renders badly.
 */
export function ExternalSourceConfig({
    storedSource,
    forImport = false,
    disabled = false,
    onChange,
}: Readonly<{
    /** The source the item has; going back to its fetcher brings its settings back. */
    storedSource?: PortalNavigationItemSource;
    /** Offers only the fetchers that can list a folder. */
    forImport?: boolean;
    disabled?: boolean;
    onChange: (state: ExternalSourceState) => void;
}>) {
    const { data: fetchers, isLoading, isError } = useFetchers({ forImport });
    const [type, setType] = useState(storedSource?.type ?? '');
    const [settings, setSettings] = useState<FetcherSettings | null>(null);
    const [useAutoFetch, setUseAutoFetch] = useState(storedSource?.useAutoFetch ?? false);
    const [fetchCron, setFetchCron] = useState(storedSource?.fetchCron ?? '');
    const typeId = useId();
    const autoFetchId = useId();

    const onChangeRef = useRef(onChange);
    onChangeRef.current = onChange;

    const scheduleError = fetchCron ? validateCron(fetchCron) : 'Choose when to fetch.';

    // The server leaves out settings left at their default, which the settings form fills in again.
    const storedWithDefaults = useMemo(() => {
        const schema = fetchers?.find(candidate => candidate.id === storedSource?.type)?.schema;
        if (!storedSource || !schema) return storedSource;
        return { ...storedSource, configuration: storedSettings(schema, storedSource.configuration) };
    }, [fetchers, storedSource]);

    useEffect(() => {
        if (!type || !settings) {
            onChangeRef.current({ source: null, valid: false, ...(storedWithDefaults ? { unchanged: false } : {}) });
            return;
        }
        const source = { type, configuration: settings.configuration, useAutoFetch, ...(useAutoFetch ? { fetchCron } : {}) };
        onChangeRef.current({
            source,
            valid: settings.valid && (!useAutoFetch || scheduleError === null),
            ...(storedWithDefaults ? { unchanged: isSameSource(storedWithDefaults, source) } : {}),
        });
    }, [type, settings, useAutoFetch, fetchCron, scheduleError, storedWithDefaults]);

    if (isLoading) {
        return <Skeleton className="h-24 w-full rounded" />;
    }
    if (isError || !fetchers) {
        return (
            <Alert variant="destructive">
                <AlertCircleIcon />
                <AlertDescription>The external source types could not be loaded. Close this dialog and try again.</AlertDescription>
            </Alert>
        );
    }
    if (fetchers.length === 0) {
        return (
            <p className="text-sm text-muted-foreground">
                {forImport
                    ? 'No installed external source type can import a folder. Ask your administrator to install one, such as GitHub or GitLab.'
                    : 'No external source type is installed. Ask your administrator to install one, such as GitHub or HTTP.'}
            </p>
        );
    }

    const fetcher = fetchers.find(candidate => candidate.id === type);
    const isStoredType = storedSource !== undefined && storedSource.type === type;

    function chooseFetcher(id: string) {
        setSettings(null);
        setType(id);
    }

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
                <Label htmlFor={typeId}>Source type</Label>
                <Select value={type} onValueChange={chooseFetcher} disabled={disabled}>
                    <SelectTrigger id={typeId} className="w-full">
                        <SelectValue placeholder="Select a source type" />
                    </SelectTrigger>
                    <SelectContent>
                        {fetchers.map(candidate => (
                            <SelectItem key={candidate.id} value={candidate.id}>
                                {candidate.name}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                {forImport ? <p className="text-xs text-muted-foreground">Only sources that can list a folder are offered.</p> : null}
            </div>

            {type && !fetcher ? (
                <Alert variant="destructive">
                    <AlertCircleIcon />
                    <AlertDescription>
                        The {fetcherLabel(type)} source type is no longer installed. Choose another one, or ask your administrator to
                        install it again.
                    </AlertDescription>
                </Alert>
            ) : null}

            {fetcher ? (
                <FetcherSettingsForm
                    key={fetcher.id}
                    schema={fetcher.schema}
                    storedConfiguration={isStoredType ? storedSource.configuration : undefined}
                    storedSettingsShown={isStoredType ? storedWithDefaults?.configuration : undefined}
                    disabled={disabled}
                    onChange={setSettings}
                />
            ) : null}

            <div className="flex flex-col gap-3">
                <div className="flex items-center gap-2">
                    <Switch id={autoFetchId} checked={useAutoFetch} onCheckedChange={setUseAutoFetch} disabled={disabled} />
                    <Label htmlFor={autoFetchId} className="font-normal">
                        Fetch automatically
                    </Label>
                </div>
                {useAutoFetch ? (
                    <CronScheduleInput
                        idPrefix={`${autoFetchId}-schedule`}
                        value={fetchCron}
                        onChange={setFetchCron}
                        error={scheduleError ?? undefined}
                        disabled={disabled}
                    />
                ) : null}
            </div>
        </div>
    );
}

// Keyed by fetcher, so that choosing another one starts its settings afresh.
function FetcherSettingsForm({
    schema,
    storedConfiguration,
    storedSettingsShown,
    disabled,
    onChange,
}: Readonly<{
    schema: JsonSchema;
    /** The settings the item has, when the source keeps its fetcher. */
    storedConfiguration?: Record<string, unknown>;
    /** Those settings as the form shows them, the ones the server left out filled in. */
    storedSettingsShown?: Record<string, unknown>;
    disabled: boolean;
    onChange: (settings: FetcherSettings) => void;
}>) {
    const resolver = useMemo(() => jsonSchemaResolver(schema, { basePath: 'configuration' }), [schema]);
    const form = useForm<FieldValues>({
        resolver,
        mode: 'onTouched',
        defaultValues: { configuration: storedSettingsShown ?? settingsDefaults(schema) },
    });
    const [secretsCleared, setSecretsCleared] = useState(false);
    const [jsonText, setJsonText] = useState<string | null>(null);
    const [jsonInvalid, setJsonInvalid] = useState(false);
    const [settingsValid, setSettingsValid] = useState(true);
    const jsonSwitchId = useId();

    const onChangeRef = useRef(onChange);
    onChangeRef.current = onChange;
    // Validation is asynchronous: only the outcome for the latest settings is reported.
    const reportCount = useRef(0);

    const report = useCallback(
        async (configuration: Record<string, unknown>) => {
            const count = ++reportCount.current;
            const result = await resolver({ configuration }, undefined, { fields: {}, shouldUseNativeValidation: false });
            if (count === reportCount.current) {
                const valid = Object.keys(result.errors).length === 0;
                setSettingsValid(valid);
                onChangeRef.current({ configuration, valid });
            }
        },
        [resolver],
    );

    const currentConfiguration = useCallback((): Record<string, unknown> => form.getValues().configuration ?? {}, [form]);

    // The secrets cleared for another address, put back masked if the address returns to the stored one.
    const clearedSecrets = useRef<string[]>([]);

    /**
     * The server restores a masked secret only for the address it belongs to. Once the address changes, a masked secret
     * would be refused, so it is cleared for the user to enter it again.
     */
    const syncSecretsWithAddress = useCallback(
        (configuration: Record<string, unknown>): Record<string, unknown> => {
            if (!storedConfiguration) return configuration;
            // Compared with the settings as stored, as the server compares them.
            if (sameSourceAddress(storedConfiguration, configuration)) {
                const restorable = clearedSecrets.current.filter(key => !configuration[key]);
                clearedSecrets.current = [];
                setSecretsCleared(false);
                return restorable.length === 0
                    ? configuration
                    : { ...configuration, ...Object.fromEntries(restorable.map(key => [key, MASKED_SECRET])) };
            }
            const masked = maskedSecretKeys(configuration);
            if (masked.length === 0) return configuration;
            clearedSecrets.current = [...new Set([...clearedSecrets.current, ...masked])];
            setSecretsCleared(true);
            return { ...configuration, ...Object.fromEntries(masked.map(key => [key, ''])) };
        },
        [storedConfiguration],
    );

    useEffect(() => {
        const reports = reportCount;
        void report(currentConfiguration());
        const subscription = form.watch(values => {
            const configuration = (values.configuration ?? {}) as Record<string, unknown>;
            const synced = syncSecretsWithAddress(configuration);
            if (synced !== configuration) {
                // The watch fires again with the synced values, which are reported then.
                form.reset({ configuration: synced }, { keepDefaultValues: true });
                return;
            }
            void report(configuration);
        });
        return () => {
            subscription.unsubscribe();
            // A validation still running for a fetcher no longer chosen must not report.
            reports.current++;
        };
    }, [form, report, currentConfiguration, syncSecretsWithAddress]);

    function toggleJson(editAsJson: boolean) {
        setJsonInvalid(false);
        setJsonText(editAsJson ? JSON.stringify(currentConfiguration(), null, 2) : null);
    }

    function editJson(text: string) {
        setJsonText(text);
        const parsed = parseConfiguration(text);
        if (!parsed) {
            setJsonInvalid(true);
            reportCount.current++;
            onChangeRef.current({ configuration: currentConfiguration(), valid: false });
            return;
        }
        setJsonInvalid(false);
        const synced = syncSecretsWithAddress(parsed);
        if (synced !== parsed) setJsonText(JSON.stringify(synced, null, 2));
        form.reset({ configuration: synced }, { keepDefaultValues: true });
    }

    return (
        <div className="flex flex-col gap-3">
            {jsonText === null ? (
                <JsonSchemaForm schema={schema} control={form.control} name="configuration" disabled={disabled} />
            ) : (
                <div className="flex flex-col gap-1.5">
                    {/* Sized by the editor itself: graphene ships a fixed set of Tailwind utilities, and a height class it
                        does not use itself (such as h-48) does not exist. */}
                    <div className="overflow-hidden rounded-md border">
                        <CodeEditor
                            value={jsonText}
                            onChange={value => editJson(value ?? '')}
                            language="json"
                            height="12rem"
                            disabled={disabled}
                        />
                    </div>
                    {jsonInvalid ? <p className="text-xs text-destructive">The settings are not valid JSON.</p> : null}
                    {/* The form shows which field is wrong; the JSON editor cannot, so it says why the source cannot be saved. */}
                    {!jsonInvalid && !settingsValid ? (
                        <p className="text-xs text-destructive">Some settings are missing or not valid for this source type.</p>
                    ) : null}
                </div>
            )}
            <div className="flex items-center gap-2">
                <Switch id={jsonSwitchId} checked={jsonText !== null} onCheckedChange={toggleJson} disabled={disabled || jsonInvalid} />
                <Label htmlFor={jsonSwitchId} className="font-normal">
                    Edit as JSON
                </Label>
            </div>
            {secretsCleared ? (
                <Alert>
                    <AlertCircleIcon />
                    <AlertDescription>
                        The address changed, so the saved secrets were cleared. Re-enter them: a secret is only kept for the address it was
                        saved for.
                    </AlertDescription>
                </Alert>
            ) : null}
        </div>
    );
}

function settingsDefaults(schema: JsonSchema): Record<string, unknown> {
    return (extractDefaults(schema) ?? {}) as Record<string, unknown>;
}

/**
 * The stored settings as the settings form shows them. The server leaves settings that are false out: a switch it left
 * out is off, whatever its default. An address it left out stays out: the server compares addresses as stored, and one
 * filled in by its default would be another, for which it refuses a masked secret. Any other setting it left out takes
 * its default, as the form fills it in.
 */
function storedSettings(schema: JsonSchema, stored: Record<string, unknown>): Record<string, unknown> {
    const defaults = Object.entries(settingsDefaults(schema)).filter(([key]) => !isAddressField(key));
    const switchesLeftOut = Object.entries(schema.properties ?? {})
        .filter(([key, property]) => typeof property === 'object' && property.type === 'boolean' && !(key in stored))
        .map(([key]) => [key, false]);
    return { ...Object.fromEntries(defaults), ...Object.fromEntries(switchesLeftOut), ...stored };
}

function parseConfiguration(text: string): Record<string, unknown> | null {
    try {
        const parsed: unknown = JSON.parse(text);
        return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, unknown>) : null;
    } catch {
        return null;
    }
}
