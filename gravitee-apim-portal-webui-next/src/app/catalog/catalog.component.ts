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
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { map, Observable } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { TabsViewComponent } from './tabs-view/tabs-view.component';
import { BannerComponent } from '../../components/banner/banner.component';
import { Category } from '../../entities/categories/categories';
import { BannerButton } from '../../entities/configuration/configuration-portal-next';
import { CategoriesService } from '../../services/categories.service';
import { ConfigService } from '../../services/config.service';
import { CurrentUserService } from '../../services/current-user.service';

interface BannerButtonVM {
  displayed: boolean;
  label?: string;
  href?: string;
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [BannerComponent, AsyncPipe, MatTabsModule, MatButtonModule, TabsViewComponent],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CatalogComponent implements OnInit {
  showBanner: boolean;
  bannerTitle: string;
  bannerSubtitle: string;
  primaryButton: BannerButtonVM;
  secondaryButton: BannerButtonVM;

  categories$: Observable<Category[]> = of();
  private isUserAuthenticated = inject(CurrentUserService).isUserAuthenticated;

  constructor(
    private configService: ConfigService,
    private categoriesService: CategoriesService,
  ) {
    this.showBanner = this.configService.configuration?.portalNext?.banner?.enabled ?? false;
    this.bannerTitle = this.configService.configuration?.portalNext?.banner?.title ?? '';
    this.bannerSubtitle = this.configService.configuration?.portalNext?.banner?.subtitle ?? '';
    this.primaryButton = this.bannerButtonToVM(this.configService.configuration?.portalNext?.banner?.primaryButton);
    this.secondaryButton = this.bannerButtonToVM(this.configService.configuration?.portalNext?.banner?.secondaryButton);
  }

  ngOnInit() {
    this.categories$ = this.categoriesService.categories().pipe(map(({ data }) => data));
  }

  private bannerButtonToVM(bannerButton: BannerButton | undefined): BannerButtonVM {
    return {
      displayed: !!bannerButton && !!bannerButton.enabled && (bannerButton.visibility === 'PUBLIC' || this.isUserAuthenticated()),
      label: bannerButton?.label,
      href: bannerButton?.target,
    };
  }
}
