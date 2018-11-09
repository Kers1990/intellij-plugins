/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.lang.javascript.uml;

import com.intellij.diagram.DiagramProvider;
import com.intellij.diagram.PsiDiagramNode;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;

public class FlashUmlClassNode extends PsiDiagramNode {
  public FlashUmlClassNode(final JSClass clazz, DiagramProvider provider) {
    super(clazz, provider);
  }

  @Override
  public String getTooltip() {
    return "<html><b>" + ((JSClass)getElement()).getQualifiedName() + "</b></html>";
  }
}
