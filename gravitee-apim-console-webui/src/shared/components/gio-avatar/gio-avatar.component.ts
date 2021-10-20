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
import { AfterViewInit, Component, ElementRef, Input, OnChanges, ViewChild } from '@angular/core';
import { toSvg } from 'jdenticon';

@Component({
  selector: 'gio-avatar',
  template: require('./gio-avatar.component.html'),
  styles: [require('./gio-avatar.component.scss')],
  host: {
    '[style.width.px]': 'size',
    '[style.height.px]': 'size',
  },
})
export class GioAvatarComponent implements AfterViewInit, OnChanges {
  @Input()
  public id: string;
  @Input()
  set src(src: string) {
    this.imgSrc = src ?? '';
  }
  @Input()
  public name: string;
  @Input('size')
  public inputSize: number;
  @Input()
  public roundedBorder: boolean;

  @ViewChild('avatarContainer') avatarContainerEleRef: ElementRef;

  public imgSrc = '';
  public defaultSize: number;
  public size: number;

  constructor(private hostEl: ElementRef) {
    this.defaultSize = Math.min(Number(this.hostEl.nativeElement.offsetWidth), Number(this.hostEl.nativeElement.offsetHeight)) || 110;
  }

  ngOnChanges(): void {
    this.size = this.inputSize || this.defaultSize;
  }

  ngAfterViewInit(): void {
    if (this.imgSrc) {
      return;
    }

    if (this.avatarContainerEleRef) {
      const svgString = toSvg(this.name, this.size, { backColor: '#FFF' });
      this.avatarContainerEleRef.nativeElement.innerHTML = svgString;
    }
  }

  onImgError() {
    this.imgSrc = null;
    this.ngAfterViewInit();
  }
}
