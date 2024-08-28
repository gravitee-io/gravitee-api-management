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
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { switchMap, tap } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { CurrentUserService } from '../../services/current-user.service';
import { PortalMenuLinksService } from '../../services/portal-menu-links.service';

@Component({
  selector: 'app-log-out',
  standalone: true,
  imports: [],
  template: '',
})
export class LogOutComponent implements OnInit {
  private destroyRef = inject(DestroyRef);

  constructor(
    private authService: AuthService,
    private currentUserService: CurrentUserService,
    private portalMenuLinksService: PortalMenuLinksService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.authService
      .logout()
      .pipe(
        tap(_ => this.currentUserService.clear()),
        switchMap(_ => this.portalMenuLinksService.loadCustomLinks()),
        tap(_ => this.router.navigate([''])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }
}
