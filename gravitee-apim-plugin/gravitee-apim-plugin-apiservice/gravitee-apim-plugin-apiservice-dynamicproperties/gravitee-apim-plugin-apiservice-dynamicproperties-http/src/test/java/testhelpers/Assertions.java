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
package testhelpers;

import static io.gravitee.apim.plugin.apiservice.dynamicproperties.http.HttpDynamicPropertiesService.HTTP_DYNAMIC_PROPERTIES_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static testhelpers.Fixtures.MY_API;

import io.gravitee.apim.rest.api.common.apiservices.events.DynamicPropertiesEvent;
import io.gravitee.apim.rest.api.common.apiservices.events.ManagementApiServiceEvent;
import io.gravitee.common.event.Event;
import io.gravitee.definition.model.v4.property.Property;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.assertj.core.api.ObjectAssert;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Assertions {

    public static class ScheduledJobAssertions {

        private ScheduledJobAssertions() {}

        public static ObjectAssert<Disposable> assertScheduledJobIsRunning(AtomicReference<Disposable> scheduledJob) {
            return assertThat(scheduledJob.get())
                .as("Background job is running")
                .isNotNull()
                .satisfies(disposable -> assertThat(disposable.isDisposed()).isFalse());
        }

        public static ObjectAssert<Disposable> assertScheduledJobIsDisposed(AtomicReference<Disposable> scheduledJob) {
            return assertThat(scheduledJob.get())
                .as("Background job is disposed")
                .isNotNull()
                .satisfies(disposable -> assertThat(disposable.isDisposed()).isTrue());
        }
    }

    @AllArgsConstructor
    public static class PropertyEventAssertions {

        private Event<ManagementApiServiceEvent, DynamicPropertiesEvent> propertyEvent;

        public static PropertyEventAssertions assertThatEvent(Event<ManagementApiServiceEvent, DynamicPropertiesEvent> event) {
            return new PropertyEventAssertions(event);
        }

        public void contains(List<Property> expectedProperties) {
            assertThat(propertyEvent.type()).isEqualTo(ManagementApiServiceEvent.DYNAMIC_PROPERTY_UPDATE);
            assertThat(propertyEvent.content()).satisfies(property -> {
                assertThat(property.pluginId()).isEqualTo(HTTP_DYNAMIC_PROPERTIES_TYPE);
                assertThat(property.apiId()).isEqualTo(MY_API);
                assertThat(property.dynamicProperties()).containsExactlyElementsOf(expectedProperties);
            });
        }
    }
}
