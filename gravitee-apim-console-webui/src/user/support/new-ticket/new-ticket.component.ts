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
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { combineLatest, EMPTY, Subject } from 'rxjs';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute, Router } from '@angular/router';

import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { Api } from '../../../entities/management-api-v2';
import { ApplicationService } from '../../../services-ngx/application.service';
import { Application } from '../../../entities/application/Application';
import { NewTicket } from '../../../entities/ticket/newTicket';
import { TicketService } from '../../../services-ngx/ticket.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

type TicketForm = FormGroup<{
  api: FormControl<string>;
  application: FormControl<string>;
  subject: FormControl<string>;
  content: FormControl<string>;
  copyToSender: FormControl<boolean>;
}>;

@Component({
  selector: 'new-ticket',
  templateUrl: './new-ticket.component.html',
  styleUrls: ['./new-ticket.component.scss'],
  standalone: false,
})
export class NewTicketComponent implements OnInit {
  ticketForm: TicketForm;
  apis: Api[] = [];
  applications: Application[] = [];

  private unsubscribe$: Subject<void> = new Subject<void>();

  constructor(
    private readonly apiService: ApiV2Service,
    private readonly applicationService: ApplicationService,
    private readonly ticketService: TicketService,
    private readonly snackBarService: SnackBarService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    // FIXME: Handle pagination on APIs...
    combineLatest([this.apiService.search({}, null, 1, 100), this.applicationService.list(null, null, null, 1, 100)])
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe(([apiResponse, applicationResponse]) => {
        this.apis = apiResponse.data;
        this.applications = applicationResponse.data;
      });

    this.ticketForm = new FormGroup({
      api: new FormControl(''),
      application: new FormControl(''),
      subject: new FormControl('', [Validators.required]),
      content: new FormControl('', [Validators.required]),
      copyToSender: new FormControl(false) as FormControl<boolean>,
    });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    if (this.ticketForm.invalid) {
      return;
    }

    const ticketFormValue = this.ticketForm.getRawValue();

    const newTicket: NewTicket = {
      subject: ticketFormValue.subject,
      content: ticketFormValue.content,
      copyToSender: ticketFormValue.copyToSender,
    };

    if (ticketFormValue.api && ticketFormValue.api !== '') {
      newTicket.api = ticketFormValue.api;
    }

    if (ticketFormValue.application && ticketFormValue.application !== '') {
      newTicket.application = ticketFormValue.application;
    }

    this.ticketService
      .create(newTicket)
      .pipe(
        tap(() => {
          this.snackBarService.success('New ticket successfully created!');
          this.router.navigate(['..'], { relativeTo: this.activatedRoute });
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
