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
package io.gravitee.management.rest.resource.param;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RangesParam extends AbstractParam<List<Range>> {

    public RangesParam(String param) throws WebApplicationException {
        super(param);
    }

    @Override
    protected List<Range> parse(String param) throws Throwable {
        try {
            if (param != null) {
                String [] inputRanges = param.split(";");
                List<Range> ranges = new ArrayList<>(inputRanges.length);
                for(String inputRange : inputRanges) {
                    String [] inputRangeValues = inputRange.trim().split(":");
                    ranges.add(new Range(
                            Double.parseDouble(inputRangeValues[0]),
                            Double.parseDouble(inputRangeValues[1])));
                }

                return ranges;
            }
        } catch (IllegalArgumentException iae) {
        }

        return null;
    }
}
