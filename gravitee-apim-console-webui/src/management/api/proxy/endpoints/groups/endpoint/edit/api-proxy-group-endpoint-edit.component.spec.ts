import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';

import { ApiProxyGroupEndpointEditComponent } from './api-proxy-group-endpoint-edit.component';

import { GioHttpTestingModule } from '../../../../../../../shared/testing';
import { UIRouterState, UIRouterStateParams } from '../../../../../../../ajs-upgraded-providers';
import { ApiProxyGroupsEndpointModule } from '../api-proxy-groups-endpoint.module';

describe('ApiProxyGroupEndpointEditComponent', () => {
  const API_ID = 'apiId';
  const DEFAULT_GROUP_NAME = 'default-group';
  const DEFAULT_ENDPOINT_NAME = 'default-endpoint';
  const fakeUiRouter = { go: jest.fn() };
  let fixture: ComponentFixture<ApiProxyGroupEndpointEditComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, ApiProxyGroupsEndpointModule, MatIconTestingModule],
      providers: [
        { provide: UIRouterStateParams, useValue: { apiId: API_ID, groupName: DEFAULT_GROUP_NAME, endpointName: DEFAULT_ENDPOINT_NAME } },
        { provide: UIRouterState, useValue: fakeUiRouter },
      ],
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
    httpTestingController.verify();
  });

  describe('Edit mode', () => {
    beforeEach(async () => {
      TestBed.compileComponents();
      fixture = TestBed.createComponent(ApiProxyGroupEndpointEditComponent);
      loader = TestbedHarnessEnvironment.loader(fixture);
      httpTestingController = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
    });

    it('should go back to endpoints', async () => {
      const routerSpy = jest.spyOn(fakeUiRouter, 'go');

      await loader.getHarness(MatButtonHarness.with({ selector: '[mattooltip="Go back"]' })).then((button) => button.click());

      expect(routerSpy).toHaveBeenCalledWith('management.apis.detail.proxy.ng-endpoints', { apiId: API_ID }, undefined);
    });
  });
});
