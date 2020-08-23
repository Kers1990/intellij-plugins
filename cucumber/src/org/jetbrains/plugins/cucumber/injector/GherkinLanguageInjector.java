// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.cucumber.injector;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.intellij.plugins.intelliLang.inject.InjectorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.psi.GherkinPystring;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.plugins.cucumber.psi.GherkinLexer.PYSTRING_MARKER;

public class GherkinLanguageInjector implements MultiHostInjector {
    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(GherkinPystring.class);
    }

    @Override
    public void getLanguagesToInject(@NotNull final MultiHostRegistrar registrar, @NotNull final PsiElement context) {
        if (!(context instanceof GherkinPystring)) {
            return;
        }

        final PsiLanguageInjectionHost host = (PsiLanguageInjectionHost) context;

        String hostText = host.getText();
        int newLineCharacterOffset = 0;
        while (newLineCharacterOffset < hostText.length() && hostText.charAt(newLineCharacterOffset) != '\n') {
            newLineCharacterOffset++;
        }
        if (newLineCharacterOffset >= hostText.length()) {
            return;
        }
        String languageMarker = StringUtil.trimTrailing(hostText.substring(PYSTRING_MARKER.length(), newLineCharacterOffset));
        final Language language = InjectorUtils.getLanguageByString(languageMarker);

        if (language != null) {
            TextRange range = TextRange.create(newLineCharacterOffset, host.getTextLength() - PYSTRING_MARKER.length());

            if (!range.isEmpty()) {
                registrar.startInjecting(language);
                registrar.addPlace(null, null, host, range);
                registrar.doneInjecting();
            }
        }
    }
}