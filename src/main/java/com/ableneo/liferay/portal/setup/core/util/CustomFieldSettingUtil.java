package com.ableneo.liferay.portal.setup.core.util;

import com.liferay.expando.kernel.model.ExpandoColumn;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;

/**
 * Utility for setting a custom field.
 *
 * @author msi
 */
public final class CustomFieldSettingUtil {

    private static final Log LOG = LogFactoryUtil.getLog(CustomFieldSettingUtil.class);

    private CustomFieldSettingUtil() {}

    /**
     * Auxiliary method that returns the expando value of a given expando field
     * with a given key.
     *
     * @param key The name of the expando field.
     */
    // CHECKSTYLE:OFF
    public static void setExpandoValue(
        final String resolverHint,
        final long groupId,
        final long company,
        final Class clazz,
        final long id,
        final String key,
        final String value
    ) {
        String valueCopy = value;
        try {
            ExpandoValue ev = ExpandoValueLocalServiceUtil.getValue(company, clazz.getName(), "CUSTOM_FIELDS", key, id);
            // resolve any values to be substituted
            valueCopy = ResolverUtil.lookupAll(groupId, company, valueCopy, resolverHint);
            if (ev == null) {
                long classNameId = ClassNameLocalServiceUtil.getClassNameId(clazz.getName());

                ExpandoTable expandoTable = ExpandoTableLocalServiceUtil.getTable(
                    company,
                    classNameId,
                    "CUSTOM_FIELDS"
                );
                ExpandoColumn expandoColumn = ExpandoColumnLocalServiceUtil.getColumn(
                    company,
                    classNameId,
                    expandoTable.getName(),
                    key
                );

                // In this we are adding MyUserColumnData for the column MyUserColumn. See the above line
                ExpandoValueLocalServiceUtil.addValue(
                    classNameId,
                    expandoTable.getTableId(),
                    expandoColumn.getColumnId(),
                    id,
                    valueCopy
                );
            } else {
                ev.setData(valueCopy);
                ExpandoValueLocalServiceUtil.updateExpandoValue(ev);
            }
        } catch (Exception ex) {
            LOG.error(
                "Expando (custom field) not found or problem accessing it: " +
                key +
                " for class " +
                clazz.getName() +
                " with id " +
                id,
                ex
            );
        }
    }
    // CHECKSTYLE:ON
}
