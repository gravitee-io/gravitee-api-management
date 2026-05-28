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
import { Button, Input, Label, Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@gravitee/graphene-core';

// ─── Common presets ───────────────────────────────────────────────────────────

const PRESETS = [
    { label: 'Every 5 minutes', value: '0 */5 * * * *' },
    { label: 'Every 10 minutes', value: '0 */10 * * * *' },
    { label: 'Every 30 minutes', value: '0 */30 * * * *' },
    { label: 'Every hour', value: '0 0 * * * *' },
    { label: 'Every 6 hours', value: '0 0 */6 * * *' },
    { label: 'Every day at midnight', value: '0 0 0 * * *' },
] as const;

// ─── Basic 6-field cron validation ───────────────────────────────────────────

const CRON_FIELD = /^(\*|[0-9,\-*/]+)$/;

export function validateCron(value: string): string | null {
    const parts = value.trim().split(/\s+/);
    if (parts.length !== 6) return 'Must have exactly 6 fields: sec min hr dom mon dow';
    for (const part of parts) {
        if (!CRON_FIELD.test(part)) return `Invalid cron field: "${part}"`;
    }
    return null;
}

// ─── Export ───────────────────────────────────────────────────────────────────

interface CronScheduleInputProps {
    value: string;
    error?: string;
    onChange: (v: string) => void;
    disabled?: boolean;
}

export function CronScheduleInput({ value, error, onChange, disabled }: Readonly<CronScheduleInputProps>) {
    function handlePreset(preset: string) {
        if (preset === '__custom__') return;
        onChange(preset);
    }

    const matchedPreset = PRESETS.find(p => p.value === value)?.value ?? '__custom__';

    return (
        <div className="space-y-3">
            <div className="space-y-1.5">
                <Label htmlFor="dp-schedule" className="text-sm">
                    Schedule (cron)
                </Label>
                <p className="text-xs text-muted-foreground py-2">
                    6-field cron expression: <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">sec min hr dom mon dow</code>.
                    Example: <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">0 */5 * * * *</code> = every 5 minutes.
                </p>
                <Input
                    id="dp-schedule"
                    value={value}
                    placeholder="0 */5 * * * *"
                    onChange={e => onChange(e.target.value)}
                    disabled={disabled}
                    aria-invalid={Boolean(error)}
                    className="font-mono text-sm"
                />
                {error && <p className="text-xs text-destructive">{error}</p>}
            </div>
            <div className="flex items-center gap-2">
                <p className="text-xs text-muted-foreground shrink-0">Quick select:</p>
                <Select value={matchedPreset} onValueChange={handlePreset} disabled={disabled}>
                    <SelectTrigger className="flex-1 text-xs h-7">
                        <SelectValue placeholder="Choose a preset" />
                    </SelectTrigger>
                    <SelectContent>
                        <SelectItem value="__custom__" disabled>
                            Custom
                        </SelectItem>
                        {PRESETS.map(p => (
                            <SelectItem key={p.value} value={p.value}>
                                {p.label}
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    className="text-xs h-7 px-2 shrink-0"
                    onClick={() => onChange('0 */5 * * * *')}
                    disabled={disabled}
                >
                    Reset
                </Button>
            </div>
        </div>
    );
}
