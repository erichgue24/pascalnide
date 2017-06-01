/*
 *  Copyright 2017 Tran Le Duy
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

package com.duy.pascal.backend.builtin_libraries;


import com.duy.pascal.backend.ast.expressioncontext.ExpressionContextMixin;

import java.util.Map;

public interface IPascalLibrary {

    boolean instantiate(Map<String, Object> pluginargs);

    /**
     * Invoked when the receiver is shut down.
     */
    void shutdown();

    void declareConstants(ExpressionContextMixin parentContext);

    void declareTypes(ExpressionContextMixin parentContext);

    void declareVariables(ExpressionContextMixin parentContext);

    void declareFunctions(ExpressionContextMixin parentContext);

}