/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { AsyncPipe } from '@angular/common';
import {Component, DestroyRef, inject, input, InputSignal, OnInit} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import {firstValueFrom, Observable, of, tap} from 'rxjs';
import { BreadcrumbService } from 'xng-breadcrumb';

import { LoaderComponent } from '../../../../components/loader/loader.component';
import { PageComponent } from '../../../../components/page/page.component';
import { Page } from '../../../../entities/page/page';
import { PageService } from '../../../../services/page.service';
import {MobileClassDirective} from "../../../../directives/mobile-class.directive";

@Component({
  selector: 'app-documentation-folder',
  imports: [MobileClassDirective],
  standalone: true,
  templateUrl: "./documentation-folder.component.html",
  styleUrl: "./documentation-folder.component.scss",
})
export class DocumentationFolderComponent {
}
