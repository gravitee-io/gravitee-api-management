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
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router } from '@angular/router';
import { switchMap, tap } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { CurrentUserService } from '../../services/current-user.service';
import { PortalMenuLinksService } from '../../services/portal-menu-links.service';

@Component({
  selector: 'app-log-in',
  standalone: true,
  imports: [MatCardModule, MatFormField, MatInput, MatButton, MatLabel, ReactiveFormsModule, MatError],
  templateUrl: './log-in.component.html',
  styleUrl: './log-in.component.scss',
})
export class LogInComponent {
  logInForm: FormGroup<{ username: FormControl; password: FormControl }> = new FormGroup({
    username: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required]),
  });
  error = signal(200);

  private destroyRef = inject(DestroyRef);
  constructor(
    private authService: AuthService,
    private currentUserService: CurrentUserService,
    private portalMenuLinksService: PortalMenuLinksService,
    private router: Router,
  ) {}

  logIn() {
    this.authService
      .login(this.logInForm.value.username, this.logInForm.value.password)
      .pipe(
        switchMap(_ => this.currentUserService.loadUser()),
        switchMap(_ => this.portalMenuLinksService.loadCustomLinks()),
        tap(_ => this.router.navigate([''])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        error: (err: HttpErrorResponse) => {
          this.error.set(err.status);
        },
      });
  }
}
