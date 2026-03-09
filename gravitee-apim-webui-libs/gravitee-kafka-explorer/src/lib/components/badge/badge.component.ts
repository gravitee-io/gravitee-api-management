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
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type BadgeColor = 'default' | 'primary' | 'success' | 'warning' | 'error';

@Component({
  selector: 'gke-badge',
  standalone: true,
  template: '<ng-content />',
  styleUrls: ['./badge.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'gke-badge',
    '[class.gke-badge--primary]': 'color() === "primary"',
    '[class.gke-badge--success]': 'color() === "success"',
    '[class.gke-badge--warning]': 'color() === "warning"',
    '[class.gke-badge--error]': 'color() === "error"',
  },
})
export class BadgeComponent {
  color = input<BadgeColor>('default');
}
