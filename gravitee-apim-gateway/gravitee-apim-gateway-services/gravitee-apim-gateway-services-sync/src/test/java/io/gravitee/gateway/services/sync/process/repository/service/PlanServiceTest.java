/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.process.repository.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanServiceTest {

    private PlanService cut;

    @BeforeEach
    public void beforeEach() {
        cut = new PlanService();
    }

    @Nested
    class RegisterTest {

        @Test
        void should_register() {
            cut.register(ApiReactorDeployable.builder().apiId("api").build());
            ReflectionUtils.tryToReadFieldValue(PlanService.class, "plansPerApi", cut).andThen(plansPerApi -> {
                Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                assertThat(map).containsKey("api");
                assertThat(map.get("api")).isEmpty();
                return Try.success(true);
            });
        }

        @Test
        void should_not_register_null() {
            cut.register((ApiReactorDeployable) null);
            ReflectionUtils.tryToReadFieldValue(PlanService.class, "plansPerApi", cut).andThen(plansPerApi -> {
                Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                assertThat(map).isEmpty();
                return Try.success(true);
            });
        }
    }

    @Nested
    class UnregisterTest {

        @Test
        void should_unregister() {
            ReflectionUtils.tryToReadFieldValue(PlanService.class, "plansPerApi", cut)
                .andThen(plansPerApi -> {
                    Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                    map.put("api", Set.of("plan"));
                    return Try.success(plansPerApi);
                })
                .andThen(plansPerApi -> {
                    cut.unregister(ApiReactorDeployable.builder().apiId("api").build());
                    return Try.success(plansPerApi);
                })
                .andThen(plansPerApi -> {
                    Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                    assertThat(map).doesNotContainKey("api");
                    return Try.success(true);
                });
        }

        @Test
        void should_not_register_null() {
            ReflectionUtils.tryToReadFieldValue(PlanService.class, "plansPerApi", cut)
                .andThen(plansPerApi -> {
                    Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                    map.put("api", Set.of("plan"));
                    return Try.success(plansPerApi);
                })
                .andThen(plansPerApi -> {
                    cut.unregister((ApiReactorDeployable) null);
                    return Try.success(plansPerApi);
                })
                .andThen(plansPerApi -> {
                    Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                    assertThat(map).containsKey("api");
                    return Try.success(true);
                });
        }
    }

    @Nested
    class IsDeployedTest {

        @Test
        void should_plan_being_deployed() {
            ReflectionUtils.tryToReadFieldValue(PlanService.class, "plansPerApi", cut)
                .andThen(plansPerApi -> {
                    Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                    map.put("api", Set.of("plan"));
                    return Try.success(plansPerApi);
                })
                .andThen(plansPerApi -> {
                    assertThat(cut.isDeployed("api", "plan")).isTrue();
                    return Try.success(plansPerApi);
                });
        }

        @Test
        void should_not_plan_being_deployed_when_wrong_plan() {
            ReflectionUtils.tryToReadFieldValue(PlanService.class, "plansPerApi", cut)
                .andThen(plansPerApi -> {
                    Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                    map.put("api", Set.of("plan"));
                    return Try.success(plansPerApi);
                })
                .andThen(plansPerApi -> {
                    assertThat(cut.isDeployed("api", "wrong_plan")).isFalse();
                    return Try.success(plansPerApi);
                });
        }

        @Test
        void should_not_plan_being_deployed_when_wrong_api() {
            ReflectionUtils.tryToReadFieldValue(PlanService.class, "plansPerApi", cut)
                .andThen(plansPerApi -> {
                    Map<String, Set<String>> map = (Map<String, Set<String>>) plansPerApi;
                    map.put("api", Set.of("plan"));
                    return Try.success(plansPerApi);
                })
                .andThen(plansPerApi -> {
                    assertThat(cut.isDeployed("wrong_api", "plan")).isFalse();
                    return Try.success(plansPerApi);
                });
        }
    }
}
