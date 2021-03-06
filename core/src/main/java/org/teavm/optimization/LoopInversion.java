/*
 *  Copyright 2016 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.optimization;

import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;

public class LoopInversion implements MethodOptimization {
    @Override
    public void optimize(MethodReader method, Program program) {
        new LoopInversionImpl(program, method.parameterCount() + 1).apply();
    }
}
