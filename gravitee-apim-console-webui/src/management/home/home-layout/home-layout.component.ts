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
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay, startWith } from 'rxjs/operators';

import { TaskService } from '../../../services-ngx/task.service';

@Component({
  selector: 'home-layout',
  templateUrl: './home-layout.component.html',
  styleUrls: ['./home-layout.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeLayoutComponent {
  public taskLabel = this.taskService.getTasks().pipe(
    map((tasks) => `My Tasks <span class="gio-badge-accent">${tasks.page.total_elements}</span>`),
    startWith('Tasks'),
    // If thrown, keep the label as is
    catchError(() => of('Tasks')),
    shareReplay(1),
  );

  public tabs: { label: Observable<string>; routerLink: string }[] = [
    {
      label: of('Overview'),
      routerLink: './overview',
    },
    {
      label: of('API Health Check'),
      routerLink: './apiHealthCheck',
    },
    {
      label: this.taskLabel,
      routerLink: './tasks',
    },
    {
      label: of('Broadcasts'),
      routerLink: './broadcasts',
    },
  ];

  constructor(private readonly taskService: TaskService) {}
}
