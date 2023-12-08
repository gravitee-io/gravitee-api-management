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
package inmemory;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public interface InMemoryAlternative<T> {
    /**
     * Init the storage with the given items
     * @param storage the items to store
     */
    default InMemoryAlternative<T> initWith(Storage<T> storage) {
        reset();
        storage().addAll(storage);
        return this;
    }

    void syncStorageWith(InMemoryAlternative<T> other);

    /** Reset the storage */
    void reset();

    /** @return the storage */
    Storage<T> storage();

    default List<T> data() {
        return storage().unmodifiableData();
    }

    default OptionalInt findIndex(List<T> storage, Predicate<T> predicate) {
        IntPredicate intPredicate = i -> predicate.test(storage.get(i));
        return IntStream.range(0, storage.size()).filter(intPredicate).findFirst();
    }
}
