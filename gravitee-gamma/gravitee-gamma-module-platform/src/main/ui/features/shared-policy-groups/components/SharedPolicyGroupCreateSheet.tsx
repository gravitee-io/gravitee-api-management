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
    Field,
    FieldLabel,
    Input,
    ScrollArea,
    Separator,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Textarea,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { useState, type FormEvent } from 'react';

import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import { toReadableApiType, toReadableFlowPhase, type ApiType, type FlowPhase } from '../types/sharedPolicyGroup';
import { PHASE_BY_API_TYPE } from '../utils/sharedPolicyGroupPayload';

const CREATABLE_API_TYPES: readonly ApiType[] = ['PROXY', 'MESSAGE'];
const DEFAULT_API_TYPE: ApiType = 'PROXY';
const DESCRIPTION_MAX_LENGTH = 300;
const PREREQUISITE_MESSAGE_MAX_LENGTH = 300;
const PREREQUISITE_MESSAGE_PLACEHOLDER =
    'Message displayed when using SPG in Policy Studio. e.g.: "The resource cache "my-cache" is required"....';

export interface SharedPolicyGroupCreateFormValues {
    name: string;
    description: string;
    prerequisiteMessage: string;
    apiType: ApiType;
    phase: FlowPhase;
}

type SetFormField = <K extends keyof SharedPolicyGroupCreateFormValues>(key: K, value: SharedPolicyGroupCreateFormValues[K]) => void;

function buildEmptyForm(): SharedPolicyGroupCreateFormValues {
    return {
        name: '',
        description: '',
        prerequisiteMessage: '',
        apiType: DEFAULT_API_TYPE,
        phase: PHASE_BY_API_TYPE[DEFAULT_API_TYPE][0],
    };
}

function BasicInformationFields({
    form,
    disabled,
    setField,
}: Readonly<{
    form: SharedPolicyGroupCreateFormValues;
    disabled: boolean;
    setField: SetFormField;
}>) {
    return (
        <>
            <h3 className="text-sm font-semibold">Basic information</h3>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="spg-name">
                    Name{' '}
                    <span className="text-destructive" aria-hidden>
                        *
                    </span>
                </FieldLabel>
                <Input
                    id="spg-name"
                    value={form.name}
                    onChange={e => setField('name', e.target.value)}
                    placeholder="e.g. Default authentication"
                    maxLength={512}
                    disabled={disabled}
                    required
                />
            </Field>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="spg-description">Describe the purpose of this policy group</FieldLabel>
                <p className="text-xs text-muted-foreground">{DESCRIPTION_MAX_LENGTH} characters max.</p>
                <Textarea
                    id="spg-description"
                    value={form.description}
                    onChange={e => setField('description', e.target.value)}
                    placeholder="Describe what this policy group is used for"
                    maxLength={DESCRIPTION_MAX_LENGTH}
                    disabled={disabled}
                />
            </Field>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel htmlFor="spg-prerequisite-message">Prerequisite message</FieldLabel>
                <p className="text-xs text-muted-foreground">{PREREQUISITE_MESSAGE_MAX_LENGTH} characters max.</p>
                <Textarea
                    id="spg-prerequisite-message"
                    value={form.prerequisiteMessage}
                    onChange={e => setField('prerequisiteMessage', e.target.value)}
                    placeholder={PREREQUISITE_MESSAGE_PLACEHOLDER}
                    maxLength={PREREQUISITE_MESSAGE_MAX_LENGTH}
                    disabled={disabled}
                />
            </Field>
        </>
    );
}

function ScopeFields({
    form,
    disabled,
    setField,
    onApiTypeChange,
}: Readonly<{
    form: SharedPolicyGroupCreateFormValues;
    disabled: boolean;
    setField: SetFormField;
    onApiTypeChange: (apiType: ApiType) => void;
}>) {
    return (
        <>
            <Separator />
            <h3 className="text-sm font-semibold">Scope</h3>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel id="spg-api-type-label">
                    API Type{' '}
                    <span className="text-destructive" aria-hidden>
                        *
                    </span>
                </FieldLabel>
                <ToggleGroup
                    type="single"
                    value={form.apiType}
                    onValueChange={value => value && onApiTypeChange(value as ApiType)}
                    disabled={disabled}
                    aria-labelledby="spg-api-type-label"
                >
                    {CREATABLE_API_TYPES.map(apiType => (
                        <ToggleGroupItem key={apiType} value={apiType}>
                            {toReadableApiType(apiType)}
                        </ToggleGroupItem>
                    ))}
                </ToggleGroup>
            </Field>

            <Field orientation="vertical" className="gap-1.5">
                <FieldLabel id="spg-phase-label">
                    Phase{' '}
                    <span className="text-destructive" aria-hidden>
                        *
                    </span>
                </FieldLabel>
                <ToggleGroup
                    type="single"
                    value={form.phase}
                    onValueChange={value => value && setField('phase', value as FlowPhase)}
                    disabled={disabled}
                    aria-labelledby="spg-phase-label"
                >
                    {PHASE_BY_API_TYPE[form.apiType].map(phase => (
                        <ToggleGroupItem key={phase} value={phase}>
                            {toReadableFlowPhase(phase)}
                        </ToggleGroupItem>
                    ))}
                </ToggleGroup>
            </Field>
        </>
    );
}

interface SharedPolicyGroupCreateSheetProps {
    readonly open: boolean;
    readonly onClose: () => void;
    readonly onSubmit: (values: SharedPolicyGroupCreateFormValues) => Promise<void> | void;
    readonly isSaving?: boolean;
}

export function SharedPolicyGroupCreateSheet({ open, onClose, onSubmit, isSaving = false }: SharedPolicyGroupCreateSheetProps) {
    const [form, setForm] = useState<SharedPolicyGroupCreateFormValues>(buildEmptyForm);

    const setField: SetFormField = (key, value) => setForm(previous => ({ ...previous, [key]: value }));
    const isValid = form.name.trim() !== '';

    function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (isValid && !isSaving) {
            void onSubmit({ ...form, name: form.name.trim() });
        }
    }

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Add Shared Policy Group</SheetTitle>
                    <SheetDescription>
                        Policy groups can be reused across multiple APIs, so you only need to configure their policies once.
                    </SheetDescription>
                </SheetHeader>

                <form onSubmit={handleSubmit} className="flex min-h-0 flex-1 flex-col">
                    <ScrollArea className="min-h-0 flex-1">
                        <div className="flex flex-col gap-5 px-4 py-4">
                            <BasicInformationFields form={form} disabled={isSaving} setField={setField} />
                            <ScopeFields
                                form={form}
                                disabled={isSaving}
                                setField={setField}
                                onApiTypeChange={apiType =>
                                    setForm(previous => ({ ...previous, apiType, phase: PHASE_BY_API_TYPE[apiType][0] }))
                                }
                            />
                        </div>
                    </ScrollArea>

                    <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                        <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={!isValid || isSaving}>
                            {isSaving ? 'Creating…' : 'Create'}
                        </Button>
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    );
}
