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
import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';

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

function buildEmptyForm(): SharedPolicyGroupCreateFormValues {
    return {
        name: '',
        description: '',
        prerequisiteMessage: '',
        apiType: DEFAULT_API_TYPE,
        phase: PHASE_BY_API_TYPE[DEFAULT_API_TYPE][0],
    };
}

export function SharedPolicyGroupCreateSheet({
    open,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    onClose: () => void;
    onSubmit: (values: SharedPolicyGroupCreateFormValues) => void;
    isSaving: boolean;
}>) {
    const [form, setForm] = useState<SharedPolicyGroupCreateFormValues>(buildEmptyForm);
    const phases = useMemo(() => PHASE_BY_API_TYPE[form.apiType], [form.apiType]);

    useEffect(() => {
        if (!open) return;
        setForm(buildEmptyForm());
    }, [open]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    function setField<K extends keyof SharedPolicyGroupCreateFormValues>(key: K, value: SharedPolicyGroupCreateFormValues[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
    }

    function handleApiTypeChange(apiType: ApiType) {
        setForm(prev => ({ ...prev, apiType, phase: PHASE_BY_API_TYPE[apiType][0] }));
    }

    const isValid = form.name.trim() !== '';

    function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!isValid) return;
        onSubmit({ ...form, name: form.name.trim() });
    }

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Add Policy Group</SheetTitle>
                    <SheetDescription>
                        Policy groups can be reused across multiple APIs, so you only need to configure their policies once.
                    </SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <form id="shared-policy-group-create-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
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
                                disabled={isSaving}
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
                                disabled={isSaving}
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
                                disabled={isSaving}
                            />
                        </Field>

                        <Separator />

                        <h3 className="text-sm font-semibold">Scope</h3>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel>
                                API Type{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <ToggleGroup
                                type="single"
                                value={form.apiType}
                                onValueChange={value => {
                                    if (value) handleApiTypeChange(value as ApiType);
                                }}
                                disabled={isSaving}
                            >
                                {CREATABLE_API_TYPES.map(apiType => (
                                    <ToggleGroupItem key={apiType} value={apiType}>
                                        {toReadableApiType(apiType)}
                                    </ToggleGroupItem>
                                ))}
                            </ToggleGroup>
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel>
                                Phase{' '}
                                <span className="text-destructive" aria-hidden>
                                    *
                                </span>
                            </FieldLabel>
                            <ToggleGroup
                                type="single"
                                value={form.phase}
                                onValueChange={value => {
                                    if (value) setField('phase', value as FlowPhase);
                                }}
                                disabled={isSaving}
                            >
                                {phases.map(phase => (
                                    <ToggleGroupItem key={phase} value={phase}>
                                        {toReadableFlowPhase(phase)}
                                    </ToggleGroupItem>
                                ))}
                            </ToggleGroup>
                        </Field>
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="shared-policy-group-create-form" disabled={!isValid || isSaving}>
                        {isSaving ? 'Creating…' : 'Create'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
