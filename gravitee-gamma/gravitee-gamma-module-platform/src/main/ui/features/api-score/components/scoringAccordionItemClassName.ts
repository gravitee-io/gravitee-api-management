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
import type { CSSProperties } from 'react';

/** Standalone bordered accordion rows (not divider-style lists). */
export const SCORING_ACCORDION_ITEM_CLASSNAME = 'rounded-lg border px-4';

/**
 * Graphene AccordionItem adds `last:border-b-0`; inline style wins when that utility is missing from CSS.
 */
export function scoringAccordionItemStyle(isLast: boolean): CSSProperties | undefined {
    return isLast ? { borderBottomWidth: '1px' } : undefined;
}
