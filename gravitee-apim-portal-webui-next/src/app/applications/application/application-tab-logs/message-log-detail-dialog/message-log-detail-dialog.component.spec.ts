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
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component, input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { By } from '@angular/platform-browser';

import { MessageLogDetailDialogComponent, MessageLogDetailDialogData } from './message-log-detail-dialog.component';
import { MessageLogDetailDialogHarness } from './message-log-detail-dialog.harness';
import { AppTestingModule } from '../../../../../testing/app-testing.module';
import { DivHarness } from '../../../../../testing/div.harness';

@Component({
  selector: 'app-more-filters-dialog-test',
  template: ` <button (click)="openDialog()">Open sesame</button>`,
  standalone: true,
})
class TestComponent {
  result = input.required<MessageLogDetailDialogData>();

  constructor(private readonly matDialog: MatDialog) {}

  public openDialog() {
    this.matDialog
      .open<MessageLogDetailDialogComponent, MessageLogDetailDialogData, void>(MessageLogDetailDialogComponent, {
        id: 'message-log-detail-dialog',
        data: this.result(),
      })
      .afterClosed()
      .subscribe();
  }
}

const FAKE_DATA = (): MessageLogDetailDialogData => ({
  requestId: 'request-id',
  correlationId: 'correlation-id',
  clientId: 'client-id',
  operation: 'SUBSCRIBE',
  timestamp: '0',
  entrypoint: {
    name: 'Cats Rule',
    headers: {},
    metadata: {},
  },
  endpoint: {
    name: 'Dogs Drool',
    headers: {},
    metadata: {},
  },
});

describe('MessageLogDetailDialogComponent', () => {
  let fixture: ComponentFixture<TestComponent>;
  let rootHarnessLoader: HarnessLoader;
  let componentHarness: MessageLogDetailDialogHarness;

  const init = async (dialogData: MessageLogDetailDialogData) => {
    await TestBed.configureTestingModule({
      imports: [TestComponent, MessageLogDetailDialogComponent, AppTestingModule],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true, // This traps focus checks and so avoid warnings when dealing with
          isTabbable: () => true, // This traps focus checks and so avoid warnings when dealing with
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    rootHarnessLoader = TestbedHarnessEnvironment.documentRootLoader(fixture);

    fixture.componentRef.setInput('result', dialogData);

    fixture.detectChanges();

    const btn: HTMLButtonElement = fixture.debugElement.query(By.css('button')).nativeElement;
    expect(btn).toBeTruthy();
    btn.click();

    componentHarness = await rootHarnessLoader.getHarness(MessageLogDetailDialogHarness);

    fixture.detectChanges();
  };

  it('should display basic information', async () => {
    const messageLog = FAKE_DATA();
    await init(messageLog);

    const timestamp = await rootHarnessLoader.getHarness(
      DivHarness.with({ selector: '.message-log-detail__information-row', text: /Timestamp:.*/ }),
    );
    expect(await timestamp.getText()).toContain(messageLog.timestamp);

    const requestId = await rootHarnessLoader.getHarness(
      DivHarness.with({ selector: '.message-log-detail__information-row', text: /Request ID:.*/ }),
    );
    expect(await requestId.getText()).toContain(messageLog.requestId);

    const clientId = await rootHarnessLoader.getHarness(
      DivHarness.with({ selector: '.message-log-detail__information-row', text: /Client ID:.*/ }),
    );
    expect(await clientId.getText()).toContain(messageLog.clientId);

    const correlationId = await rootHarnessLoader.getHarness(
      DivHarness.with({ selector: '.message-log-detail__information-row', text: /Correlation ID:.*/ }),
    );
    expect(await correlationId.getText()).toContain(messageLog.correlationId);

    const operation = await rootHarnessLoader.getHarness(
      DivHarness.with({ selector: '.message-log-detail__information-row', text: /Operation:.*/ }),
    );
    expect(await operation.getText()).toContain('Subscribe');
  });

  it('should display entrypoint and endpoint when defined', async () => {
    const messageLog = FAKE_DATA();
    await init(messageLog);

    expect(await componentHarness.isEntrypointCardShown()).toEqual(true);

    const entrypointCard = await componentHarness.getEntrypointCard();
    const entrypointCardText = await entrypointCard.getText();
    expect(entrypointCardText).toContain('Entrypoint:' + messageLog.entrypoint.name);

    expect(await componentHarness.isEndpointCardShown()).toEqual(true);

    const endpointCard = await componentHarness.getEndpointCard();
    const endpointCardText = await endpointCard.getText();
    expect(endpointCardText).toContain('Endpoint:' + messageLog.endpoint.name);
  });

  it('should not display entrypoint and endpoint when undefined', async () => {
    const messageLog = FAKE_DATA();
    messageLog.entrypoint = {};
    messageLog.endpoint = {};

    await init(messageLog);
    expect(await componentHarness.isEntrypointCardShown()).toEqual(false);
    expect(await componentHarness.isEndpointCardShown()).toEqual(false);
  });
});
