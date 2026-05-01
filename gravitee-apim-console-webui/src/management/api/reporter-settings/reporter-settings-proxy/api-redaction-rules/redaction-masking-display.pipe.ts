/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Pipe, PipeTransform } from '@angular/core';

import { RedactionRule } from '../../../../../entities/management-api-v2';

export type MaskingDisplay = {
  label: string;
  badgeClass: string;
  detail: string;
};

@Pipe({ name: 'redactionMaskingDisplay', standalone: true, pure: true })
export class RedactionMaskingDisplayPipe implements PipeTransform {
  transform(rule: RedactionRule): MaskingDisplay {
    const isPartial = rule.maskingStrategy?.type === 'PARTIAL';
    const label = isPartial ? 'PARTIAL' : 'FULL';
    const badgeClass = isPartial ? 'gio-badge-accent' : 'gio-badge-neutral';
    let detail: string;
    if (!isPartial) {
      const text = rule.maskingStrategy?.replacement ?? '[REDACTED]';
      detail = `→ "${text}"`;
    } else {
      const pre = rule.maskingStrategy?.prefixLength ?? 0;
      const suf = rule.maskingStrategy?.suffixLength ?? 0;
      const char = rule.maskingStrategy?.replacement ?? '*';
      detail = `prefix ${pre} · suffix ${suf} · char "${char}"`;
    }
    return { label, badgeClass, detail };
  }
}
