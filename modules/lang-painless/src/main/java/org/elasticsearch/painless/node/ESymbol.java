/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.symbol.SemanticScope;
import org.elasticsearch.painless.symbol.SemanticScope.Variable;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.ir.StaticNode;
import org.elasticsearch.painless.ir.VariableNode;
import org.elasticsearch.painless.lookup.PainlessLookupUtility;

import java.util.Objects;

/**
 * Represents a variable load/store.
 */
public class ESymbol extends AExpression {

    private final String symbol;

    public ESymbol(int identifer, Location location, String symbol) {
        super(identifer, location);

        this.symbol = Objects.requireNonNull(symbol);
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    Output analyze(ClassNode classNode, SemanticScope semanticScope, Input input) {
        Output output = new Output();
        Class<?> type = semanticScope.getScriptScope().getPainlessLookup().canonicalTypeNameToType(symbol);

        if (type != null)  {
            if (input.write) {
                throw createError(new IllegalArgumentException("invalid assignment: " +
                        "cannot write a value to a static type [" + PainlessLookupUtility.typeToCanonicalTypeName(type) + "]"));
            }

            if (input.read == false) {
                throw createError(new IllegalArgumentException("not a statement: " +
                        "static type [" + PainlessLookupUtility.typeToCanonicalTypeName(type) + "] not used"));
            }

            output.actual = type;
            output.isStaticType = true;

            StaticNode staticNode = new StaticNode();

            staticNode.setLocation(getLocation());
            staticNode.setExpressionType(output.actual);

            output.expressionNode = staticNode;
        } else if (semanticScope.isVariableDefined(symbol)) {
            if (input.read == false && input.write == false) {
                throw createError(new IllegalArgumentException("not a statement: variable [" + symbol + "] not used"));
            }

            Variable variable = semanticScope.getVariable(getLocation(), symbol);

            if (input.write && variable.isFinal()) {
                throw createError(new IllegalArgumentException("Variable [" + variable.getName() + "] is read-only."));
            }

            output.actual = variable.getType();

            VariableNode variableNode = new VariableNode();

            variableNode.setLocation(getLocation());
            variableNode.setExpressionType(output.actual);
            variableNode.setName(symbol);

            output.expressionNode = variableNode;
        } else {
            output.partialCanonicalTypeName = symbol;
        }

        return output;
    }
}
