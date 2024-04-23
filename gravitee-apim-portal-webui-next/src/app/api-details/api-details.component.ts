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
import { Component, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardContent } from '@angular/material/card';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { map, Observable, of } from 'rxjs';

import { ApiCardComponent } from '../../components/api-card/api-card.component';
import { ApiPictureComponent } from '../../components/api-picture/api-picture.component';
import { BannerComponent } from '../../components/banner/banner.component';
import { Api } from '../../entities/api/api';

@Component({
  selector: 'app-api-details',
  standalone: true,
  imports: [
    ApiCardComponent,
    ApiPictureComponent,
    MatButton,
    MatCard,
    MatCardActions,
    MatCardContent,
    BannerComponent,
    MatTabsModule,
    MatIconModule,
    AsyncPipe,
    RouterModule,
  ],
  templateUrl: './api-details.component.html',
  styleUrl: './api-details.component.scss',
})
export class ApiDetailsComponent implements OnInit {
  details$: Observable<Api> = of();

  constructor(
    private domSanitizer: DomSanitizer,
    private matIconRegistry: MatIconRegistry,
    private activatedRoute: ActivatedRoute,
  ) {
    this.matIconRegistry.addSvgIcon(`light-bulb`, this.domSanitizer.bypassSecurityTrustResourceUrl('assets/images/lightbulb_24px.svg'));
  }

  ngOnInit(): void {
    this.details$ = this.activatedRoute.data.pipe(map(({ api }) => api));
  }
}
