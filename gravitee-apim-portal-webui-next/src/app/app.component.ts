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
import { Component, effect, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterOutlet } from '@angular/router';
import { BreadcrumbService } from 'xng-breadcrumb';

import { FooterComponent } from '../components/footer/footer.component';
import { NavBarComponent } from '../components/nav-bar/nav-bar.component';
import { ConfigService } from '../services/config.service';
import { CurrentUserService } from '../services/current-user.service';
import { ThemeService } from '../services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavBarComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  currentUser = inject(CurrentUserService).user;
  logo = inject(ThemeService).logo;
  favicon = inject(ThemeService).favicon;
  siteTitle: string;

  constructor(
    private configService: ConfigService,
    // Don't delete BreadcrumbService from here - it's responsible for correct breadcrumb navigation
    private breadcrumbService: BreadcrumbService,
    private title: Title,
  ) {
    this.siteTitle = configService.configuration?.portalNext?.siteTitle ?? 'Developer Portal';
    this.title.setTitle(this.siteTitle);
    effect(() => {
      if (this.favicon()) {
        this.updateFavicon();
      }
    });
  }

  private updateFavicon() {
    let link: HTMLLinkElement | null = document.querySelector("link[rel~='icon']");
    if (!link) {
      link = document.createElement('link');
      link.rel = 'icon';
      document.getElementsByTagName('head')[0].appendChild(link);
    }
    link.href = this.favicon();
  }
}
