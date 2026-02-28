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
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map, shareReplay } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ObservabilityBreakpointService {
  // Using Breakpoints.XSmall for true mobile devices (max-width: 599.98px)
  // This ensures only smartphones and very small tablets are considered mobile
  // Laptops and larger screens will use desktop navigation
  readonly isMobile$ = inject(BreakpointObserver)
    .observe([Breakpoints.XSmall])
    .pipe(
      map(state => state.matches),
      shareReplay({ refCount: true, bufferSize: 1 }),
    );
  readonly isMobile = toSignal(this.isMobile$);

  // TODO: expect conflict here with changes from https://github.com/gravitee-io/gravitee-api-management/pull/15001
  // Using Breakpoints.XSmall + Small + Medium for narrow viewports (max-width: 1279.98px)
  // This covers phones, small tablets, and medium tablets — cards grid switches to 2-col below this threshold
  readonly isNarrow$ = inject(BreakpointObserver)
    .observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.Medium])
    .pipe(
      map(state => state.matches),
      shareReplay({ refCount: true, bufferSize: 1 }),
    );
  readonly isNarrow = toSignal(this.isNarrow$);
}
