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
import { Badge, cn } from '@gravitee/graphene-core';
import { ShieldCheckIcon } from '@gravitee/graphene-core/icons';

import type { ScoringSeverityKey } from '../../types/apiScore';
import { countColorClasses, scoreColorClasses, scoreToPercent } from '../../utils/scoreFormat';

/** A score as a colored `NN%` pill, or a neutral "Not available" when never evaluated. */
export function ScorePill({ score }: { score: number | null | undefined }) {
    const percent = scoreToPercent(score);
    if (percent === null) {
        return (
            <Badge variant="outline" className="gap-1 text-xs text-muted-foreground border-border">
                Not available
            </Badge>
        );
    }
    return (
        <Badge variant="outline" className={cn('gap-1 text-xs tabular-nums', scoreColorClasses(score))}>
            <ShieldCheckIcon className="size-3" aria-hidden />
            {percent}%
        </Badge>
    );
}

/** A severity count: `—` when absent, a neutral `0`, or a severity-colored count. */
export function CountBadge({ severity, count }: { severity: ScoringSeverityKey; count: number | null | undefined }) {
    if (typeof count !== 'number') {
        return <span className="text-muted-foreground text-sm">—</span>;
    }
    return (
        <Badge variant="outline" className={cn('text-xs tabular-nums', countColorClasses(severity, count))}>
            {count}
        </Badge>
    );
}
