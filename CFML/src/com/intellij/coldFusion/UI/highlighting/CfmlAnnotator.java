// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.coldFusion.UI.highlighting;

import com.intellij.coldFusion.model.lexer.CfscriptTokenTypes;
import com.intellij.coldFusion.model.psi.impl.CfmlAttributeImpl;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author vnikolaenko
 */
public class CfmlAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    final ASTNode elementNode = element.getNode();
    if (elementNode != null) {
      if (elementNode.getElementType() == CfscriptTokenTypes.IDENTIFIER &&
          element.getParent() instanceof CfmlAttributeImpl) {
        holder.createWeakWarningAnnotation(element, null).setTextAttributes(CfmlHighlighter.CfmlFileHighlighter.CFML_ATTRIBUTE);
      }
      if (elementNode.getElementType() == CfscriptTokenTypes.ACTION_NAME) {
        holder.createInfoAnnotation(elementNode, null).setTextAttributes(DefaultLanguageHighlighterColors.KEYWORD);
      }
    }
  }
}
