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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.jetbrains.annotations.NotNull;

public class Storage<T> {

    private List<T> data = new ArrayList<>();

    public Storage() {}

    public List<T> unmodifiableData() {
        return Collections.unmodifiableList(data);
    }

    List<T> data() {
        return data;
    }

    void clear() {
        data.clear();
    }

    boolean addAll(Storage<? extends T> items) {
        return data.addAll(items.data);
    }

    public static <T> Storage<T> from(List<T> list) {
        final Storage<T> storage = new Storage<>();
        storage.data = list;
        return storage;
    }

    public static <T> Storage<T> of() {
        return Storage.from(List.of());
    }

    public static <T> Storage<T> of(T element) {
        return Storage.from(List.of(element));
    }

    public static <T> Storage<T> of(T e1, T e2) {
        return Storage.from(List.of(e1, e2));
    }

    public static <T> Storage<T> of(T e1, T e2, T e3) {
        return Storage.from(List.of(e1, e2, e3));
    }

    public static <T> Storage<T> of(T e1, T e2, T e3, T e4) {
        return Storage.from(List.of(e1, e2, e3, e4));
    }

    public static <T> Storage<T> of(T e1, T e2, T e3, T e4, T e5) {
        return Storage.from(List.of(e1, e2, e3, e4, e5));
    }

    public static <T> Storage<T> of(T e1, T e2, T e3, T e4, T e5, T e6) {
        return Storage.from(List.of(e1, e2, e3, e4, e5, e6));
    }

    public static <T> Storage<T> of(T e1, T e2, T e3, T e4, T e5, T e6, T e7) {
        return Storage.from(List.of(e1, e2, e3, e4, e5, e6, e7));
    }

    public static <T> Storage<T> of(T e1, T e2, T e3, T e4, T e5, T e6, T e7, T e8) {
        return Storage.from(List.of(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    public static <T> Storage<T> of(T e1, T e2, T e3, T e4, T e5, T e6, T e7, T e8, T e9) {
        return Storage.from(List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    public static <T> Storage<T> of(T e1, T e2, T e3, T e4, T e5, T e6, T e7, T e8, T e9, T e10) {
        return Storage.from(List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }
}
