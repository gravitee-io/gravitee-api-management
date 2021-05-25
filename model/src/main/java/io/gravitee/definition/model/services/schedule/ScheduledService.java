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
package io.gravitee.definition.model.services.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.Service;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class ScheduledService extends Service {

    private String schedule;

    private long interval = -1;

    private TimeUnit unit;

    public ScheduledService(String name) {
        super(name);
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    @JsonSetter // Ensure backward compatibility
    private void setTrigger(Trigger trigger) {
        this.schedule = convertToCron(trigger.getRate(), trigger.getUnit());
    }

    @JsonIgnore
    public long getInterval() {
        return interval;
    }

    @JsonIgnore
    public TimeUnit getUnit() {
        return unit;
    }

    @JsonProperty // Ensure backward compatibility
    private void setInterval(Long interval) {
        this.interval = interval;
        if (this.unit != null) {
            this.schedule = convertToCron(interval, unit);
        }
    }

    @JsonProperty // Ensure backward compatibility
    private void setUnit(TimeUnit timeUnit) {
        this.unit = timeUnit;
        if (this.interval != -1) {
            this.schedule = convertToCron(interval, timeUnit);
        }
    }

    public static String convertToCron(Long rate, TimeUnit unit) {
        if (rate == null || unit == null) {
            return "";
        }
        String schedule;
        List<String> scheduleList = Arrays.asList("*", "*", "*", "*", "*", "*");
        List<TimeUnit> timeUnits = Arrays.asList(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS);
        int index = timeUnits.indexOf(unit);
        scheduleList.set(index, "*/" + rate);
        schedule = String.join(" ", scheduleList);
        return schedule;
    }
}
