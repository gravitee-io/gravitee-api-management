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

const RADIUS = 42;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

export function CircularProgress({ pct }: Readonly<{ pct: number }>) {
    const offset = CIRCUMFERENCE * (1 - pct / 100);
    return (
        <div className="relative w-24 h-24">
            <svg viewBox="0 0 100 100" aria-hidden className="w-24 h-24 text-primary" style={{ transform: 'rotate(-90deg)' }}>
                <circle
                    cx="50"
                    cy="50"
                    r={RADIUS}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="8"
                    className="text-muted-foreground"
                    style={{ opacity: 0.15 }}
                />
                <circle
                    cx="50"
                    cy="50"
                    r={RADIUS}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="8"
                    strokeLinecap="round"
                    strokeDasharray={CIRCUMFERENCE}
                    strokeDashoffset={offset}
                    style={{ transition: 'stroke-dashoffset 0.5s ease' }}
                />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center" aria-label={`${pct}% complete`}>
                <span className="text-xl font-bold">{pct}%</span>
            </div>
        </div>
    );
}
