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
import { Component, inject, OnInit } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader, MatCardTitle } from '@angular/material/card';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { get } from 'lodash';

@Component({
  selector: 'app-service-unavailable',
  standalone: true,
  imports: [MatCard, MatCardTitle, MatCardContent, RouterModule, MatCardHeader],
  templateUrl: './service-unavailable.component.html',
  styleUrl: './service-unavailable.component.scss',
})
export class ServiceUnavailableComponent implements OnInit {
  public activatedRoute = inject(ActivatedRoute);
  public router = inject(Router);

  public message = 'Portal API unreachable or error occurs, please check logs';

  ngOnInit() {
    const state = this.router.lastSuccessfulNavigation?.extras.state;
    const error = get(state, 'errors[0]');
    if (error?.code === 'errors.maintenance.mode') {
      this.message = error.message;
    }
  }
}
