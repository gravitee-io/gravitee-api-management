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
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
    Textarea,
} from '@gravitee/graphene-core';
import { useEffect, useState, type FormEvent } from 'react';

import { STANDARD_SHEET_WIDTH } from '../../../shared/layout/sheetLayout';
import {
    QUALITY_RULE_DESCRIPTION_MAX_LENGTH,
    QUALITY_RULE_NAME_MAX_LENGTH,
    type QualityRule,
    type QualityRuleWrite,
} from '../types/qualityRule';

const EMPTY_FORM: QualityRuleWrite = { name: '', description: '' };

function RequiredMark() {
    return (
        <span className="text-destructive" aria-hidden>
            *
        </span>
    );
}

export function QualityRuleSheet({
    open,
    rule,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    /** Present in edit mode; absent when creating. */
    rule?: QualityRule;
    onClose: () => void;
    onSubmit: (write: QualityRuleWrite) => void;
    isSaving: boolean;
}>) {
    const isEdit = rule !== undefined;
    const [form, setForm] = useState<QualityRuleWrite>(EMPTY_FORM);

    useEffect(() => {
        if (!open) return;
        setForm(rule ? { name: rule.name, description: rule.description } : EMPTY_FORM);
    }, [open, rule]);

    const trimmed: QualityRuleWrite = { name: form.name.trim(), description: form.description.trim() };
    const isValid = trimmed.name !== '' && trimmed.description !== '';
    const hasChanged = !rule || trimmed.name !== rule.name || trimmed.description !== rule.description;

    function handleSubmit(event: FormEvent) {
        event.preventDefault();
        if (!isValid || !hasChanged) return;
        onSubmit(trimmed);
    }

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>{isEdit ? 'Edit manual rule' : 'New manual rule'}</SheetTitle>
                    <SheetDescription>Reviewers check these rules when they accept or reject an API.</SheetDescription>
                </SheetHeader>

                <ScrollArea className="flex-1 min-h-0">
                    <form id="quality-rule-form" onSubmit={handleSubmit} className="flex flex-col gap-5 px-1 py-4">
                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="quality-rule-name">
                                Rule name <RequiredMark />
                            </FieldLabel>
                            <Input
                                id="quality-rule-name"
                                value={form.name}
                                maxLength={QUALITY_RULE_NAME_MAX_LENGTH}
                                onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
                                disabled={isSaving}
                                required
                            />
                            <p className="text-xs text-muted-foreground">
                                {form.name.length}/{QUALITY_RULE_NAME_MAX_LENGTH}
                            </p>
                        </Field>

                        <Field orientation="vertical" className="gap-1.5">
                            <FieldLabel htmlFor="quality-rule-description">
                                Description <RequiredMark />
                            </FieldLabel>
                            <Textarea
                                id="quality-rule-description"
                                value={form.description}
                                maxLength={QUALITY_RULE_DESCRIPTION_MAX_LENGTH}
                                rows={4}
                                onChange={e => setForm(prev => ({ ...prev, description: e.target.value }))}
                                disabled={isSaving}
                                required
                            />
                            <p className="text-xs text-muted-foreground">
                                {form.description.length}/{QUALITY_RULE_DESCRIPTION_MAX_LENGTH}
                            </p>
                        </Field>
                    </form>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="submit" form="quality-rule-form" disabled={!isValid || !hasChanged || isSaving}>
                        {isSaving ? 'Saving…' : isEdit ? 'Save changes' : 'Create rule'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
