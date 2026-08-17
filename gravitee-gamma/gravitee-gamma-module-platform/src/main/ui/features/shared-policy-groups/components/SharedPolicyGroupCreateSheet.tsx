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
    ScrollArea,
    Separator,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    ToggleGroup,
    ToggleGroupItem,
} from '@gravitee/graphene-core';
import { useActionState, useState } from 'react';

import { SharedPolicyGroupBasicFields } from './SharedPolicyGroupBasicFields';
import { FormActionSubmitButton } from '../../../shared/components/FormActionSubmitButton';
import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import { toReadableApiType, toReadableFlowPhase, type ApiType, type FlowPhase } from '../types/sharedPolicyGroup';
import { PHASE_BY_API_TYPE, type SharedPolicyGroupBasicFormValues } from '../utils/sharedPolicyGroupPayload';

const CREATABLE_API_TYPES: readonly ApiType[] = ['PROXY', 'MESSAGE'];
const DEFAULT_API_TYPE: ApiType = 'PROXY';

export interface SharedPolicyGroupCreateFormValues extends SharedPolicyGroupBasicFormValues {
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
                <FieldLabel id="spg-api-type-label" required>
                    API Type
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
                <FieldLabel id="spg-phase-label" required>
                    Phase
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
}

export function SharedPolicyGroupCreateSheet({ open, onClose, onSubmit }: SharedPolicyGroupCreateSheetProps) {
    const [form, setForm] = useState<SharedPolicyGroupCreateFormValues>(buildEmptyForm);

    const setField: SetFormField = (key, value) => setForm(previous => ({ ...previous, [key]: value }));
    const isValid = form.name.trim() !== '';
    const [, submitSharedPolicyGroup, isSaving] = useActionState<null, FormData>(async () => {
        if (isValid) {
            await onSubmit({ ...form, name: form.name.trim() });
        }
        return null;
    }, null);

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && !isSaving && onClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Add Shared Policy Group</SheetTitle>
                    <SheetDescription>
                        Policy groups can be reused across multiple APIs, so you only need to configure their policies once.
                    </SheetDescription>
                </SheetHeader>

                <form action={submitSharedPolicyGroup} className="flex min-h-0 flex-1 flex-col">
                    <ScrollArea className="min-h-0 flex-1">
                        <div className="flex flex-col gap-5 px-4 py-4">
                            <SharedPolicyGroupBasicFields idPrefix="spg" values={form} disabled={isSaving} onChange={setField} />
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
                        <FormActionSubmitButton disabled={!isValid} label="Create" pendingLabel="Creating…" />
                    </SheetFooter>
                </form>
            </SheetContent>
        </Sheet>
    );
}
