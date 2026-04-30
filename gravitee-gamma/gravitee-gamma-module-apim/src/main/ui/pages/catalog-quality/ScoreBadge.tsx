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

function getScoreColor(score: number): string {
    if (score >= 70) return 'hsl(142 71% 45%)';
    if (score >= 40) return 'hsl(38 92% 50%)';
    return 'hsl(0 84% 60%)';
}

function getScoreLabel(score: number): string {
    if (score >= 70) return 'Good';
    if (score >= 40) return 'Fair';
    return 'Poor';
}

export function ScoreBadge({ score }: { readonly score: number }) {
    const color = getScoreColor(score);
    return (
        <span
            style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '2px 10px',
                borderRadius: '9999px',
                fontSize: '13px',
                fontWeight: 600,
                color: 'white',
                backgroundColor: color,
            }}
        >
            {score}
            <span style={{ fontWeight: 400, fontSize: '11px', opacity: 0.9 }}>{getScoreLabel(score)}</span>
        </span>
    );
}
