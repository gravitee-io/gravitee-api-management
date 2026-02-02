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
import { Component, computed, input, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'app-copy-button',
  imports: [MatButton],
  template: `
    <button
      mat-button
      [attr.aria-label]="ariaLabel()"
      (click)="onClickCopy()">
      <span class="material-icons m3-icon-medium-new">{{ copied() ? 'check' : 'content_copy' }}</span>
    </button>
  `,
  styles: [
    `
      button {
        min-width: 48px;
      }
    `,
  ],
})
export class CopyButtonComponent {
  content = input.required<string>();
  label = input.required<string>();

  ariaLabel = computed(() => $localize`:@@copyButtonPrefix:Copy` + ` ${this.label()}`);
  copied = signal(false);

  onClickCopy() {
    navigator.clipboard.writeText(this.content()).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }
}
