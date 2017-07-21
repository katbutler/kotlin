/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.effectsystem.factories

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.effectsystem.structure.ConstantID
import org.jetbrains.kotlin.effectsystem.impls.ESBooleanConstant
import org.jetbrains.kotlin.effectsystem.impls.ESConstant
import org.jetbrains.kotlin.effectsystem.structure.NOT_NULL_ID

fun Boolean.lift(): ESBooleanConstant = ESBooleanConstant(ConstantID(this), this)

fun Nothing?.lift(): ESConstant = ESConstant(ConstantID(this), this, DefaultBuiltIns.Instance.nullableNothingType)

object ValuesFactory {
    val NOT_NULL_CONSTANT = ESConstant(
            id = NOT_NULL_ID,
            value = object {}, // anonymous singleton to be sure that this value will be
            type = DefaultBuiltIns.Instance.anyType)
}