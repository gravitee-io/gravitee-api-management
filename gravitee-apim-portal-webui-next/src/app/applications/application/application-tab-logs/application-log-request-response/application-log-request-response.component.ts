import {Component, computed, input, Input, Signal} from '@angular/core';
import {Log, LogMetadataApi, LogMetadataPlan} from "../../../../../entities/log/log";
import {CopyCodeComponent} from "../../../../../components/copy-code/copy-code.component";
import {DatePipe} from "@angular/common";
import {MatCard, MatCardContent, MatCardModule} from "@angular/material/card";
import {
  MatExpansionModule,
  MatExpansionPanel,
  MatExpansionPanelHeader,
  MatExpansionPanelTitle
} from "@angular/material/expansion";

interface LogVM extends Log {
  apiName: string;
  planName: string;
  requestHeaders: { key: string; value: string }[];
  responseHeaders: { key: string; value: string }[];
}

@Component({
  selector: 'app-application-log-request-response',
  standalone: true,
  imports: [
    CopyCodeComponent,
    DatePipe,
    MatCardModule,
    MatExpansionModule
  ],
  templateUrl: './application-log-request-response.component.html',
  styleUrl: './application-log-request-response.component.scss'
})
export class ApplicationLogRequestResponseComponent {
  log = input.required<Log>();
  logVM: Signal<LogVM> = computed(() => {
    const log = this.log();

    const apiName = log.metadata?.[log.api]
      ? `${(log.metadata[log.api] as LogMetadataApi).name} (${(log.metadata[log.api] as LogMetadataApi).version})`
      : '';
    const apiType = (log.metadata?.[log.api] as LogMetadataApi)?.apiType;
    const planName = log.metadata?.[log.plan] ? (log.metadata[log.plan] as LogMetadataPlan).name : '';
    const requestHeaders = log.request?.headers
      ? Object.entries(log.request.headers).map(keyValueArray => ({ key: keyValueArray[0], value: keyValueArray[1] }))
      : [];
    const responseHeaders = log.response?.headers
      ? Object.entries(log.response.headers).map(keyValueArray => ({ key: keyValueArray[0], value: keyValueArray[1] }))
      : [];
    return { ...log, apiName, apiType, planName, requestHeaders, responseHeaders };
  })
}
