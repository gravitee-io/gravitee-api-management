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

@Pipe({ name: 'middleEllipsis', standalone: true })
export class MiddleEllipsisPipe implements PipeTransform {
  transform(value: string, frontChars: number = 15, backChars: number = 10): string {
    if (!value) return '';
    if (value.length <= frontChars + backChars) return value;
    return value.slice(0, frontChars) + '...' + value.slice(value.length - backChars);
  }
}
