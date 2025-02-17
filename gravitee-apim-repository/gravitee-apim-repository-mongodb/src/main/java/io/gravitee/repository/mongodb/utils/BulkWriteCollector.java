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
package io.gravitee.repository.mongodb.utils;

import com.mongodb.client.model.WriteModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.bson.Document;

public class BulkWriteCollector implements Collector<WriteModel<Document>, List<WriteModel<Document>>, Boolean> {

    private final int batchSize;
    private final Function<List<WriteModel<Document>>, Boolean> bulkWriteFunction;
    private boolean success = true;

    public BulkWriteCollector(int batchSize, Function<List<WriteModel<Document>>, Boolean> bulkWriteFunction) {
        this.batchSize = batchSize;
        this.bulkWriteFunction = bulkWriteFunction;
    }

    @Override
    public Supplier<List<WriteModel<Document>>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<WriteModel<Document>>, WriteModel<Document>> accumulator() {
        return (list, item) -> {
            if (success) {
                list.add(item);
                if (list.size() >= batchSize) {
                    success = bulkWriteFunction.apply(new ArrayList<>(list));
                    list.clear();
                }
            }
        };
    }

    @Override
    public BinaryOperator<List<WriteModel<Document>>> combiner() {
        return (list1, list2) -> {
            if (success) {
                list1.addAll(list2);
                if (list1.size() >= batchSize) {
                    success = bulkWriteFunction.apply(new ArrayList<>(list1));
                    list1.clear();
                }
            }
            return list1;
        };
    }

    @Override
    public Function<List<WriteModel<Document>>, Boolean> finisher() {
        return list -> {
            if (success && !list.isEmpty()) {
                success = bulkWriteFunction.apply(list);
            }
            return success;
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
