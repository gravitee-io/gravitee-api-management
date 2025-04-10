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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { GioClipboardModule, GioIconsModule } from '@gravitee/ui-particles-angular';

import { ExposedEntrypoint } from '../../../../entities/management-api-v2';

@Component({
  selector: 'exposed-entrypoints',
  templateUrl: './exposed-entrypoints.component.html',
  styleUrls: ['./exposed-entrypoints.component.scss'],
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIcon, MatTooltip, GioIconsModule, GioClipboardModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExposedEntrypointsComponent {
  exposedEntrypoints = input.required<ExposedEntrypoint[]>();
}
