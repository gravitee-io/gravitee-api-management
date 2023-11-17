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
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';

import { EnvironmentService } from '../services-ngx/environment.service';
import { Environment } from '../entities/environment/environment';
import { Constants } from '../entities/Constants';
import { EnvironmentSettingsService } from '../services-ngx/environment-settings.service';

@Injectable({ providedIn: 'root' })
export class EnvironmentResolver implements Resolve<{ currentEnvironment: Environment; environments: Environment[] }> {
  constructor(
    private readonly environmentService: EnvironmentService,
    private readonly environmentSettingsService: EnvironmentSettingsService,
    @Inject('Constants') private constants: Constants,
  ) {}

  /**
   * Resolve the environment from the route parameter.
   * If the environment is not found, the first one is returned.
   */
  resolve(
    route: ActivatedRouteSnapshot,
    _: RouterStateSnapshot,
  ): Observable<{ currentEnvironment: Environment; environments: Environment[] }> {
    const envId = route.paramMap.get('envId');

    return this.environmentService.list().pipe(
      map((environments) => {
        const currentEnvironment = environments.find((e) => e.id.toLowerCase() === envId.toLowerCase()) ?? environments[0];

        if (!currentEnvironment) {
          throw new Error(`No environment found!`);
        }

        // FIXME: this is a hack to make the environment available in the constants. Try to remove it.
        this.constants.org.currentEnv = currentEnvironment;

        return { currentEnvironment, environments };
      }),
      switchMap(({ currentEnvironment, environments }) => {
        return this.environmentSettingsService.get().pipe(
          map((settings) => {
            // FIXME: this is a hack to make the environment settings available in the constants. Try to remove it.
            this.constants.env.settings = settings;
            return {
              currentEnvironment,
              environments,
            };
          }),
        );
      }),
    );
  }
}
