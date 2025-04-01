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
import { Component, Input } from '@angular/core';
import { toNumber } from 'lodash';

@Component({
  selector: 'gio-circular-percentage',
  templateUrl: './gio-circular-percentage.component.html',
  styleUrls: ['./gio-circular-percentage.component.scss'],
  standalone: false,
})
export class GioCircularPercentageComponent {
  @Input()
  public score = 0;

  @Input()
  public reverseColor = false;

  public get percentage(): number {
    return parseInt((toNumber(this.score ?? 0) * 100).toFixed(0), 10);
  }

  public get colorClass(): string {
    if (this.percentage < 50) {
      return this.reverseColor ? 'good-color' : 'bad-color';
    } else if (this.percentage >= 50 && this.percentage < 80) {
      return 'medium-color';
    }
    return this.reverseColor ? 'bad-color' : 'good-color';
  }
}
