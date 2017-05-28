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

package com.duy.pascal.backend.pascaltypes.rangetype;

import android.support.annotation.Nullable;

import com.duy.pascal.backend.linenumber.LineInfo;
import com.duy.pascal.backend.runtime.VariableContext;
import com.duy.pascal.backend.runtime.exception.RuntimePascalException;
import com.js.interpreter.codeunit.RuntimeExecutableCodeUnit;

/**
 * Created by Duy on 25-May-17.
 */
public interface Containable {
    boolean contain(@Nullable VariableContext f, @Nullable RuntimeExecutableCodeUnit<?> main,
                    Object value) throws RuntimePascalException;

    /**
     * the line of code
     */
    LineInfo getLineNumber();
}
