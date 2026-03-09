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
import { Component, computed, ElementRef, HostListener, inject, input, InputSignal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { isEmpty } from 'lodash';
import { catchError, map } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';
import { User } from '../../../entities/user/user';
import { PortalService } from '../../../services/portal.service';

@Component({
  selector: 'app-mobile-nav-bar',
  templateUrl: './mobile-nav-bar.component.html',
  styleUrl: './mobile-nav-bar.component.scss',
  imports: [RouterLink, RouterLinkActive, MatIcon, MatButton],
})
export class MobileNavBarComponent {
  currentUser: InputSignal<User> = input({});
  topBarNavigationItems: InputSignal<PortalNavigationItem[]> = input<PortalNavigationItem[]>([]);
  hasHomepage = toSignal(
    inject(PortalService)
      .getPortalHomepages()
      .pipe(
        map(homepages => homepages?.length > 0),
        catchError(() => of(false)),
      ),
  );

  protected isLoggedIn = computed(() => {
    return !isEmpty(this.currentUser());
  });
  protected isMobileMenuOpened = false;
  private readonly elementRef = inject(ElementRef);

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: MouseEvent) {
    if (this.isMobileMenuOpened && !this.elementRef.nativeElement.contains(event.target)) {
      this.closeMenu();
    }
  }

  closeMenu() {
    this.isMobileMenuOpened = false;
  }
}
