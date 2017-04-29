package com.duy.pascal.backend.exceptions;

import com.duy.pascal.backend.pascaltypes.DeclaredType;
import com.js.interpreter.ast.returnsvalue.ReturnsValue;

public class UnConvertibleTypeException extends com.duy.pascal.backend.exceptions.ParsingException {

    public final ReturnsValue obj;
    public final DeclaredType out;
    public final DeclaredType in;
    public final boolean implicit;

    public UnConvertibleTypeException(ReturnsValue obj,
                                      DeclaredType out, DeclaredType in, boolean implicit) {
        super(obj.getLine(),
                "The expression or variable \"" + obj + "\" is of type \"" + out + "\""
                        + ", which cannot be " + (implicit ? "implicitly " : "")
                        + "converted to to the type \"" + in + "\"");

        this.obj = obj;
        this.out = out;
        this.in = in;
        this.implicit = implicit;
    }
}
