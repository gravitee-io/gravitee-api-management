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
    CardHeader,
    CardTitle,
    Checkbox,
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    Input,
    Label,
    Popover,
    PopoverContent,
    PopoverTrigger,
    ScrollArea,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Switch,
    Textarea,
    cn,
} from '@gravitee/graphene-core';
import { CheckIcon, ChevronsUpDownIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useRef, useState } from 'react';

import { useGroups } from '../../../../hooks/useGroups';
import { useOrgTags } from '../../../../hooks/useOrgTags';
import type { GeneralFormData, PlanContext, PlanSecurityType } from '../../../../types/plan';

function CharacteristicsInput({ value, onChange, readOnly }: { value: string[]; onChange: (v: string[]) => void; readOnly?: boolean }) {
    const [input, setInput] = useState('');
    const inputRef = useRef<HTMLInputElement>(null);

    function addTag() {
        const trimmed = input.trim();
        if (trimmed && !value.includes(trimmed)) {
            onChange([...value, trimmed]);
        }
        setInput('');
    }

    function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
        if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();
            addTag();
        } else if (e.key === 'Backspace' && input === '' && value.length > 0) {
            onChange(value.slice(0, -1));
        }
    }

    return (
        <div
            className={cn(
                'flex flex-wrap gap-1.5 min-h-10 rounded-md border border-input bg-background px-3 py-2',
                readOnly ? 'cursor-not-allowed opacity-50' : 'cursor-text',
            )}
        >
            {value.map(tag => (
                <span
                    key={tag}
                    className="inline-flex items-center gap-1 rounded-md bg-secondary text-secondary-foreground text-xs font-medium px-2 py-0.5"
                >
                    {tag}
                    {!readOnly && (
                        <button
                            type="button"
                            onClick={e => {
                                e.stopPropagation();
                                onChange(value.filter(t => t !== tag));
                            }}
                            className="opacity-60 hover:opacity-100 hover:text-destructive"
                            aria-label={`Remove ${tag}`}
                        >
                            <XIcon className="size-3" aria-hidden />
                        </button>
                    )}
                </span>
            ))}
            {!readOnly && (
                <input
                    ref={inputRef}
                    value={input}
                    onChange={e => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onBlur={addTag}
                    className="flex-1 min-w-24 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
                    placeholder={value.length === 0 ? 'Type and press Enter…' : ''}
                    aria-label="Add characteristic"
                />
            )}
        </div>
    );
}

interface PlanGeneralStepProps {
    ctx: PlanContext;
    securityType: PlanSecurityType;
    value: GeneralFormData;
    onChange: (v: GeneralFormData) => void;
    errors: Partial<Record<keyof GeneralFormData, string>>;
    readOnly?: boolean;
}

export function PlanGeneralStep({ ctx, securityType, value, onChange, errors, readOnly = false }: Readonly<PlanGeneralStepProps>) {
    const [groupsOpen, setGroupsOpen] = useState(false);
    const [tagsOpen, setTagsOpen] = useState(false);
    const { data: groupsResponse } = useGroups();
    const { data: orgTags = [] } = useOrgTags();
    const groups = (groupsResponse as { data?: { id: string; name: string }[] } | undefined)?.data ?? [];

    function toggleGroup(id: string) {
        const next = value.excludedGroups.includes(id) ? value.excludedGroups.filter(g => g !== id) : [...value.excludedGroups, id];
        onChange({ ...value, excludedGroups: next });
    }

    function toggleTag(id: string) {
        const next = value.tags.includes(id) ? value.tags.filter(t => t !== id) : [...value.tags, id];
        onChange({ ...value, tags: next });
    }

    const selectedGroupLabels = value.excludedGroups
        .map(id => groups.find(g => g.id === id)?.name)
        .filter(Boolean)
        .join(', ');

    const selectedTagLabels = value.tags
        .map(id => orgTags.find(t => t.id === id)?.name)
        .filter(Boolean)
        .join(', ');
    const isKeyless = securityType === 'KEY_LESS';
    const showAccessControl = ctx.type === 'api';

    return (
        <div className="space-y-6">
            {/* Plan details */}
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Plan details</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="plan-name">
                            Name <span className="text-destructive">*</span>
                        </Label>
                        <Input
                            id="plan-name"
                            value={value.name}
                            onChange={e => onChange({ ...value, name: e.target.value })}
                            placeholder="e.g. Professional"
                            maxLength={50}
                            disabled={readOnly}
                        />
                        {errors.name && <p className="text-xs text-destructive">{errors.name}</p>}
                        <p className="text-xs text-muted-foreground text-right">{value.name.length}/50</p>
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="plan-description">Description</Label>
                        <Textarea
                            id="plan-description"
                            value={value.description}
                            onChange={e => onChange({ ...value, description: e.target.value })}
                            placeholder="Describe what this plan offers…"
                            rows={3}
                            disabled={readOnly}
                        />
                    </div>

                    <div className="space-y-2">
                        <Label>Characteristics</Label>
                        <CharacteristicsInput
                            value={value.characteristics}
                            onChange={tags => onChange({ ...value, characteristics: tags })}
                            readOnly={readOnly}
                        />
                    </div>
                </CardContent>
            </Card>

            {/* Conditions — API only */}
            {ctx.type === 'api' && (
                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">Conditions</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className="space-y-2">
                            <Label htmlFor="plan-conditions">Page of General Conditions</Label>
                            <Select
                                value={value.generalConditions || '__none__'}
                                onValueChange={v => onChange({ ...value, generalConditions: v === '__none__' ? '' : v })}
                                disabled={readOnly}
                            >
                                <SelectTrigger id="plan-conditions" className="w-full">
                                    <SelectValue placeholder="None" />
                                </SelectTrigger>
                                <SelectContent
                                    position="popper"
                                    className="max-h-60 overflow-y-auto"
                                    style={{ width: 'var(--radix-select-trigger-width)', minWidth: 'unset' }}
                                >
                                    <SelectItem value="__none__">None</SelectItem>
                                </SelectContent>
                            </Select>
                            <p className="text-xs text-muted-foreground">General conditions page shown to consumers before subscribing.</p>
                        </div>
                    </CardContent>
                </Card>
            )}

            {/* Subscriptions */}
            <Card>
                <CardHeader>
                    <CardTitle className="text-base">Subscriptions</CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                    <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                        <div className="space-y-0.5">
                            <Label htmlFor="auto-validate" className="text-sm font-medium">
                                Auto validate subscription
                            </Label>
                            <p className="text-xs text-muted-foreground">Automatically approve new subscriptions without manual review.</p>
                        </div>
                        <Switch
                            id="auto-validate"
                            checked={isKeyless ? true : value.autoValidation}
                            onCheckedChange={checked => onChange({ ...value, autoValidation: checked })}
                            disabled={readOnly || isKeyless}
                        />
                    </div>

                    {!isKeyless && (
                        <>
                            <div className="flex items-center justify-between rounded-lg border px-4 py-3">
                                <div className="space-y-0.5">
                                    <Label htmlFor="comment-required" className="text-sm font-medium">
                                        Require comment on subscription
                                    </Label>
                                    <p className="text-xs text-muted-foreground">Consumer must provide a comment when subscribing.</p>
                                </div>
                                <Switch
                                    id="comment-required"
                                    checked={value.commentRequired}
                                    onCheckedChange={checked =>
                                        onChange({
                                            ...value,
                                            commentRequired: checked,
                                            commentMessage: checked ? value.commentMessage : '',
                                        })
                                    }
                                    disabled={readOnly}
                                />
                            </div>

                            {value.commentRequired && (
                                <div className="space-y-2 pl-2">
                                    <Label htmlFor="comment-message">Custom message to display to consumer</Label>
                                    <Input
                                        id="comment-message"
                                        value={value.commentMessage}
                                        onChange={e => onChange({ ...value, commentMessage: e.target.value })}
                                        placeholder="Please describe your use case…"
                                        maxLength={64}
                                        disabled={readOnly}
                                    />
                                    <p className="text-xs text-muted-foreground text-right">{value.commentMessage.length}/64</p>
                                </div>
                            )}
                        </>
                    )}
                </CardContent>
            </Card>

            {/* Deployment — sharding tags */}
            {orgTags.length > 0 && (
                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">Deployment</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                        <Label>Sharding tags</Label>
                        <Popover open={tagsOpen} onOpenChange={setTagsOpen}>
                            <PopoverTrigger asChild>
                                <Button
                                    variant="outline"
                                    role="combobox"
                                    aria-expanded={tagsOpen}
                                    className="w-full justify-between font-normal"
                                    disabled={readOnly}
                                >
                                    <span className="truncate text-left">
                                        {selectedTagLabels || <span className="text-muted-foreground">None</span>}
                                    </span>
                                    <ChevronsUpDownIcon className="ml-2 size-4 shrink-0 opacity-50" />
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent className="w-[var(--radix-popover-trigger-width)] p-0" align="start">
                                <Command>
                                    <CommandInput placeholder="Search tags…" />
                                    <CommandList>
                                        <CommandEmpty>No tags found.</CommandEmpty>
                                        <CommandGroup>
                                            <ScrollArea className="max-h-60">
                                                {orgTags.map(tag => (
                                                    <CommandItem key={tag.id} value={tag.name} onSelect={() => toggleTag(tag.id)}>
                                                        <Checkbox checked={value.tags.includes(tag.id)} className="mr-2" aria-hidden />
                                                        {tag.name}
                                                        {value.tags.includes(tag.id) && <CheckIcon className="ml-auto size-4" />}
                                                    </CommandItem>
                                                ))}
                                            </ScrollArea>
                                        </CommandGroup>
                                    </CommandList>
                                </Command>
                            </PopoverContent>
                        </Popover>
                        <p className="text-xs text-muted-foreground">
                            Only gateways advertising matching sharding tags will enforce this plan.
                        </p>
                    </CardContent>
                </Card>
            )}

            {/* Access control — API only */}
            {showAccessControl && (
                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">Access Control</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-2">
                        <Label>Groups excluded</Label>
                        <Popover open={groupsOpen} onOpenChange={setGroupsOpen}>
                            <PopoverTrigger asChild>
                                <Button
                                    variant="outline"
                                    role="combobox"
                                    aria-expanded={groupsOpen}
                                    className="w-full justify-between font-normal"
                                    disabled={readOnly}
                                >
                                    <span className="truncate text-left">
                                        {selectedGroupLabels || <span className="text-muted-foreground">None</span>}
                                    </span>
                                    <ChevronsUpDownIcon className="ml-2 size-4 shrink-0 opacity-50" />
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent className="w-[var(--radix-popover-trigger-width)] p-0" align="start">
                                <Command>
                                    <CommandInput placeholder="Search groups…" />
                                    <CommandList>
                                        <CommandEmpty>No groups found.</CommandEmpty>
                                        <CommandGroup>
                                            <ScrollArea className="max-h-60">
                                                {groups.map(g => (
                                                    <CommandItem key={g.id} value={g.name} onSelect={() => toggleGroup(g.id)}>
                                                        <Checkbox
                                                            checked={value.excludedGroups.includes(g.id)}
                                                            className="mr-2"
                                                            aria-hidden
                                                        />
                                                        {g.name}
                                                        {value.excludedGroups.includes(g.id) && <CheckIcon className="ml-auto size-4" />}
                                                    </CommandItem>
                                                ))}
                                            </ScrollArea>
                                        </CommandGroup>
                                    </CommandList>
                                </Command>
                            </PopoverContent>
                        </Popover>
                        <p className="text-xs text-muted-foreground">Members of these groups cannot subscribe to this plan.</p>
                    </CardContent>
                </Card>
            )}
        </div>
    );
}
