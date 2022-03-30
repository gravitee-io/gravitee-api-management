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
package io.gravitee.definition.jackson.services.core;

import io.gravitee.definition.jackson.datatype.services.core.deser.ScheduledServiceDeserializer;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class ScheduledServiceDeserializerTest {

    @Test
    public void shouldConvertCronForEvery10Seconds() {
        final String every10SecondsCRON = ScheduledServiceDeserializer.convertToCron(10L, TimeUnit.SECONDS);
        Assert.assertEquals("*/10 * * * * *", every10SecondsCRON);
    }

    @Test
    public void shouldConvertCronForEvery5Minutes() {
        final String every5MinutesCRON = ScheduledServiceDeserializer.convertToCron(5L, TimeUnit.MINUTES);
        Assert.assertEquals("0 */5 * * * *", every5MinutesCRON);
    }

    @Test
    public void shouldConvertCronForEvery2Hours() {
        final String every2HoursCRON = ScheduledServiceDeserializer.convertToCron(2L, TimeUnit.HOURS);
        Assert.assertEquals("0 0 */2 * * *", every2HoursCRON);
    }

    @Test
    public void shouldConvertCronForEveryDay() {
        final String everyDayCRON = ScheduledServiceDeserializer.convertToCron(1L, TimeUnit.DAYS);
        Assert.assertEquals("0 0 0 */1 * *", everyDayCRON);
    }
}
