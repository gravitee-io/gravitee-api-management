/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, computed, input } from '@angular/core';

export type ButtonAppearance = 'filled' | 'outlined' | 'text';
const VALID_APPEARANCES: string[] = ['filled', 'outlined', 'text'];

@Component({
  selector: 'gmd-button',
  imports: [CommonModule],
  templateUrl: './gmd-button.component.html',
  styleUrl: './gmd-button.component.scss',
})
export class GmdButtonComponent {
  appearance = input<ButtonAppearance>('filled');
  link = input<string | undefined>();
  target = input<string | undefined>();

  appearanceVM = computed(() => VALID_APPEARANCES.find(buttonAppearance => this.appearance() === buttonAppearance) ?? 'filled');
  hrefVM = computed(() => this.link() || '/');
  targetVM = computed(() => this.target() || '_self');
}
