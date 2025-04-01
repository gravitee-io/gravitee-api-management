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
import { Directive, Input, OnInit, TemplateRef, ViewContainerRef } from '@angular/core';

import { GioPermissionService } from './gio-permission.service';

export interface GioPermissionCheckOptions {
  anyOf?: string[];
  noneOf?: string[];
}

@Directive({
  selector: '[gioPermission]',
  standalone: false,
})
export class GioPermissionDirective implements OnInit {
  @Input()
  gioPermission: GioPermissionCheckOptions = {};

  constructor(
    private readonly permissionService: GioPermissionService,
    private templateRef: TemplateRef<unknown>,
    private viewContainer: ViewContainerRef,
  ) {}

  ngOnInit(): void {
    if (this.gioPermission.anyOf && this.gioPermission.noneOf) {
      throw new Error('You should only set `anyOf` or `noneOf` but not both at the same time.');
    }

    this.viewContainer.clear();

    if (
      (this.gioPermission.anyOf && this.permissionService.hasAnyMatching(this.gioPermission.anyOf)) ||
      (this.gioPermission.noneOf && !this.permissionService.hasAnyMatching(this.gioPermission.noneOf))
    ) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }
}
