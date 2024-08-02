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
import { Component, Input } from '@angular/core';
import { toSvg } from 'jdenticon';

@Component({
  selector: 'app-picture',
  standalone: true,
  imports: [],
  template: `
    <img
      i18n-alt="@@apiPicture"
      alt="API Picture"
      class="round"
      [src]="picture"
      [height]="size"
      [width]="size"
      (error)="useGeneratedPicture()"
      placeholder="assets/images/logo.png" />
  `,
  styleUrl: './picture.component.scss',
})
export class PictureComponent {
  @Input()
  hashValue!: string;
  @Input()
  picture: string | undefined = 'assets/images/logo.png';
  @Input()
  size: number = 40;

  useGeneratedPicture() {
    const asSvg = toSvg(this.hashValue, this.size, { backColor: '#FFF', padding: 0 });
    const encodedToBase64 = window.btoa(asSvg);
    this.picture = `data:image/svg+xml;base64,${encodedToBase64}`;
  }
}
