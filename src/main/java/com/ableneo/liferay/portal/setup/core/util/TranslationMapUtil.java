package com.ableneo.liferay.portal.setup.core.util;

import com.ableneo.liferay.portal.setup.domain.TranslationType;
import com.liferay.portal.kernel.util.LocaleUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TranslationMapUtil {
    private static final Logger LOG = LoggerFactory.getLogger(TranslationMapUtil.class);

    private TranslationMapUtil() {}

    public static Map<Locale, String> getTranslationMap(
        final List<TranslationType> translations,
        final long groupId,
        final String defaultLocaleTitle,
        final String locationHint
    ) {
        Map<Locale, String> translationMap = new HashMap<>();

        translationMap.put(LocaleUtil.getSiteDefault(), defaultLocaleTitle);
        if (translations != null) {
            for (TranslationType translation : translations) {
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
