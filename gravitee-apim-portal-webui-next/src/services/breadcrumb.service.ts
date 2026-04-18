/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Injectable, signal } from '@angular/core';

import { Breadcrumb } from '../components/breadcrumbs/breadcrumbs.component';

@Injectable({ providedIn: 'root' })
export class BreadcrumbService {
  private readonly _breadcrumbs = signal<Breadcrumb[]>([]);
  readonly breadcrumbs = this._breadcrumbs.asReadonly();

  set(breadcrumbs: Breadcrumb[]) {
    this._breadcrumbs.set(breadcrumbs);
  }

  clear(): void {
    this._breadcrumbs.set([]);
  }
}
