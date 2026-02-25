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
import { Component, inject, Input } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';

import { ButtonToggleGroupComponent } from './button-toggle-group.component';

@Component({
  selector: 'app-button-toggle-option',
  standalone: true,
  imports: [MatButton, MatIcon],
  templateUrl: './button-toggle-option.component.html',
  styleUrl: './button-toggle-option.component.scss',
})
export class ButtonToggleOptionComponent {
  @Input({ required: true })
  value!: string;

  @Input({ required: true })
  icon!: string;

  @Input({ required: true })
  label!: string;

  protected readonly group = inject(ButtonToggleGroupComponent);

  get isActive(): boolean {
    return this.group.value() === this.value;
  }

  select(): void {
    this.group.value.set(this.value);
  }
}
