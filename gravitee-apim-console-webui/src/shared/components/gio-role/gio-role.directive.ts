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

import { GioRoleService } from './gio-role.service';

export interface GioRoleCheckOptions {
  anyOf?: { scope: string; name: string }[];
  noneOf?: { scope: string; name: string }[];
}

@Directive({
  selector: '[gioRole]',
  standalone: false,
})
export class GioRoleDirective implements OnInit {
  @Input()
  gioRole: GioRoleCheckOptions = {};

  constructor(
    private readonly roleService: GioRoleService,
    private templateRef: TemplateRef<unknown>,
    private viewContainer: ViewContainerRef,
  ) {}

  ngOnInit(): void {
    if (this.gioRole.anyOf && this.gioRole.noneOf) {
      throw new Error('You should only set `anyOf` or `noneOf` but not both at the same time.');
    }

    this.viewContainer.clear();
    if (
      (this.gioRole.anyOf && this.roleService.hasAnyMatching(this.gioRole.anyOf)) ||
      (this.gioRole.noneOf && !this.roleService.hasAnyMatching(this.gioRole.noneOf))
    ) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }
}
