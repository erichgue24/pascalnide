package com.duy.pascal.interperter.ast.runtime.operators.number;


import android.support.annotation.NonNull;

import com.duy.pascal.interperter.ast.expressioncontext.CompileTimeContext;
import com.duy.pascal.interperter.ast.expressioncontext.ExpressionContext;
import com.duy.pascal.interperter.ast.runtime.operators.BinaryOperatorEval;
import com.duy.pascal.interperter.ast.runtime.value.RuntimeValue;
import com.duy.pascal.interperter.ast.runtime.value.access.ConstantAccess;
import com.duy.pascal.interperter.declaration.lang.types.BasicType;
import com.duy.pascal.interperter.declaration.lang.types.OperatorTypes;
import com.duy.pascal.interperter.declaration.lang.types.RuntimeType;
import com.duy.pascal.interperter.linenumber.LineInfo;
import com.duy.pascal.interperter.exceptions.runtime.arith.PascalArithmeticException;
import com.duy.pascal.interperter.exceptions.runtime.internal.InternalInterpreterException;

public class CharBiOperatorEval extends BinaryOperatorEval {

    public CharBiOperatorEval(RuntimeValue operon1, RuntimeValue operon2, OperatorTypes operator, LineInfo line) {
        super(operon1, operon2, operator, line);
    }


    @NonNull
    @Override
    public RuntimeType getRuntimeType(ExpressionContext exprContext) throws Exception {
        switch (operator_type) {
            case EQUALS:
            case GREATEREQ:
            case GREATERTHAN:
            case LESSEQ:
            case LESSTHAN:
            case NOTEQUAL:
                return new RuntimeType(BasicType.Boolean, false);
            default:
                return new RuntimeType(BasicType.Character, false);
        }
    }

    @Override
    public Object operate(Object value1, Object value2)
            throws PascalArithmeticException, InternalInterpreterException {
        char v1 = (char) value1;
        char v2 = (char) value2;
        switch (operator_type) {
            case AND:
                return v1 & v2;
            case DIV:
                return v1 / v2;
            case EQUALS:
                return v1 == v2;
            case GREATEREQ:
                return v1 >= v2;
            case GREATERTHAN:
                return v1 > v2;
            case LESSEQ:
                return v1 <= v2;
            case LESSTHAN:
                return v1 < v2;
            case MINUS:
                return v1 - v2;
            case MOD:
                return v1 % v2;
            case MULTIPLY:
                return v1 * v2;
            case NOTEQUAL:
                return v1 != v2;
            case OR:
                return v1 | v2;
            case PLUS:
                return (char) (v1 + v2);
            case SHIFTLEFT:
                return v1 << v2;
            case SHIFTRIGHT:
                return v1 >> v2;
            case XOR:
                return v1 ^ v2;
            default:
                throw new InternalInterpreterException(line);
        }
    }

    @Override
    public RuntimeValue compileTimeExpressionFold(CompileTimeContext context) throws Exception {
        Object val = this.compileTimeValue(context);
        if (val != null) {
            return new ConstantAccess<>(val, line);

        } else {
            return new CharBiOperatorEval(
                    operon1.compileTimeExpressionFold(context),
                    operon2.compileTimeExpressionFold(context), operator_type, line);
        }
    }
}
