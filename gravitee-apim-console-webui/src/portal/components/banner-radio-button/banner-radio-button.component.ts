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
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatRadioButton } from '@angular/material/radio';

@Component({
  selector: 'banner-radio-button',
  standalone: true,
  imports: [MatRadioButton],
  templateUrl: './banner-radio-button.component.html',
  styleUrl: './banner-radio-button.component.scss',
})
export class BannerRadioButtonComponent {
  @Input()
  value!: boolean;

  @Input()
  title!: string;

  @Input()
  subtitle!: string;

  @Input()
  disabled: boolean = false;

  @Input()
  selected: boolean = false;

  @Output()
  bannerOptionSelected = new EventEmitter<boolean>();

  onBannerOptionSelected() {
    this.bannerOptionSelected.emit(this.value);
  }
}
