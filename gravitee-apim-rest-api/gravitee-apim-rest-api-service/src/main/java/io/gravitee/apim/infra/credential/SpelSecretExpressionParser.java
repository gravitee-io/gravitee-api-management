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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.OpPlus;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

@Service
public class SpelSecretExpressionParser implements SecretExpressionParser {

    private static final int MAX_LENGTH = 2048;
    private static final String PLACEHOLDER_KEY = "key";

    private static final Map<String, String> READABLE = Map.of("#context", "attributes", "#api", "properties");

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public Optional<String> literalPath(String field, String expression) {
        var arguments = argumentsOfSecretsGet(field, expression);
        if (!arguments.stream().allMatch(SpelSecretExpressionParser::isAllowedArgument)) {
            throw invalid(field);
        }
        var path = text(arguments.getFirst());
        var key = arguments.size() == 2 ? Optional.of(arguments.get(1)) : Optional.<SpelNode>empty();
        if (path.isEmpty()) {
            if (key.isPresent() && text(key.get()).isEmpty()) {
                throw new InvalidCredentialException(
                    "The expression of " + field + " reads its path from context data, so its key must be text."
                );
            }
            return Optional.empty();
        }
        if (key.isEmpty()) {
            if (path.get().lastIndexOf(':') <= path.get().lastIndexOf('/')) {
                throw new InvalidCredentialException(
                    "The expression of " + field + " must name a key, as /vault/secret/name:key or as a second argument."
                );
            }
            return path;
        }
        return Optional.of(path.get() + ":" + text(key.get()).orElse(PLACEHOLDER_KEY));
    }

    // Text joined with + folds into one string, so the plain-path checks see the path the gateway resolves.
    private static Optional<String> text(SpelNode node) {
        return switch (node) {
            case StringLiteral literal -> Optional.of((String) literal.getLiteralValue().getValue());
            case OpPlus plus -> text(plus.getChild(0)).flatMap(left -> text(plus.getChild(1)).map(right -> left + right));
            default -> Optional.empty();
        };
    }

    private List<SpelNode> argumentsOfSecretsGet(String field, String expression) {
        if (expression == null || expression.length() > MAX_LENGTH || !expression.startsWith("{") || !expression.endsWith("}")) {
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
        return IntStream.range(0, get.getChildCount()).mapToObj(get::getChild).toList();
    }

    private static boolean isAllowedArgument(SpelNode node) {
        return switch (node) {
            case StringLiteral ignored -> true;
            case OpPlus plus -> plus.getChildCount() == 2 && isAllowedArgument(plus.getChild(0)) && isAllowedArgument(plus.getChild(1));
            case CompoundExpression compound -> isContextData(compound);
            default -> false;
        };
    }

    private static boolean isContextData(CompoundExpression compound) {
        return (
            compound.getChildCount() == 3 &&
            compound.getChild(0) instanceof VariableReference variable &&
            compound.getChild(1) instanceof PropertyOrFieldReference map &&
            map.getName().equals(READABLE.get(variable.toStringAST())) &&
            compound.getChild(2) instanceof Indexer indexer &&
            indexer.getChildCount() == 1 &&
            indexer.getChild(0) instanceof StringLiteral
        );
    }

    private static InvalidCredentialException invalid(String field) {
        return new InvalidCredentialException(
            "The expression of " +
                field +
                " must be a single {#secrets.get(...)} whose arguments are text, #context.attributes['name'] or #api.properties['name']."
        );
    }
}
