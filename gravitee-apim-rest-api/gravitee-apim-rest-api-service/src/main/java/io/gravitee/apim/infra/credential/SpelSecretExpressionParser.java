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
package io.gravitee.apim.infra.credential;

import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import io.gravitee.apim.core.credential.service_provider.SecretExpressionParser;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.IntLiteral;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.OpPlus;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

/**
 * Walks the parsed expression against an allow-list, rather than matching text, so a type reference, a constructor, a
 * bean or a method call cannot hide anywhere in it.
 */
@Service
public class SpelSecretExpressionParser implements SecretExpressionParser {

    private static final String PREFIX = "{";
    private static final String SUFFIX = "}";
    private static final int MAX_LENGTH = 2048;

    /** The request data an argument may read, by variable: each one only through a map, never through getters. */
    private static final Map<String, Set<String>> READABLE = Map.of(
        "#context",
        Set.of("attributes"),
        "#request",
        Set.of("headers", "params", "pathParams"),
        "#api",
        Set.of("properties")
    );

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public Optional<String> staticPath(String field, String expression) {
        if (expression == null || expression.length() > MAX_LENGTH || !expression.startsWith(PREFIX) || !expression.endsWith(SUFFIX)) {
            throw invalid(field);
        }
        SpelNode root;
        try {
            root = ((SpelExpression) parser.parseRaw(expression.substring(1, expression.length() - 1))).getAST();
        } catch (ParseException e) {
            throw invalid(field);
        }
        if (
            !(root instanceof CompoundExpression) ||
            root.getChildCount() != 2 ||
            !(root.getChild(0) instanceof VariableReference secrets && secrets.toStringAST().equals("#secrets")) ||
            !(root.getChild(1) instanceof MethodReference get && get.getName().equals("get")) ||
            get.getChildCount() < 1 ||
            get.getChildCount() > 2
        ) {
            throw invalid(field);
        }
        var arguments = IntStream.range(0, get.getChildCount()).mapToObj(get::getChild).toList();
        if (!arguments.stream().allMatch(SpelSecretExpressionParser::isAllowedArgument)) {
            throw invalid(field);
        }
        if (!arguments.stream().allMatch(StringLiteral.class::isInstance)) {
            return Optional.empty();
        }
        var path = literal(arguments.getFirst());
        return Optional.of(arguments.size() == 2 ? path + ":" + literal(arguments.get(1)) : path);
    }

    private static boolean isAllowedArgument(SpelNode node) {
        return switch (node) {
            case StringLiteral ignored -> true;
            case OpPlus plus -> plus.getChildCount() == 2 && isAllowedArgument(plus.getChild(0)) && isAllowedArgument(plus.getChild(1));
            case CompoundExpression compound -> isRequestData(compound);
            default -> false;
        };
    }

    /** {@code #variable.map[key]}, optionally indexed again, e.g. {@code #request.headers['X-Tenant'][0]}. */
    private static boolean isRequestData(CompoundExpression compound) {
        if (compound.getChildCount() < 3 || !(compound.getChild(0) instanceof VariableReference variable)) {
            return false;
        }
        var maps = READABLE.get(variable.toStringAST());
        if (maps == null || !(compound.getChild(1) instanceof PropertyOrFieldReference map) || !maps.contains(map.getName())) {
            return false;
        }
        return IntStream.range(2, compound.getChildCount()).mapToObj(compound::getChild).allMatch(SpelSecretExpressionParser::isKey);
    }

    private static boolean isKey(SpelNode node) {
        return (
            node instanceof Indexer indexer &&
            indexer.getChildCount() == 1 &&
            (indexer.getChild(0) instanceof StringLiteral || indexer.getChild(0) instanceof IntLiteral)
        );
    }

    private static String literal(SpelNode node) {
        return (String) ((StringLiteral) node).getLiteralValue().getValue();
    }

    private static InvalidCredentialException invalid(String field) {
        return new InvalidCredentialException(
            "The expression of " +
                field +
                " must be a single {#secrets.get(...)} whose arguments are text or request data such as #context.attributes['name']."
        );
    }
}
