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
import { Component, inject, Input } from '@angular/core';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

import { IntegrationsService } from '../../../services-ngx/integrations.service';

@Component({
  selector: 'portal-header',
  standalone: true,
  imports: [GioBannerModule],
  templateUrl: './portal-header.component.html',
  styleUrl: './portal-header.component.scss',
})
export class PortalHeaderComponent {
  @Input()
  title!: string;

  @Input()
  subtitle?: string;

  @Input()
  showTechPreviewMessage: boolean = true;

  techPreviewMessage = inject(IntegrationsService).bannerMessages.techPreview;
}
