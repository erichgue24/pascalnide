/*
 *  Copyright (c) 2017 Tran Le Duy
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

package com.duy.pascal.interperter.ast.instructions.assign_statement;

import android.support.annotation.NonNull;

import com.duy.pascal.interperter.ast.codeunit.RuntimeExecutableCodeUnit;
import com.duy.pascal.interperter.ast.expressioncontext.CompileTimeContext;
import com.duy.pascal.interperter.ast.expressioncontext.ExpressionContext;
import com.duy.pascal.interperter.ast.instructions.ExecutionResult;
import com.duy.pascal.interperter.ast.variablecontext.VariableContext;
import com.duy.pascal.interperter.ast.runtime_value.operators.BinaryOperatorEval;
import com.duy.pascal.interperter.ast.runtime_value.references.Reference;
import com.duy.pascal.interperter.ast.runtime_value.value.AssignableValue;
import com.duy.pascal.interperter.ast.runtime_value.value.RuntimeValue;
import com.duy.pascal.interperter.debugable.DebuggableNode;
import com.duy.pascal.interperter.linenumber.LineInfo;
import com.duy.pascal.interperter.exceptions.parsing.convert.UnConvertibleTypeException;
import com.duy.pascal.interperter.exceptions.runtime.RuntimePascalException;
import com.duy.pascal.interperter.declaration.lang.types.BasicType;
import com.duy.pascal.interperter.declaration.lang.types.OperatorTypes;
import com.duy.pascal.ui.debug.CallStack;

/**
 * a /= b
 * Divides a through b, and stores the result in a
 */
public class DivAssignStatement extends DebuggableNode implements AssignNode {
    private AssignableValue left;
    private RuntimeValue divOp;
    private LineInfo line;

    public DivAssignStatement(@NonNull AssignableValue left, @NonNull RuntimeValue divOp,
                              LineInfo line) throws Exception {
        this.left = left;
        this.divOp = divOp;
        this.line = line;
    }

    public DivAssignStatement(@NonNull ExpressionContext f,
                              @NonNull AssignableValue left, RuntimeValue value,
                              LineInfo line) throws Exception {
        this.left = left;
        this.line = line;
        this.divOp = BinaryOperatorEval.generateOp(f, left, value, OperatorTypes.DIVIDE, line);
        if (!(left.getRuntimeType(f).getRawType().equals(BasicType.Double) ||
                left.getRuntimeType(f).getRawType().equals(BasicType.Float))) {
            throw new UnConvertibleTypeException(left, BasicType.Double,
                    left.getRuntimeType(f).getRawType(), f);
        }
    }


    @Override
    public ExecutionResult executeImpl(VariableContext context,
                                       RuntimeExecutableCodeUnit<?> main) throws RuntimePascalException {

        Reference ref = left.getReference(context, main);
        Object v = this.divOp.getValue(context, main);
        ref.set(v);

        if (main.isDebug()) main.getDebugListener().onVariableChange(new CallStack(context));

        return ExecutionResult.NOPE;
    }

    @Override
    public String toString() {
        return left + " := " + divOp;
    }

    @Override
    public LineInfo getLineNumber() {
        return this.line;
    }

    @Override
    public AssignNode compileTimeConstantTransform(CompileTimeContext c)
            throws Exception {
        return new DivAssignStatement(left, divOp.compileTimeExpressionFold(c), line);
    }
}
