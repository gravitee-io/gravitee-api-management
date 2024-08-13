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
import { Component, Input } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'app-copy-code-icon',
  standalone: true,
  imports: [MatIcon, CdkCopyToClipboard, MatIconButton],
  template: `
    <button
      mat-icon-button
      class="btn"
      [attr.aria-label]="label"
      [class.clicked]="clicked"
      [cdkCopyToClipboard]="contentToCopy"
      (cdkCopyToClipboardCopied)="onCopied()">
      <mat-icon>{{ clicked ? 'check' : 'content_copy' }}</mat-icon>
    </button>
  `,
})
export class CopyCodeIconComponent {
  @Input()
  contentToCopy!: string;

  @Input()
  label: string = '';

  clicked: boolean = false;

  onCopied() {
    this.clicked = true;

    setTimeout(() => {
      this.clicked = false;
    }, 2000);
  }
}
