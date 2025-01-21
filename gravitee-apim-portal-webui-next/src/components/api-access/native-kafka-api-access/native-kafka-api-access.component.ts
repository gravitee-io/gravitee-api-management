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
import { Component, computed, input } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatTab, MatTabGroup } from '@angular/material/tabs';

import { PlanSecurityEnum } from '../../../entities/plan/plan';
import { CopyCodeComponent } from '../../copy-code/copy-code.component';

@Component({
  selector: 'app-native-kafka-api-access',
  imports: [CopyCodeComponent, MatIcon, MatTabGroup, MatTab],
  templateUrl: './native-kafka-api-access.component.html',
  styleUrl: './native-kafka-api-access.component.scss',
})
export class NativeKafkaApiAccessComponent {
  planSecurity = input.required<PlanSecurityEnum>();
  entrypointUrl = input.required<string>();
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
      `  --bootstrap-server ${this.computedEntrypointUrl()} \\\n` +
      `  --topic test-topic \\\n` +
      `  --from-beginning \\\n` +
      `  --consumer.config ${this.configurationFileName}`,
  );

  producerCommand = computed(
    () =>
      `./bin/kafka-console-producer.sh \\\n` +
      `  --bootstrap-server ${this.computedEntrypointUrl()} \\\n` +
      `  --topic test-topic \\\n` +
      `  --producer.config ${this.configurationFileName}`,
  );

  private computedEntrypointUrl = computed(() => (this.entrypointUrl().length ? this.entrypointUrl() : 'localhost:9092'));
  private computedClientId = computed(() => (this.clientId().length ? this.clientId() : '{{ CLIENT_ID }}'));

  private trustStoreConfig =
    '\n' +
    '# Configure your truststore if you are using a certificate\n' +
    '# not automatically trusted by your installation\n' +
    '# ssl.truststore.location={{ PATH_TO_TRUSTSTORE_JKS }}\n' +
    '# ssl.truststore.password={{ TRUSTSTORE_PASSWORD }}';
}
