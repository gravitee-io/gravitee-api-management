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
import { AsyncPipe } from '@angular/common';
import { Component, inject, Input, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { BreadcrumbService } from 'xng-breadcrumb';

import { BannerComponent } from '../../../components/banner/banner.component';
import { BreadcrumbNavigationComponent } from '../../../components/breadcrumb-navigation/breadcrumb-navigation.component';
import { PictureComponent } from '../../../components/picture/picture.component';
import { Application } from '../../../entities/application/application';
import { CurrentUserService } from '../../../services/current-user.service';

@Component({
  selector: 'app-application',
  standalone: true,
  imports: [
    BreadcrumbNavigationComponent,
    RouterOutlet,
    AsyncPipe,
    BannerComponent,
    MatButton,
    MatCard,
    MatCardContent,
    MatIcon,
    MatTabLink,
    MatTabNav,
    MatTabNavPanel,
    PictureComponent,
    RouterLinkActive,
    RouterLink,
  ],
  templateUrl: './application.component.html',
  styleUrl: './application.component.scss',
})
export class ApplicationComponent implements OnInit {
  @Input() application!: Application;
  isAuthenticated = inject(CurrentUserService).isUserAuthenticated;

  constructor(private breadcrumbService: BreadcrumbService) {}

  ngOnInit(): void {
    this.breadcrumbService.set('@appName', this.application.name);
  }
}
