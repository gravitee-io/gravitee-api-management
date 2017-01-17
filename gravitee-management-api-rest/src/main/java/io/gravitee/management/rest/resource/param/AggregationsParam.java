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

import io.gravitee.management.model.analytics.query.AggregationType;

import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AggregationsParam extends AbstractParam<List<Aggregation>> {

    public AggregationsParam(String param) throws WebApplicationException {
        super(param);
    }

    @Override
    protected List<Aggregation> parse(String param) throws Throwable {
        try {
            if (param != null) {
                String [] inputAggs = param.split(";");
                List<Aggregation> aggregations = new ArrayList<>(inputAggs.length);
                for(String inputAgg : inputAggs) {
                    String [] inputRangeValues = inputAgg.trim().split(":");
                    aggregations.add(new Aggregation(
                            AggregationType.valueOf(inputRangeValues[0].toUpperCase()),
                            inputRangeValues[1]));
                }

                return aggregations;
            }
        } catch (IllegalArgumentException iae) {
        }

        return null;
    }
}
