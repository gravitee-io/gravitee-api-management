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
    Combobox,
    ComboboxChip,
    ComboboxChips,
    ComboboxChipsInput,
    ComboboxContent,
    ComboboxEmpty,
    ComboboxItem,
    ComboboxList,
    Field,
    FieldLabel,
    Input,
    ScrollArea,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    useComboboxAnchor,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useMemo, useState } from 'react';

import type {
    EntrypointMappingRow,
    EntrypointTarget,
    NewEntrypointPayload,
    OrgEnvironment,
    OrgTag,
    UpdateEntrypointPayload,
} from '../types/entrypoint';
import { buildSheetSelectOptions, submitEntrypointForm } from '../utils/buildEntrypointPayload';
import {
    composeEntrypointValue,
    decomposeEntrypointValue,
    EMPTY_ENTRYPOINT_FORM,
    findDuplicateMapping,
    isEntrypointFormDirty,
    isEntrypointFormValid,
    isValidEntrypointHttpUrl,
    isValidKafkaDomain,
    isValidPort,
    KAFKA_DOMAIN_PLACEHOLDER,
    type EntrypointFormValues,
} from '../utils/entrypointForm';
import { displayNameFor } from '../utils/targetDisplayNames';

export type EntrypointSheetMode = 'create' | 'edit';

interface MultiSelectFieldProps {
    id: string;
    label: string;
    required?: boolean;
    placeholder: string;
    hint?: string;
    options: { id: string; name: string }[];
    selectedIds: string[];
    onChange: (ids: string[]) => void;
    disabled?: boolean;
}

function MultiSelectField({ id, label, required, placeholder, hint, options, selectedIds, onChange, disabled }: MultiSelectFieldProps) {
    const anchorRef = useComboboxAnchor();
    const sortedOptions = useMemo(() => [...options].sort((a, b) => a.name.localeCompare(b.name)), [options]);
    const selectedOptions = useMemo(() => sortedOptions.filter(option => selectedIds.includes(option.id)), [sortedOptions, selectedIds]);

    return (
        <Field orientation="vertical" className="gap-1.5">
            <FieldLabel htmlFor={id}>
                {label}
                {required && (
                    <span className="text-destructive" aria-hidden>
                        {' '}
                        *
                    </span>
                )}
            </FieldLabel>
            <Combobox
                multiple
                value={selectedIds}
                onValueChange={value => onChange(Array.isArray(value) ? value : [value])}
                disabled={disabled}
            >
                <ComboboxChips ref={anchorRef}>
                    {selectedOptions.map(option => (
                        <ComboboxChip key={option.id} removeAriaLabel={`Remove ${option.name}`}>
                            {option.name}
                        </ComboboxChip>
                    ))}
                    <ComboboxChipsInput id={id} placeholder={selectedIds.length === 0 ? placeholder : ''} readOnly />
                </ComboboxChips>
                <ComboboxContent anchor={anchorRef} align="start">
                    <ComboboxList>
                        {sortedOptions.length === 0 ? <ComboboxEmpty>No options available</ComboboxEmpty> : null}
                        {sortedOptions.map(option => (
                            <ComboboxItem key={option.id} value={option.id}>
                                {option.name}
                            </ComboboxItem>
                        ))}
                    </ComboboxList>
                </ComboboxContent>
            </Combobox>
            {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
        </Field>
    );
}

interface EntrypointSheetProps {
    open: boolean;
    mode: EntrypointSheetMode;
    target: EntrypointTarget;
    entrypoint?: EntrypointMappingRow;
    tags: OrgTag[];
    environments: OrgEnvironment[];
    defaultForm?: Partial<EntrypointFormValues>;
    existingRows: EntrypointMappingRow[];
    onClose: () => void;
    onSubmit: (data: NewEntrypointPayload | UpdateEntrypointPayload) => void;
    isSaving: boolean;
}

interface InitialSnapshot {
    form: EntrypointFormValues;
    tagKeys: string[];
    environmentIds: string[];
}

export function EntrypointSheet(props: EntrypointSheetProps) {
    const { open, mode, target, entrypoint, tags, environments, defaultForm, existingRows, onClose, onSubmit, isSaving } = props;
    const [form, setForm] = useState<EntrypointFormValues>(EMPTY_ENTRYPOINT_FORM);
    const [tagKeys, setTagKeys] = useState<string[]>([]);
    const [environmentIds, setEnvironmentIds] = useState<string[]>([]);
    const [initial, setInitial] = useState<InitialSnapshot | null>(null);

    useEffect(() => {
        if (!open) return;
        if (mode === 'edit' && entrypoint) {
            const initialForm = decomposeEntrypointValue(target, entrypoint.value);
            setForm(initialForm);
            setTagKeys(entrypoint.tags);
            setEnvironmentIds(entrypoint.environmentIds);
            setInitial({ form: initialForm, tagKeys: entrypoint.tags, environmentIds: entrypoint.environmentIds });
        } else {
            setForm({ ...EMPTY_ENTRYPOINT_FORM, ...defaultForm });
            setTagKeys([]);
            setEnvironmentIds([]);
            setInitial(null);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, mode, entrypoint, target]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    function setField<K extends keyof EntrypointFormValues>(key: K, value: EntrypointFormValues[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    const composedValue = useMemo(() => composeEntrypointValue(target, form), [target, form]);
    const isValueValid = isEntrypointFormValid(target, form);
    const tagsValid = tagKeys.length > 0;

    const duplicate = useMemo(() => {
        if (!isValueValid) return undefined;
        return findDuplicateMapping(target, composedValue, environmentIds, existingRows, entrypoint?.id);
    }, [isValueValid, target, composedValue, environmentIds, existingRows, entrypoint]);

    const isValid = isValueValid && tagsValid && !duplicate;
    const hasChanged = isEntrypointFormDirty(target, { form, tagKeys, environmentIds }, initial);
    const canSubmit = isValid && hasChanged;
    const targetLabel = displayNameFor(target);
    const { tagOptions, environmentOptions } = useMemo(() => buildSheetSelectOptions(tags, environments), [tags, environments]);

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>
                        {mode === 'create' ? 'Add' : 'Edit'} {targetLabel} Mapping
                    </SheetTitle>
                    <SheetDescription>
                        Establish a mapping between an entrypoint and sharding tags. This mapping will be used on the portal to display
                        different entrypoints based on API tags.
                    </SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <div className="flex flex-col gap-5 px-1 py-4">
                        {target === 'HTTP' && (
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="entrypoint-http-value">
                                    Entrypoint URL{' '}
                                    <span className="text-destructive" aria-hidden>
                                        *
                                    </span>
                                </FieldLabel>
                                <Input
                                    id="entrypoint-http-value"
                                    type="url"
                                    value={form.httpValue}
                                    onChange={e => setField('httpValue', e.target.value)}
                                    placeholder="https://api.example.com"
                                    disabled={isSaving}
                                    aria-invalid={form.httpValue.trim() !== '' && !isValidEntrypointHttpUrl(form.httpValue)}
                                />
                                {form.httpValue.trim() !== '' && !isValidEntrypointHttpUrl(form.httpValue) ? (
                                    <p className="text-sm text-destructive" role="alert">
                                        Enter a valid URL (http:// or https://).
                                    </p>
                                ) : null}
                            </Field>
                        )}

                        {target === 'TCP' && (
                            <Field orientation="vertical" className="gap-1.5">
                                <FieldLabel htmlFor="entrypoint-tcp-port">
                                    TCP Port{' '}
                                    <span className="text-destructive" aria-hidden>
                                        *
                                    </span>
                                </FieldLabel>
                                <Input
                                    id="entrypoint-tcp-port"
                                    type="number"
                                    min={1}
                                    max={65535}
                                    value={form.tcpPort}
                                    onChange={e => setField('tcpPort', e.target.value)}
                                    placeholder="4082"
                                    disabled={isSaving}
                                    aria-invalid={form.tcpPort.trim() !== '' && !isValidPort(form.tcpPort)}
                                />
                                {form.tcpPort.trim() !== '' && !isValidPort(form.tcpPort) ? (
                                    <p className="text-sm text-destructive" role="alert">
                                        Port must be a number between 1 and 65535.
                                    </p>
                                ) : null}
                            </Field>
                        )}

                        {target === 'KAFKA' && (
                            <>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="entrypoint-kafka-domain">
                                        Kafka Bootstrap Domain Pattern{' '}
                                        <span className="text-destructive" aria-hidden>
                                            *
                                        </span>
                                    </FieldLabel>
                                    <Input
                                        id="entrypoint-kafka-domain"
                                        value={form.kafkaDomain}
                                        onChange={e => setField('kafkaDomain', e.target.value)}
                                        placeholder={KAFKA_DOMAIN_PLACEHOLDER}
                                        disabled={isSaving}
                                        aria-invalid={form.kafkaDomain.trim() !== '' && !isValidKafkaDomain(form.kafkaDomain)}
                                    />
                                    <p className="text-xs text-muted-foreground">Must contain {KAFKA_DOMAIN_PLACEHOLDER} placeholder.</p>
                                </Field>
                                <Field orientation="vertical" className="gap-1.5">
                                    <FieldLabel htmlFor="entrypoint-kafka-port">
                                        Kafka Port{' '}
                                        <span className="text-destructive" aria-hidden>
                                            *
                                        </span>
                                    </FieldLabel>
                                    <Input
                                        id="entrypoint-kafka-port"
                                        type="number"
                                        min={1}
                                        max={65535}
                                        value={form.kafkaPort}
                                        onChange={e => setField('kafkaPort', e.target.value)}
                                        placeholder="9092"
                                        disabled={isSaving}
                                        aria-invalid={form.kafkaPort.trim() !== '' && !isValidPort(form.kafkaPort)}
                                    />
                                    {form.kafkaPort.trim() !== '' && !isValidPort(form.kafkaPort) ? (
                                        <p className="text-sm text-destructive" role="alert">
                                            Port must be a number between 1 and 65535.
                                        </p>
                                    ) : null}
                                </Field>
                            </>
                        )}

                        <MultiSelectField
                            id="entrypoint-tags"
                            label="Sharding Tags"
                            required
                            placeholder="Select sharding tags"
                            options={tagOptions}
                            selectedIds={tagKeys}
                            onChange={setTagKeys}
                            disabled={isSaving}
                        />

                        <MultiSelectField
                            id="entrypoint-environments"
                            label="Environments"
                            placeholder="All environments"
                            hint="Leave empty to apply to all environments."
                            options={environmentOptions}
                            selectedIds={environmentIds}
                            onChange={setEnvironmentIds}
                            disabled={isSaving}
                        />

                        {duplicate ? (
                            <p className="text-sm text-destructive" role="alert">
                                An entrypoint mapping with this value already exists for an overlapping environment.
                            </p>
                        ) : null}
                    </div>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-col gap-2 border-t pt-4 sm:flex-col">
                    <Button type="button" variant="outline" className="w-full" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button
                        type="button"
                        className="w-full"
                        onClick={() =>
                            submitEntrypointForm(canSubmit, onSubmit, entrypoint?.id, target, composedValue, tagKeys, environmentIds)
                        }
                        disabled={!canSubmit || isSaving}
                    >
                        {isSaving ? 'Saving...' : mode === 'create' ? 'Add Mapping' : 'Save Changes'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
