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
import { NgOptimizedImage } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-company-title',
  standalone: true,
  imports: [NgOptimizedImage, RouterLink],
  templateUrl: './company-title.component.html',
  styleUrl: './company-title.component.scss',
})
export class CompanyTitleComponent {
  @Input()
  title: string = 'Developer Portal';
  @Input()
  logo!: string;

  updateLogo() {
    this.logo = 'assets/images/logo.png';
  }
}
