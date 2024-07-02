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
import { CdkCopyToClipboard } from '@angular/cdk/clipboard';
import { Component, computed, Input, Signal, signal } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';

import { CopyCodeIconComponent } from './copy-code-icon/copy-code-icon/copy-code-icon.component';

@Component({
  selector: 'app-copy-code',
  standalone: true,
  imports: [MatIcon, MatIconButton, MatSuffix, CdkCopyToClipboard, CopyCodeIconComponent],
  templateUrl: './copy-code.component.html',
  styleUrl: './copy-code.component.scss',
})
export class CopyCodeComponent {
  @Input()
  title!: string;

  @Input()
  text: string = '';

  @Input()
  mode: 'TEXT' | 'PASSWORD' = 'TEXT';

  hidePassword = signal(true);

  passwordContent: Signal<string> = computed(() => (this.hidePassword() ? this.text.replaceAll(/./g, '*') : this.text));
}
