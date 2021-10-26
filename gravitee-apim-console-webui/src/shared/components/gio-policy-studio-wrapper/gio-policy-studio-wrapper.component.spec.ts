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
import { HttpTestingController } from '@angular/common/http/testing';

import { GioPolicyStudioWrapperComponent } from './gio-policy-studio-wrapper.component';
import { GioPolicyStudioWrapperModule } from './gio-policy-studio-wrapper.module';

import { CONSTANTS_TESTING, GioHttpTestingModule } from '../../testing';
import { fakeFlowConfigurationSchema } from '../../../entities/flow/configurationSchema.fixture';

describe('GioPolicyStudioWrapperComponent', () => {
  let component: GioPolicyStudioWrapperComponent;
  let fixture: ComponentFixture<GioPolicyStudioWrapperComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [GioPolicyStudioWrapperModule, GioHttpTestingModule],
    });
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture = TestBed.createComponent(GioPolicyStudioWrapperComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should match snapshot', () => {
    httpTestingController
      .expectOne({
        method: 'GET',
        url: `${CONSTANTS_TESTING.org.baseURL}/configuration/flows/configuration-schema`,
      })
      .flush(fakeFlowConfigurationSchema());

    expect(component).toBeDefined();
  });
});
