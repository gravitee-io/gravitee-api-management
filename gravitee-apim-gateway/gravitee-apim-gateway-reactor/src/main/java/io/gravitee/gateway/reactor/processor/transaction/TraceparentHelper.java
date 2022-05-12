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
package io.gravitee.gateway.reactor.processor.transaction;

import java.util.UUID;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TraceparentHelper {

    private static final String FLAGS = "-00";
    private static final String VERSION = "00-";

    public static String leftPad(String value, int length) {
        if (value.length() < length) {
            StringBuilder builder = new StringBuilder(length);
            for (int i = value.length(); i < length; ++i) {
                builder.append('0');
            }
            builder.append(value);
            return builder.toString();
        }
        return value;
    }

    public static String buildTraceparentFrom(UUID uuid) {
        String parentId = Long.toHexString(uuid.getLeastSignificantBits()); // 8-byte array (16 hex digits) - all bytes as zero is forbidden
        String traceId = Long.toHexString(uuid.getMostSignificantBits()) + parentId; // 16-byte array  (32 hex digits) - all bytes as zero is forbidden
        return VERSION + leftPad(traceId, 32) + "-" + leftPad(parentId, 16) + FLAGS;
    }

    public static boolean isValid(String traceparent) {
        String[] array = traceparent.split("-");

        return (
            isHexString(array[0], 2, 'f') && // 1 byte arr => 2 hex char
            isHexString(array[1], 32, '0') && // 16 bytes arr => 32 hex char
            isHexString(array[2], 16, '0') && // 8 bytes arr => 16 hex char
            (array[3].length() == 2)
        );
    }

    /**
     * check if the value string contains only expectedLength hex digits in lower case.
     * all digits as 'notAll' is invalid
     * @param value
     * @param expectedLength
     * @param notAll
     * @return
     */
    private static final boolean isHexString(String value, int expectedLength, char notAll) {
        boolean sameCharForbidden = true;
        for (char c : value.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
            sameCharForbidden = sameCharForbidden && (c == notAll);
            --expectedLength;
        }
        return 0 == expectedLength && !sameCharForbidden;
    }
}
