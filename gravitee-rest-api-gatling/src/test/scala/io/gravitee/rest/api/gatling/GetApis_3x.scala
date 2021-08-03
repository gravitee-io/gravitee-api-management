/**
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
package io.gravitee.rest.api.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class Apis3xSimulation extends Simulation {

  val httpProtocol = http
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling Simulator")
    .disableFollowRedirect

  val scn = scenario("Management Scenario") // A scenario is a chain of requests and pauses
    .exec(
      http("get APIs")
        .get("http://localhost:8083/management/organizations/DEFAULT/environments/DEFAULT/apis")
        .basicAuth("admin", "admin")
        .check(status.is(200))
    )
    

  setUp(scn.inject(constantUsersPerSec(3).during(20.seconds)).protocols(httpProtocol))
}
