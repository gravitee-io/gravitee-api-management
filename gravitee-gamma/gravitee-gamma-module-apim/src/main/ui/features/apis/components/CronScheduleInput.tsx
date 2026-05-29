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

import { CRON_PRESETS, describeCronSchedule } from '../utils/cronSchedule';

export { validateCron } from '../utils/cronSchedule';

/** Matches Graphene `Input` default height (h-9 / 36px). */
const INPUT_ROW_HEIGHT = 'h-9';

interface CronScheduleInputProps {
    value: string;
    error?: string;
    onChange: (v: string) => void;
    disabled?: boolean;
    /** Prefix for input id (e.g. `dp` → `dp-schedule`). */
    idPrefix?: string;
}

export function CronScheduleInput({ value, error, onChange, disabled, idPrefix = 'schedule' }: Readonly<CronScheduleInputProps>) {
    const scheduleId = `${idPrefix}-cron`;

    function handlePreset(preset: string) {
        if (preset === '__custom__') return;
        onChange(preset);
    }

    const matchedPreset = CRON_PRESETS.find(p => p.value === value)?.value ?? '__custom__';
    const scheduleSummary = describeCronSchedule(value);

    return (
        <div className="space-y-3">
            <div className="space-y-2">
                <Label htmlFor={scheduleId} className="text-sm">
                    Schedule (cron)
                </Label>
                <p className="text-xs text-muted-foreground py-2">
                    6-field cron expression: <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">sec min hr dom mon dow</code>.
                    Example: <code className="font-mono bg-muted px-1 py-0.5 rounded text-xs">0 */5 * * * *</code> = every 5 minutes.
                </p>
                <div className="flex flex-col gap-2 sm:flex-row">
                    <Input
                        id={scheduleId}
                        value={value}
                        placeholder="0 */5 * * * *"
                        onChange={e => onChange(e.target.value)}
                        disabled={disabled}
                        aria-invalid={Boolean(error)}
                        className={`${INPUT_ROW_HEIGHT} min-w-0 flex-1 font-mono text-sm`}
                    />
                    <div
                        className={`${INPUT_ROW_HEIGHT} min-w-0 flex-1 flex items-center rounded-md border border-input bg-muted/40 px-3 overflow-y-auto`}
                        aria-live="polite"
                        title={scheduleSummary ?? undefined}
                    >
                        <p className={`text-sm leading-snug line-clamp-2 ${scheduleSummary ? 'text-foreground' : 'text-muted-foreground'}`}>
                            {scheduleSummary ?? 'Enter a valid cron expression to see when it runs.'}
                        </p>
                    </div>
                </div>
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
                        {CRON_PRESETS.map(p => (
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
