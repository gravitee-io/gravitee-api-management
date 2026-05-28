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
import { Label, Switch } from '@gravitee/graphene-core';
import { ChevronDownIcon, ChevronUpIcon } from '@gravitee/graphene-core/icons';
import { type ReactNode, useState } from 'react';

// ─── CollapsibleSection ───────────────────────────────────────────────────────

export function CollapsibleSection({
    title,
    defaultOpen = false,
    children,
}: {
    title: string;
    defaultOpen?: boolean;
    children: ReactNode;
}) {
    const [open, setOpen] = useState(defaultOpen);
    return (
        <div className="rounded-lg border">
            <button
                type="button"
                className="flex w-full items-center justify-between px-4 py-3 text-sm font-medium hover:bg-accent/50 transition-colors rounded-lg"
                onClick={() => setOpen(o => !o)}
                aria-expanded={open}
            >
                {title}
                {open ? (
                    <ChevronUpIcon className="size-4 shrink-0" aria-hidden />
                ) : (
                    <ChevronDownIcon className="size-4 shrink-0" aria-hidden />
                )}
            </button>
            {open && <div className="border-t px-4 pt-3 pb-3 space-y-3">{children}</div>}
        </div>
    );
}

// ─── SwitchRow ────────────────────────────────────────────────────────────────

/** Inline toggle row: label + optional description on the left, switch on the right.
 *  Used for stacked toggle lists (HTTP client, proxy, SSL, etc.).
 *  Not for use where each toggle needs its own bordered card. */
export function SwitchRow({
    id,
    label,
    desc,
    checked,
    onChange,
    disabled,
    note,
}: {
    id: string;
    label: string;
    desc?: string;
    checked: boolean;
    onChange: (v: boolean) => void;
    disabled?: boolean;
    note?: string;
}) {
    return (
        <div className="space-y-1">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-0.5">
                    <Label htmlFor={id} className={`text-sm${disabled ? ' text-muted-foreground' : ''}`}>
                        {label}
                    </Label>
                    {desc && <p className="text-xs text-muted-foreground">{desc}</p>}
                </div>
                <Switch id={id} checked={checked} onCheckedChange={onChange} disabled={disabled} />
            </div>
            {note && <p className="text-xs text-warning">{note}</p>}
        </div>
    );
}
