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
package io.gravitee.apim.core.api.model.utils;

import static io.gravitee.apim.core.utils.CollectionUtils.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

public class MigrationResult<T> {

    private static final Comparator<State> STATE_COMPARATOR = Comparator.comparing(State::getWeight);
    private final Collection<Issue> issues = new ArrayList<>();
    private final T value;

    public MigrationResult(T value, Collection<Issue> issues) {
        this.value = value;
        this.issues.addAll(issues);
    }

    @Nullable
    protected T value() {
        return state() != State.IMPOSSIBLE ? value : null;
    }

    public Collection<Issue> issues() {
        return List.copyOf(issues);
    }

    public State state() {
        return value == null ? State.IMPOSSIBLE : stream(issues).map(Issue::state).max(STATE_COMPARATOR).orElse(State.MIGRATABLE);
    }

    public MigrationResult<T> addIssue(Issue issue) {
        issues.add(issue);
        return this;
    }

    public MigrationResult<T> addIssue(String message, State state) {
        issues.add(new Issue(message, state));
        return this;
    }

    public MigrationResult<T> addIssues(Collection<Issue> issue) {
        issues.addAll(issue);
        return this;
    }

    public void processValue(Consumer<T> consumer) {
        T currentValue = value();
        if (currentValue != null) {
            consumer.accept(currentValue);
        }
    }

    public <U> MigrationResult<U> map(Function<T, U> mapper) {
        U newValue = null;
        Stream<Issue> mappingIssue = Stream.empty();
        try {
            newValue = mapper.apply(value);
        } catch (RuntimeException e) {
            mappingIssue = Stream.of(new Issue(e.getMessage(), State.IMPOSSIBLE));
        }
        return new MigrationResult<>(newValue, Stream.concat(mappingIssue, stream(issues)).toList());
    }

    public <U> MigrationResult<U> flatMap(Function<T, MigrationResult<U>> mapper) {
        return mapper.apply(value).addIssues(issues);
    }

    public <U> MigrationResult<T> foldLeft(MigrationResult<U> result, Merger<T, U> merger) {
        return new MigrationResult<>(merger.merge(value(), result.value()), issues).addIssues(result.issues);
    }

    public static <T> MigrationResult<T> value(T value) {
        return new MigrationResult<>(value, List.of());
    }

    public static <T> MigrationResult<T> issues(Collection<Issue> issues) {
        return new MigrationResult<>(null, issues);
    }

    public static <T> MigrationResult<T> issue(String message, State state) {
        return new MigrationResult<>(null, List.of(new Issue(message, state)));
    }

    public record Issue(String message, State state) {}

    @RequiredArgsConstructor
    public enum State implements Comparable<State> {
        MIGRATABLE(0),
        MIGRATED(1),
        CAN_BE_FORCED(2),
        IMPOSSIBLE(3);

        @Getter
        private final int weight;
    }

    public interface Merger<T, U> extends BiFunction<T, U, T> {
        @Nullable
        T merge(@Nullable T result1, @Nullable U result2);

        @Override
        default T apply(T result1, U result2) {
            return merge(result1, result2);
        }
    }

    public static <T> MigrationResult<List<T>> mergeList(MigrationResult<List<T>> a, MigrationResult<List<T>> b) {
        return a.foldLeft(b, (c, d) -> Stream.concat(stream(c), stream(d)).toList());
    }
}
