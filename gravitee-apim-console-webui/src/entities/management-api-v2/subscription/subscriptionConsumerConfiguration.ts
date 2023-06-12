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

/**
 * Consumer configuration associated to the subscription in case it is attached to a push plan.
 */
export interface SubscriptionConsumerConfiguration {
  /**
   * The id of the targeted entrypoint
   */
  entrypointId: string;
  /**
   * The channel to consume
   */
  channel?: string;
  /**
   * The configuration to use at subscription time to push to the target service.
   */
  entrypointConfiguration?: any;
}
