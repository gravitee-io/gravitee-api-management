/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { ActivatedRouteSnapshot, CanActivate, Router } from '@angular/router';
import { Inject, Injectable } from '@angular/core';

import { Constants } from '../entities/Constants';

@Injectable({
  providedIn: 'root',
})
export class NotMultiTenantGuard implements CanActivate {
  constructor(
    private router: Router,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  canActivate(_route: ActivatedRouteSnapshot): boolean {
    return this.constants?.org?.settings?.management?.installationType !== 'multi-tenant';
  }
}
