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
import { Component, inject } from '@angular/core';
import { MatAnchor } from '@angular/material/button';

import { BannerComponent } from '../../../../components/banner/banner.component';
import { BannerButton } from '../../../../entities/configuration/configuration-portal-next';
import { ConfigService } from '../../../../services/config.service';
import { CurrentUserService } from '../../../../services/current-user.service';

interface BannerButtonVM {
  displayed: boolean;
  label?: string;
  href?: string;
}

@Component({
  selector: 'app-catalog-banner',
  standalone: true,
  imports: [BannerComponent, MatAnchor],
  templateUrl: './catalog-banner.component.html',
  styleUrl: './catalog-banner.component.scss',
})
export class CatalogBannerComponent {
  bannerTitle: string;
  bannerSubtitle: string;
  primaryButton: BannerButtonVM;
  secondaryButton: BannerButtonVM;

  private readonly isUserAuthenticated = inject(CurrentUserService).isUserAuthenticated;

  constructor(private readonly configService: ConfigService) {
    this.bannerTitle = this.configService.configuration?.portalNext?.banner?.title ?? '';
    this.bannerSubtitle = this.configService.configuration?.portalNext?.banner?.subtitle ?? '';
    this.primaryButton = this.bannerButtonToVM(this.configService.configuration?.portalNext?.banner?.primaryButton);
    this.secondaryButton = this.bannerButtonToVM(this.configService.configuration?.portalNext?.banner?.secondaryButton);
  }

  private bannerButtonToVM(bannerButton: BannerButton | undefined): BannerButtonVM {
    return {
      displayed: !!bannerButton && !!bannerButton.enabled && (bannerButton.visibility === 'PUBLIC' || this.isUserAuthenticated()),
      label: bannerButton?.label,
      href: bannerButton?.target,
    };
  }
}
