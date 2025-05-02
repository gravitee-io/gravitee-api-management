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
import { Component, computed, effect, input, signal } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTab, MatTabGroup } from '@angular/material/tabs';

import { ConfigurationPortal } from '../../../entities/configuration/configuration-portal';
import { PlanSecurityEnum } from '../../../entities/plan/plan';
import { ConfigService } from '../../../services/config.service';
import { CopyCodeIconComponent } from '../../copy-code/copy-code-icon/copy-code-icon/copy-code-icon.component';
import { CopyCodeComponent } from '../../copy-code/copy-code.component';

@Component({
  selector: 'app-native-kafka-api-access',
  imports: [CopyCodeComponent, MatIcon, MatTabGroup, MatTab, MatFormFieldModule, MatSelectModule, CopyCodeIconComponent],
  templateUrl: './native-kafka-api-access.component.html',
  styleUrl: './native-kafka-api-access.component.scss',
})
export class NativeKafkaApiAccessComponent {
  planSecurity = input.required<PlanSecurityEnum>();

  entrypointUrls = input.required<string[]>();

  selectedEntrypointHost = signal<string>('localhost:9092');

  apiKey = input('');
  apiKeyConfigUsername = input('');
  clientId = input('');

  configurationFileName: string = 'connect.properties';

  oAuthBearerConfig = computed(
    () => `security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
sasl.login.callback.handler.class=org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerLoginCallbackHandler
sasl.oauthbearer.token.endpoint.url={{ TOKEN_ENDPOINT_URL }}
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required \\
  clientId="${this.computedClientId()}" \\
  clientSecret="{{ CLIENT_SECRET }}";
${this.trustStoreConfig}`,
  );

  plainConfig = computed(
    () => `security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \\
  username="${this.apiKeyConfigUsername()}" \\
  password="${this.apiKey()}";
${this.trustStoreConfig}`,
  );

  scram256Config = computed(
    () => `security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-256
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \\
  username="${this.apiKeyConfigUsername()}" \\
  password="${this.apiKey()}";
${this.trustStoreConfig}`,
  );

  scram512Config = computed(
    () => `security.protocol=SASL_SSL
sasl.mechanism=SCRAM-SHA-512
sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required \\
  username="${this.apiKeyConfigUsername()}" \\
  password="${this.apiKey()}";
${this.trustStoreConfig}`,
  );

  sslConfig = computed(
    () => `security.protocol=SSL
${this.trustStoreConfig}`,
  );

  consumerCommand = computed(
    () =>
      `./bin/kafka-console-consumer.sh \\\n` +
      `  --bootstrap-server ${this.selectedEntrypointHost()} \\\n` +
      `  --topic test-topic \\\n` +
      `  --from-beginning \\\n` +
      `  --consumer.config ${this.configurationFileName}`,
  );

  producerCommand = computed(
    () =>
      `./bin/kafka-console-producer.sh \\\n` +
      `  --bootstrap-server ${this.selectedEntrypointHost()} \\\n` +
      `  --topic test-topic \\\n` +
      `  --producer.config ${this.configurationFileName}`,
  );

  settings = signal<ConfigurationPortal | undefined>(undefined);
  kafkaSaslMechanisms = computed(() => this.parseKafkaSaslMechanisms(this.settings()?.kafkaSaslMechanisms));

  private computedClientId = computed(() => (this.clientId().length ? this.clientId() : '{{ CLIENT_ID }}'));

  private trustStoreConfig =
    '\n' +
    '# Configure your truststore if you are using a certificate\n' +
    '# not automatically trusted by your installation\n' +
    '# ssl.truststore.location={{ PATH_TO_TRUSTSTORE_JKS }}\n' +
    '# ssl.truststore.password={{ TRUSTSTORE_PASSWORD }}';

  constructor(private configService: ConfigService) {
    effect(() => {
      this.selectedEntrypointHost.set(this.entrypointUrls().length ? this.entrypointUrls()[0] : this.selectedEntrypointHost());
      this.settings.set(this.configService.configuration.portal);
    });
  }

  getKafkaConfigForMechanism(mech: string): string {
    switch (mech) {
      case 'PLAIN':
        return this.plainConfig();
      case 'SCRAM-SHA-256':
        return this.scram256Config();
      case 'SCRAM-SHA-512':
        return this.scram512Config();
      default:
        return '';
    }
  }

  generateIdForMechanism(mech: string): string {
    return `native-kafka-api-key-${mech.toLowerCase()}-properties`;
  }

  private parseKafkaSaslMechanisms(value: string[] | null | undefined) {
    if (value?.length) {
      return value
        .map(s => ({
          mechanism: s,
          id: this.generateIdForMechanism(s),
          config: this.getKafkaConfigForMechanism(s),
        }))
        .filter(Boolean);
    }
    return [];
  }
}
