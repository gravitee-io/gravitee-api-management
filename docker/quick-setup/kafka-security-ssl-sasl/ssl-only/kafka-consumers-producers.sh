#!/usr/bin/env bash
#
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


docker run -it --rm --network kafka-cluster-network confluentinc/cp-kafka:5.0.1 \
kafka-topics --zookeeper zookeeper-1:22181 --create --topic test --partitions 10 --replication-factor 3

#Console producer with SSL files mapped in the container
docker run -it --rm -v ${KAFKA_SSL_SECRETS_DIR}/producer:/etc/kafka/secrets \
-v ${KAFKA_SSL_SECRETS_DIR}/host.producer.ssl.config:/etc/kafka/secrets/host.producer.ssl.config  \
--network kafka-cluster-network confluentinc/cp-kafka:5.0.1  kafka-console-producer \
--broker-list kafka-broker-1:19093,kafka-broker-2:29093,kafka-broker-3:39093 --topic test \
--producer.config /etc/kafka/secrets/host.producer.ssl.config


#Console consumer with SSL files mapped in the container
docker run -it --rm -v ${KAFKA_SSL_SECRETS_DIR}/consumer:/etc/kafka/secrets \
-v ${KAFKA_SSL_SECRETS_DIR}/host.consumer.ssl.config:/etc/kafka/secrets/host.consumer.ssl.config \
--network kafka-cluster-network confluentinc/cp-kafka:5.0.1 \
kafka-console-consumer --bootstrap-server kafka-broker-1:19093,kafka-broker-2:29093,kafka-broker-3:39093 --topic test --from-beginning \
--consumer.config /etc/kafka/secrets/host.consumer.ssl.config
