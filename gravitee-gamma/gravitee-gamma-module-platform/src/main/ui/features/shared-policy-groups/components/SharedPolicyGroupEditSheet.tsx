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

import { Button, ScrollArea, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { useCallback, useEffect, useState, type FormEvent } from 'react';

import { SharedPolicyGroupBasicFields } from './SharedPolicyGroupBasicFields';
import { STANDARD_SHEET_WIDTH } from '../../applications/components/sheetLayout';
import { toReadableApiType, toReadableFlowPhase, type SharedPolicyGroup } from '../types/sharedPolicyGroup';
import type { SharedPolicyGroupBasicFormValues } from '../utils/sharedPolicyGroupPayload';

export type SharedPolicyGroupEditFormValues = SharedPolicyGroupBasicFormValues;

function toFormValues(sharedPolicyGroup: SharedPolicyGroup): SharedPolicyGroupEditFormValues {
    return {
        name: sharedPolicyGroup.name,
        description: sharedPolicyGroup.description ?? '',
        prerequisiteMessage: sharedPolicyGroup.prerequisiteMessage ?? '',
    };
}

export function SharedPolicyGroupEditSheet({
    open,
    sharedPolicyGroup,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    sharedPolicyGroup: SharedPolicyGroup | null;
    onClose: () => void;
    onSubmit: (values: SharedPolicyGroupEditFormValues) => void;
    isSaving: boolean;
}>) {
    const [form, setForm] = useState<SharedPolicyGroupEditFormValues>({
        name: '',
        description: '',
        prerequisiteMessage: '',
    });

    useEffect(() => {
        if (!open || !sharedPolicyGroup) return;
        setForm(toFormValues(sharedPolicyGroup));
    }, [open, sharedPolicyGroup]);

    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    function setField<K extends keyof SharedPolicyGroupEditFormValues>(key: K, value: SharedPolicyGroupEditFormValues[K]) {
        setForm(prev => ({ ...prev, [key]: value }));
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
                    <SheetTitle>Edit Policy Group</SheetTitle>
                    <SheetDescription>Update the name, description, and prerequisite message for this policy group.</SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <form id="shared-policy-group-edit-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-4 py-4">
                        <SharedPolicyGroupBasicFields
                            idPrefix="spg-edit"
                            values={form}
                            disabled={isSaving}
                            onChange={(key, value) => setField(key, value)}
                        />

                        {sharedPolicyGroup && (
                            <dl className="grid grid-cols-2 gap-4 rounded-lg border bg-muted/30 p-3">
                                <div className="space-y-1">
                                    <dt className="text-xs font-medium text-muted-foreground">API type</dt>
                                    <dd className="text-sm">{toReadableApiType(sharedPolicyGroup.apiType)}</dd>
                                </div>
                                <div className="space-y-1">
                                    <dt className="text-xs font-medium text-muted-foreground">Phase</dt>
                                    <dd className="text-sm">{toReadableFlowPhase(sharedPolicyGroup.phase)}</dd>
                                </div>
                            </dl>
                        )}
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="shared-policy-group-edit-form" disabled={!isValid || isSaving}>
                        {isSaving ? 'Saving…' : 'Save'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
