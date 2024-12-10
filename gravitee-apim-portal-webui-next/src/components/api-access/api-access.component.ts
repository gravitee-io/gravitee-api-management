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
import { Component, Input, OnInit } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader } from '@angular/material/card';

import { PlanSecurityEnum } from '../../entities/plan/plan';
import { SubscriptionStatusEnum } from '../../entities/subscription/subscription';
import { ConfigService } from '../../services/config.service';
import { CopyCodeComponent } from '../copy-code/copy-code.component';
import {ApiType} from "../../entities/api/api";

@Component({
  selector: 'app-api-access',
  standalone: true,
  imports: [MatCard, MatCardContent, MatCardHeader, CopyCodeComponent],
  templateUrl: './api-access.component.html',
  styleUrl: './api-access.component.scss',
})
export class ApiAccessComponent implements OnInit {
  @Input()
  planSecurity!: PlanSecurityEnum;

  @Input()
  subscriptionStatus?: SubscriptionStatusEnum;

  @Input()
  apiKey?: string;

  @Input()
  apiType?: ApiType;

  @Input()
  entrypointUrl?: string;

  @Input()
  clientId?: string;

  @Input()
  clientSecret?: string;

  curlCmd: string = '';

//   oauthbearer: string = ```security.protocol=SASL_SSL
//
// sasl.mechanism=OAUTHBEARER
// sasl.login.callback.handler.class=org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler
// sasl.login.connect.timeout.ms=15000
// sasl.oauthbearer.token.endpoint.url=https://am.gateway.master.gravitee.dev/test-jh/oauth/token
// sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required \\
//   clientId="kafka-client" \\
//   clientSecret="kafka-secret";
//
// # Configure your truststore if you self-sign certificates
// # ssl.truststore.location=/path/to.truststore.jks
// # ssl.truststore.password=<your_truststore_password>```;

  kafkaClientConfiguration = {
    oAuthBearer: "security.protocol=SASL_SSL\n" +
      "sasl.mechanism=OAUTHBEARER\n" +
      "\n" +
      "sasl.mechanism=OAUTHBEARER\n" +
      "sasl.login.callback.handler.class=org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler\n" +
      "sasl.login.connect.timeout.ms=15000\n" +
      "sasl.oauthbearer.token.endpoint.url=<token-introspection-url>\n" +
      "sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required \\\n" +
      "  clientId=\"<your-client-id>\" \\\n" +
      "  clientSecret=\"<your-client-secret>\";\n" +
      "\n" +
      "# Configure your truststore if using self-sign certificates\n" +
      "# ssl.truststore.location=/path/to.truststore.jks\n" +
    "# ssl.truststore.password=<your_truststore_password>",
    apiKey: {
      plain: "blah",
      scram256: "bloop",
      scram512: "beep"
    }
  }

  constructor(private configService: ConfigService) {}

  ngOnInit(): void {
    if (this.entrypointUrl && this.apiKey) {
      this.curlCmd = this.formatCurlCommandLine(this.entrypointUrl, this.apiKey);
    }
  }

  private formatCurlCommandLine(entrypointUrl: string, apiKey?: string): string {
    if (!entrypointUrl) {
      return '';
    }
    const apiKeyHeader =
      apiKey && this.configService.configuration.portal?.apikeyHeader
        ? `--header "${this.configService.configuration.portal.apikeyHeader}: ${apiKey}" `
        : '';
    return `curl ${apiKeyHeader}${entrypointUrl}`;
  }
}
