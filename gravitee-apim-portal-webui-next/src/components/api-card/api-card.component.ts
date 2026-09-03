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
import { Component, Input, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';

import { MatTooltipOnEllipsisDirective } from '../../directives/mat-tooltip-on-ellipsis.directive';
import { BadgeComponent } from '../badge/badge.component';
import { OverflowLabelsComponent } from '../overflow-labels/overflow-labels.component';

export type ApiCardAccess = 'SUBSCRIBED' | 'NO_KEY' | 'CREDENTIALS' | 'APPROVAL';

export interface ApiCardSkill {
  name: string;
  description?: string;
}

@Component({
  selector: 'app-api-card',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatTooltip,
    MatTooltipOnEllipsisDirective,
    BadgeComponent,
    OverflowLabelsComponent,
  ],
  templateUrl: './api-card.component.html',
  styleUrl: './api-card.component.scss',
})
export class ApiCardComponent {
  readonly typeLabel = input<string>();
  readonly capabilities = input<string[]>([]);
  readonly access = input<ApiCardAccess>();
  readonly accessLabel = input<string>();
  readonly skills = input<ApiCardSkill[]>([]);
  readonly endpoint = input<string>();
  readonly protocol = input<string>();
  readonly published = input<string>();
  readonly plans = input<string>();

  @Input({ required: true })
  apiId!: string;
  @Input({ required: true })
  title!: string;
  @Input({ required: true })
  version!: string;
  @Input()
  content?: string;

  cardSelect = output<string>();
  subscribeSelect = output<string>();
  documentationSelect = output<string>();

  protected readonly expanded = signal(false);

  protected toggleDetails(): void {
    this.expanded.update(expanded => !expanded);
  }
}
