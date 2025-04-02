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
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, ViewChild } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent } from '@angular/material/card';
import { MatTooltip } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';

import { Application } from '../../entities/application/application';
import { PictureComponent } from '../picture/picture.component';

@Component({
  selector: 'app-application-card',
  imports: [MatButton, MatCard, MatCardActions, MatCardContent, PictureComponent, RouterLink, MatTooltip],
  templateUrl: './application-card.component.html',
  styleUrl: './application-card.component.scss',
})
export class ApplicationCardComponent implements AfterViewInit {
  @Input({ required: true })
  application!: Application;

  @ViewChild('appName', { static: true }) appNameElement!: ElementRef;

  isOverflowing: boolean = false;

  constructor(private cdr: ChangeDetectorRef) {}

  ngAfterViewInit() {
    this.checkOverflow();
    this.cdr.detectChanges();
  }

  checkOverflow() {
    const el = this.appNameElement.nativeElement;
    this.isOverflowing = el.scrollWidth > el.clientWidth || el.scrollHeight > el.clientHeight;
  }
}
