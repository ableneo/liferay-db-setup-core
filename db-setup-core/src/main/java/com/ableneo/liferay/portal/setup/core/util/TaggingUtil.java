package com.ableneo.liferay.portal.setup.core.util;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2018 mimacom ag
 * Modified work Copyright (C) 2018 - 2020 ableneo Slovensko s.r.o.
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

import java.util.List;

import com.ableneo.liferay.portal.setup.LiferaySetup;
import com.ableneo.liferay.portal.setup.core.SetupContext;
import com.ableneo.liferay.portal.setup.domain.Article;
import com.ableneo.liferay.portal.setup.domain.Tag;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;
import com.liferay.portlet.journal.model.JournalArticle;

public final class TaggingUtil {
    private static final Log LOG = LogFactoryUtil.getLog(TaggingUtil.class);

    private TaggingUtil() {}

    public static void associateTags(final SetupContext setupContext, Article article, JournalArticle journalArticle)
            throws PortalException, SystemException {

        List<Tag> tags = article.getTag();
        String[] tagNames = null;
        if (tags != null) {
            tagNames = new String[tags.size()];
            for (int i = 0; i < tags.size(); i++) {
                tagNames[i] = tags.get(i).getName();
            }
        }
        AssetEntry entry = AssetEntryLocalServiceUtil.getEntry(JournalArticle.class.getName(),
                journalArticle.getResourcePrimKey());
        AssetEntryLocalServiceUtil.updateEntry(setupContext.getRunAsUserId(), setupContext.getRunInGroupId(), JournalArticle.class.getName(),
                entry.getClassPK(), null, tagNames);
    }
}
