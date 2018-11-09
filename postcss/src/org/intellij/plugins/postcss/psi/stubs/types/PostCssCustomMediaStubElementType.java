package org.intellij.plugins.postcss.psi.stubs.types;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.css.impl.stubs.base.CssNamedStub;
import com.intellij.psi.css.impl.stubs.base.CssSimpleNamedStubElementType;
import com.intellij.psi.stubs.IndexSink;
import org.intellij.plugins.postcss.PostCssLanguage;
import org.intellij.plugins.postcss.psi.PostCssCustomMedia;
import org.intellij.plugins.postcss.psi.impl.PostCssCustomMediaImpl;
import org.intellij.plugins.postcss.psi.stubs.PostCssCustomMediaIndex;
import org.jetbrains.annotations.NotNull;

public class PostCssCustomMediaStubElementType extends CssSimpleNamedStubElementType<PostCssCustomMedia> {

  public PostCssCustomMediaStubElementType() {
    super("POST_CSS_CUSTOM_MEDIA", PostCssLanguage.INSTANCE);
  }

  @Override
  public PsiElement createElement(ASTNode node) {
    return new PostCssCustomMediaImpl(node);
  }

  @Override
  public PostCssCustomMedia createPsi(@NotNull CssNamedStub<PostCssCustomMedia> stub) {
    return new PostCssCustomMediaImpl(stub, this);
  }

  @Override
  public void indexStub(@NotNull final CssNamedStub<PostCssCustomMedia> stub, @NotNull final IndexSink sink) {
    sink.occurrence(PostCssCustomMediaIndex.KEY, stub.getName());
  }
}