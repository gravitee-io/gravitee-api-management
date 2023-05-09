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
import { Connection, Writer, SchemaRegistry, SCHEMA_TYPE_BYTES } from 'k6/x/kafka';
import { k6Options } from '@env/environment';
import { GatewayTestData } from '@lib/test-api';
import { randomString } from '@helpers/random.helper';
import { generatePayloadInKB } from '@helpers/k6.helper';

const schemaRegistry = new SchemaRegistry();
const connection = new Connection({
  // ConnectionConfig object
  address: k6Options.apim.kafkaBoostrapServer,
});

const kafkaTopic = k6Options.apim.kafkaInjector.topic;

const writer = new Writer({
  brokers: [k6Options.apim.kafkaBoostrapServer],
  topic: kafkaTopic,
  autoCreateTopic: true,
  compression: k6Options.apim.kafkaInjector.compression,
  requiredAcks: k6Options.apim.kafkaInjector.acks,
});

export const options = k6Options;
options['thresholds'] = {
  // Base thresholds to see if the writer or reader is working
  kafka_writer_error_count: ['count == 0'],
  kafka_reader_error_count: ['count == 0'],
};

export function setup(): GatewayTestData {
  const message = generatePayloadInKB(k6Options.apim.kafkaInjector.messageSizeInKB);
  return {
    // The data type of the value is a byte array
    msg: schemaRegistry.serialize({
      data: Array.from(JSON.stringify(message), (x) => x.charCodeAt(0)),
      schemaType: SCHEMA_TYPE_BYTES,
    }),
  };
}

export default (data: GatewayTestData) => {
  writer.produce({
    messages: [
      {
        // The data type of the key is a string
        key: schemaRegistry.serialize({
          data: Array.from('id-' + randomString(), (x) => x.charCodeAt(0)),
          schemaType: SCHEMA_TYPE_BYTES,
        }),
        value: data.msg,
      },
    ],
  });
};

export function teardown() {
  writer.close();
  connection.close();
}
