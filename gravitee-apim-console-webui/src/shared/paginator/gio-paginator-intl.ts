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
import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

@Injectable()
export class GioPaginatorIntl extends MatPaginatorIntl {
  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    const isUnknownTotal = length < 0 || length === Number.MAX_SAFE_INTEGER;
    const totalLabel = isUnknownTotal ? 'N/A' : `${length}`;
    if (length === 0 || pageSize === 0) {
      return `0 of ${totalLabel}`;
    }
    const startIndex = page * pageSize;
    const endIndex = isUnknownTotal
      ? startIndex + pageSize
      : startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `${startIndex + 1} – ${endIndex} of ${totalLabel}`;
  };
}
