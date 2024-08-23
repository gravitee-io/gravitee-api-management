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
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { MatTooltip } from '@angular/material/tooltip';

import { PictureComponent } from '../picture/picture.component';

@Component({
  selector: 'app-radio-card',
  standalone: true,
  imports: [MatCard, MatCardHeader, MatCardContent, MatTooltip, MatRadioButton, MatRadioGroup, PictureComponent, MatIcon],
  templateUrl: './radio-card.component.html',
  styleUrl: './radio-card.component.scss',
})
export class RadioCardComponent {
  @Input()
  value: unknown;

  @Input()
  title: string = '';

  @Input()
  disabled: boolean = true;

  @Input()
  disabledMessage: string = '';

  @Input()
  selected: boolean = false;

  @Input()
  displayPicture: boolean = false;

  @Input()
  pictureUrl?: string;

  @Input()
  pictureHashValue: string = '';

  @Output()
  selectItem = new EventEmitter<unknown>();

  onSelect() {
    if (!this.disabled) {
      this.selectItem.emit(this.value);
    }
  }
}
