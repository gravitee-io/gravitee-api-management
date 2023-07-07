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
package io.gravitee.repository.jdbc.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(Parameterized.class)
public class FieldUtilsTest {

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "", "" },
                { "already_snake_case", "already_snake_case" },
                { null, null },
                { "createdAt", "created_at" },
                { "subject", "subject" },
            }
        );
    }

    @Parameterized.Parameter(0)
    public String actual;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void convertToSnakeCase() {
        String result = FieldUtils.toSnakeCase(actual);
        assertEquals(expected, result);
    }
}
