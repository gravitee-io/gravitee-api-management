/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { ComponentHarness } from '@angular/cdk/testing';

import { BadgeColor } from './badge.component';

export class BadgeHarness extends ComponentHarness {
  static hostSelector = 'gke-badge';

  async getText(): Promise<string> {
    const host = await this.host();
    return host.text();
  }

  async getColor(): Promise<BadgeColor> {
    const host = await this.host();
    if (await host.hasClass('gke-badge--primary')) return 'primary';
    if (await host.hasClass('gke-badge--success')) return 'success';
    if (await host.hasClass('gke-badge--warning')) return 'warning';
    if (await host.hasClass('gke-badge--error')) return 'error';
    return 'default';
  }
}
