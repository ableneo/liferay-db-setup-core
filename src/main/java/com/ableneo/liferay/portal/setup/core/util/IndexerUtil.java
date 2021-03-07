package com.ableneo.liferay.portal.setup.core.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.search.SearchException;

public final class IndexerUtil {
    private static final Log LOG = LogFactoryUtil.getLog(IndexerUtil.class);

    private IndexerUtil() {}

    protected static void reindexAllIndices(final Class modelClass, final long id) {
        Indexer indexer = IndexerRegistryUtil.getIndexer(modelClass);

        try {
            indexer.reindex(modelClass.getName(), id);
        } catch (SearchException e) {
            LOG.error(e);
        }
    }
}
