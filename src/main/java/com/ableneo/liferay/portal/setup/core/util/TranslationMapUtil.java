package com.ableneo.liferay.portal.setup.core.util;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Original work Copyright (C) 2016 - 2018 mimacom ag
 * Modified work Copyright (C) 2018 - 2020 ableneo, s. r. o.
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ableneo.liferay.portal.setup.domain.Translation;
import com.liferay.portal.kernel.util.LocaleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TranslationMapUtil {
    private static final Logger LOG = LoggerFactory.getLogger(TranslationMapUtil.class);

    private TranslationMapUtil() {}

    public static Map<Locale, String> getTranslationMap(final List<Translation> translations, final long groupId,
            final String defaultLocaleTitle, final String locationHint) {
        Map<Locale, String> translationMap = new HashMap<>();

        translationMap.put(LocaleUtil.getSiteDefault(), defaultLocaleTitle);
        if (translations != null) {
            for (Translation translation : translations) {
                try {
                    // convert posix locale format to language tag
                    Locale locale = Locale.forLanguageTag(translation.getLocale().replace('_', '-'));
                    translationMap.put(locale, translation.getText());
                } catch (RuntimeException ex) {
                    LOG.error("Exception while retrieving locale {} for {}", translation.getLocale(), locationHint, ex);
                }
            }
        }
        return translationMap;
    }

}
