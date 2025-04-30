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

import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map, scan } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class PolicyStudioDebugTimelineHoverService {
  private hoveredMap$ = new Subject<Record<string, boolean>>();

  setHover(id: string, isHover: boolean): void {
    this.hoveredMap$.next({ [id]: isHover });
  }

  hoveredChanges(id: string): Observable<boolean> {
    return this.hoveredMap$.pipe(
      scan((acc, curr) => Object.assign({}, acc, curr), {}),
      map((hovered) => hovered[id] ?? false),
    );
  }
}
