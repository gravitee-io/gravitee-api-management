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
package io.gravitee.repository.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {

    public static Date parse(final String stringDate) {
        try {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return simpleDateFormat.parse(stringDate);
        } catch (final ParseException pe) {
            throw new RuntimeException(pe);
        }
    }

    /*
     * Due to SQL Server accuracy on DATETIME, dates must be considered as "equals" even if there is a small difference
     * https://stackoverflow.com/questions/41774428/timestampequals-fails-if-timestamp-is-mapped-to-from-database
     *
     */
    public static boolean compareDate(String expectedDateToParse, Date actualDate) {
        return compareDate(parse(expectedDateToParse), actualDate);
    }

    public static boolean compareDate(Date expectedDate, Date actualDate) {
        if (actualDate == null) {
            return expectedDate == null;
        }
        if (expectedDate == null) {
            return false;
        }
        return compareDate(expectedDate.getTime(), actualDate.getTime());
    }

    public static boolean compareDate(long expectedTimestamp, long actualTimestamp) {
        return Math.abs(expectedTimestamp - actualTimestamp) < 3;
    }
}
