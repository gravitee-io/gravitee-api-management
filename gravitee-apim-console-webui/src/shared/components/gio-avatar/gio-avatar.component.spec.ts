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
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GioAvatarComponent } from './gio-avatar.component';
import { GioAvatarModule } from './gio-avatar.module';

describe('GioAvatarComponent', () => {
  let component: GioAvatarComponent;
  let fixture: ComponentFixture<GioAvatarComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioAvatarModule],
    });
    fixture = TestBed.createComponent(GioAvatarComponent);
    component = fixture.componentInstance;
  });

  it('should complete avatar with jdenticon', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.gio-avatar').innerHTML).toMatchSnapshot();
  });

  it('should display avatar image', () => {
    component.src = 'https://i.pravatar.cc/500';
    fixture.componentInstance.ngAfterViewInit();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.gio-avatar').innerHTML).toMatchSnapshot();
  });
});
