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

const RADIUS = 40;
const STROKE_WIDTH = 8;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;
const TRACK_STROKE = '#e4e4e7';

export function MonitoringCircularGauge({ pct, label }: Readonly<{ pct: number; label?: string }>) {
    const boundedPct = Math.min(100, Math.max(0, Number.isFinite(pct) ? pct : 0));
    const offset = CIRCUMFERENCE * (1 - boundedPct / 100);

    return (
        <div className="flex flex-col items-center gap-3">
            <div className="relative" style={{ width: '7.5rem', height: '7.5rem' }} role="img" aria-label={`${boundedPct}%`}>
                <svg viewBox="0 0 100 100" aria-hidden style={{ width: '100%', height: '100%', transform: 'rotate(-90deg)' }}>
                    <circle cx="50" cy="50" r={RADIUS} fill="none" stroke={TRACK_STROKE} strokeWidth={STROKE_WIDTH} />
                    <circle
                        cx="50"
                        cy="50"
                        r={RADIUS}
                        fill="none"
                        stroke="currentColor"
                        strokeWidth={STROKE_WIDTH}
                        strokeLinecap="round"
                        strokeDasharray={CIRCUMFERENCE}
                        strokeDashoffset={offset}
                        className="text-primary transition-all duration-500 ease-out"
                    />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center">
                    <span className="text-2xl font-bold tabular-nums text-foreground">{boundedPct}%</span>
                </div>
            </div>
            {label ? <h4 className="text-sm font-medium text-muted-foreground">{label}</h4> : null}
        </div>
    );
}
