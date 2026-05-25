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
    Badge,
    Button,
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { PencilIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useState } from 'react';

import type { MaskingType, RedactionRule } from '../../../types';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const PREVIEW_SAMPLE = 'ABCDEFGHIJ1234';

function buildPartialPreview(prefix: number, suffix: number, maskChar: string): string {
    const char = maskChar || '*';
    const len = PREVIEW_SAMPLE.length;
    if (prefix + suffix >= len) return PREVIEW_SAMPLE;
    return PREVIEW_SAMPLE.slice(0, prefix) + char.repeat(len - prefix - suffix) + PREVIEW_SAMPLE.slice(len - suffix);
}

function maskingDescription(rule: RedactionRule): string {
    const s = rule.maskingStrategy;
    if (!s || s.type === 'FULL') {
        return `Replace with ${s?.replacement ?? '[REDACTED]'}`;
    }
    const prefix = s.prefixLength ?? 0;
    const suffix = s.suffixLength ?? 0;
    const char = s.replacement ?? '*';
    return `Keep ${prefix}+${suffix} chars, mask with '${char}'`;
}

// ─── Dialog form state ────────────────────────────────────────────────────────

interface DialogForm {
    pattern: string;
    maskingType: MaskingType;
    fullReplacement: string;
    prefixLength: number;
    suffixLength: number;
    maskChar: string;
    valuePattern: string;
    patternError: string | null;
}

const DEFAULT_FORM: DialogForm = {
    pattern: '',
    maskingType: 'FULL',
    fullReplacement: '',
    prefixLength: 0,
    suffixLength: 0,
    maskChar: '*',
    valuePattern: '',
    patternError: null,
};

function formFromRule(rule: RedactionRule): DialogForm {
    const s = rule.maskingStrategy;
    const isPartial = s?.type === 'PARTIAL';
    return {
        pattern: rule.attributeNamePattern,
        maskingType: s?.type ?? 'FULL',
        fullReplacement: !isPartial ? (s?.replacement ?? '') : '',
        prefixLength: s?.prefixLength ?? 0,
        suffixLength: s?.suffixLength ?? 0,
        maskChar: isPartial ? (s?.replacement ?? '*') : '*',
        valuePattern: rule.valuePattern ?? '',
        patternError: null,
    };
}

// ─── Rule dialog ──────────────────────────────────────────────────────────────

interface RuleDialogProps {
    open: boolean;
    isEdit: boolean;
    form: DialogForm;
    onFormChange: (f: DialogForm) => void;
    onSave: () => void;
    onClose: () => void;
}

function RuleDialog({ open, isEdit, form, onFormChange, onSave, onClose }: RuleDialogProps) {
    const set = (partial: Partial<DialogForm>) => onFormChange({ ...form, ...partial });
    const isPartial = form.maskingType === 'PARTIAL';
    const preview = isPartial ? buildPartialPreview(form.prefixLength, form.suffixLength, form.maskChar) : null;

    return (
        <Dialog open={open} onOpenChange={open => !open && onClose()}>
            <DialogContent showCloseButton>
                <DialogHeader>
                    <DialogTitle>{isEdit ? 'Edit redaction rule' : 'New redaction rule'}</DialogTitle>
                </DialogHeader>

                <div className="space-y-2 py-1">
                    {/* Attribute name pattern */}
                    <div className="space-y-2">
                        <Label htmlFor="rd-pattern">
                            Attribute Name Pattern{' '}
                            <span className="text-destructive" aria-hidden>
                                *
                            </span>
                        </Label>
                        <Input
                            id="rd-pattern"
                            value={form.pattern}
                            onChange={e => set({ pattern: e.target.value, patternError: null })}
                            placeholder="e.g. api-key"
                        />
                        {form.patternError && <p className="text-xs text-destructive">{form.patternError}</p>}
                        <p className="text-xs text-muted-foreground">
                            Short name matches any namespace · <code>*</code> = one segment · <code>**</code> = any depth · prefix with{' '}
                            <code>regex:</code> for exact match
                        </p>
                    </div>

                    {/* Masking type */}
                    <div className="space-y-2">
                        <Label htmlFor="rd-masking">Masking Type</Label>
                        <Select value={form.maskingType} onValueChange={(v: MaskingType) => set({ maskingType: v })}>
                            <SelectTrigger id="rd-masking">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="FULL">Full Mask — replace entire value</SelectItem>
                                <SelectItem value="PARTIAL">Partial Mask — keep prefix / suffix visible</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>

                    {/* Full: replacement text */}
                    {!isPartial && (
                        <div className="space-y-2">
                            <Label htmlFor="rd-replacement">Replacement Text</Label>
                            <Input
                                id="rd-replacement"
                                value={form.fullReplacement}
                                onChange={e => set({ fullReplacement: e.target.value })}
                                placeholder="[REDACTED]"
                            />
                            <p className="text-xs text-muted-foreground">Leave blank to use the default: [REDACTED]</p>
                        </div>
                    )}

                    {/* Partial: prefix / suffix / mask char + preview */}
                    {isPartial && (
                        <div className="space-y-2">
                            <div className="grid grid-cols-3 gap-3">
                                <div className="space-y-2">
                                    <Label htmlFor="rd-prefix">Prefix chars</Label>
                                    <Input
                                        id="rd-prefix"
                                        type="number"
                                        min={0}
                                        value={form.prefixLength}
                                        onChange={e => set({ prefixLength: Math.max(0, Number(e.target.value) || 0) })}
                                    />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="rd-suffix">Suffix chars</Label>
                                    <Input
                                        id="rd-suffix"
                                        type="number"
                                        min={0}
                                        value={form.suffixLength}
                                        onChange={e => set({ suffixLength: Math.max(0, Number(e.target.value) || 0) })}
                                    />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="rd-maskchar">Mask char</Label>
                                    <Input
                                        id="rd-maskchar"
                                        value={form.maskChar}
                                        maxLength={1}
                                        onChange={e => set({ maskChar: e.target.value })}
                                        placeholder="*"
                                    />
                                </div>
                            </div>
                            <div className="rounded-md bg-muted px-3 py-2 text-xs">
                                Preview: <code>{preview}</code>
                            </div>
                        </div>
                    )}

                    {/* Value filter */}
                    <div className="space-y-2">
                        <Label htmlFor="rd-value-pattern">Value Filter (optional)</Label>
                        <Input
                            id="rd-value-pattern"
                            value={form.valuePattern}
                            onChange={e => set({ valuePattern: e.target.value })}
                            placeholder="e.g. ^Bearer "
                        />
                        <p className="text-xs text-muted-foreground">
                            Regex (partial match). Rule fires only when the attribute value matches.
                        </p>
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="ghost" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button onClick={onSave}>{isEdit ? 'Save' : 'Add'}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

// ─── Panel ────────────────────────────────────────────────────────────────────

export interface SpanRedactionRulesProps {
    rules: RedactionRule[];
    disabled?: boolean;
    onChange: (rules: RedactionRule[]) => void;
}

export function SpanRedactionRules({ rules, disabled, onChange }: SpanRedactionRulesProps) {
    const [dialogOpen, setDialogOpen] = useState(false);
    const [editIndex, setEditIndex] = useState<number | null>(null);
    const [form, setForm] = useState<DialogForm>(DEFAULT_FORM);

    const openAdd = () => {
        setEditIndex(null);
        setForm(DEFAULT_FORM);
        setDialogOpen(true);
    };

    const openEdit = (index: number) => {
        setEditIndex(index);
        setForm(formFromRule(rules[index]));
        setDialogOpen(true);
    };

    const closeDialog = () => {
        setDialogOpen(false);
        setEditIndex(null);
    };

    const removeRow = (index: number) => onChange(rules.filter((_, i) => i !== index));

    const existingPatterns = rules.filter((_, i) => i !== editIndex).map(r => r.attributeNamePattern);

    const handleSave = () => {
        const pattern = form.pattern.trim();
        if (!pattern) {
            setForm(f => ({ ...f, patternError: 'Pattern is required.' }));
            return;
        }
        if (existingPatterns.includes(pattern)) {
            setForm(f => ({ ...f, patternError: 'A rule with this pattern already exists.' }));
            return;
        }

        const isPartial = form.maskingType === 'PARTIAL';
        const rule: RedactionRule = {
            attributeNamePattern: pattern,
            maskingStrategy: isPartial
                ? {
                      type: 'PARTIAL',
                      prefixLength: form.prefixLength,
                      suffixLength: form.suffixLength,
                      replacement: form.maskChar || '*',
                  }
                : {
                      type: 'FULL',
                      ...(form.fullReplacement.trim() ? { replacement: form.fullReplacement.trim() } : {}),
                  },
            ...(form.valuePattern.trim() ? { valuePattern: form.valuePattern.trim() } : {}),
        };

        if (editIndex !== null) {
            onChange(rules.map((r, i) => (i === editIndex ? rule : r)));
        } else {
            onChange([...rules, rule]);
        }
        closeDialog();
    };

    return (
        <div className="space-y-3">
            <div className="flex items-center justify-between">
                <p className="text-sm font-medium">Span Attribute Redaction</p>
                {!disabled && (
                    <Button variant="outline" size="sm" onClick={openAdd}>
                        <PlusIcon className="size-3.5" aria-hidden />
                        Add rule
                    </Button>
                )}
            </div>

            <Alert>
                <AlertDescription>
                    Global redaction rules are always applied first. Rules defined here are API-specific and appended after them.
                </AlertDescription>
            </Alert>

            {rules.length === 0 ? (
                <p className="text-xs text-muted-foreground py-2">No redaction rules — span attributes are exported as-is.</p>
            ) : (
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead className="w-8">#</TableHead>
                            <TableHead>Attribute Pattern</TableHead>
                            <TableHead>Masking</TableHead>
                            <TableHead>Value Filter</TableHead>
                            {!disabled && <TableHead className="w-20" />}
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {rules.map((rule, i) => (
                            <TableRow key={rule.attributeNamePattern}>
                                <TableCell>
                                    <Badge variant="secondary">{i + 1}</Badge>
                                </TableCell>
                                <TableCell>
                                    <code className="text-xs">{rule.attributeNamePattern}</code>
                                </TableCell>
                                <TableCell className="text-xs">{maskingDescription(rule)}</TableCell>
                                <TableCell className="text-xs">
                                    {rule.valuePattern ? (
                                        <code>{rule.valuePattern}</code>
                                    ) : (
                                        <span className="text-muted-foreground">—</span>
                                    )}
                                </TableCell>
                                {!disabled && (
                                    <TableCell>
                                        <div className="flex gap-1">
                                            <Button
                                                variant="ghost"
                                                size="icon-sm"
                                                aria-label={`Edit rule ${i + 1}`}
                                                onClick={() => openEdit(i)}
                                            >
                                                <PencilIcon className="size-3.5" />
                                            </Button>
                                            <Button
                                                variant="ghost"
                                                size="icon-sm"
                                                aria-label={`Delete rule ${i + 1}`}
                                                onClick={() => removeRow(i)}
                                            >
                                                <Trash2Icon className="size-3.5 text-destructive" />
                                            </Button>
                                        </div>
                                    </TableCell>
                                )}
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            )}

            <RuleDialog
                open={dialogOpen}
                isEdit={editIndex !== null}
                form={form}
                onFormChange={setForm}
                onSave={handleSave}
                onClose={closeDialog}
            />
        </div>
    );
}
