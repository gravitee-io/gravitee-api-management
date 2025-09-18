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
import { Component, effect, ElementRef, inject, input, InputSignal } from '@angular/core';

@Component({
  selector: 'gmd-card',
  templateUrl: 'gmd-card.component.html',
  styleUrls: ['gmd-card.component.scss'],
  // eslint-disable-next-line @angular-eslint/prefer-standalone
  standalone: false,
  preserveWhitespaces: true,
})
export class GmdCardComponent {
  backgroundColor: InputSignal<string> = input('');
  textColor: InputSignal<string> = input('');

  private readonly host = inject(ElementRef<HTMLElement>);

  constructor() {
    effect(() => {
      const hostEl = this.host.nativeElement;
      if (this.backgroundColor()) {
        hostEl.style.setProperty('--gmd-card-container-color', this.backgroundColor()!);
      }
      if (this.textColor()) {
        hostEl.style.setProperty('--gmd-card-text-color', this.textColor()!);
      }
    });
  }
}
