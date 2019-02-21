/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portlet.asset.util;

/*-
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2019 ableneo Slovensko s.r.o.
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

import java.util.LinkedHashSet;
import java.util.Set;

import com.liferay.asset.kernel.model.AssetCategoryConstants;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PrefixPredicateFilter;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;

/**
 * @author José Manuel Navarro
 * @author Pawel Kruszewski - modified to use with Liferay 6.2
 */
public class AssetVocabularySettingsHelper {

	public static final long[] DEFAULT_SELECTED_CLASSNAME_IDS =
		{AssetCategoryConstants.ALL_CLASS_NAME_ID};

	public static final long[] DEFAULT_SELECTED_CLASSTYPE_PKS =
		{AssetCategoryConstants.ALL_CLASS_TYPE_PK};

	public AssetVocabularySettingsHelper() {
		_properties = new UnicodeProperties(true);
	}

	public AssetVocabularySettingsHelper(String propertiesString) {
		this();

		_properties.fastLoad(propertiesString);
	}

	public long[] getClassNameIds() {
		String[] classNameIdsAndClassTypePKs = getClassNameIdsAndClassTypePKs();

		return getClassNameIds(classNameIdsAndClassTypePKs);
	}

	public long[] getClassTypePKs() {
		String[] classNameIdsAndClassTypePKs = getClassNameIdsAndClassTypePKs();

		return getClassTypePKs(classNameIdsAndClassTypePKs);
	}

	public long[] getRequiredClassNameIds() {
		String[] classNameIdsAndClassTypePKs =
			getRequiredClassNameIdsAndClassTypePKs();

		return getClassNameIds(classNameIdsAndClassTypePKs);
	}

	public long[] getRequiredClassTypePKs() {
		String[] classNameIdsAndClassTypePKs =
			getRequiredClassNameIdsAndClassTypePKs();

		return getClassTypePKs(classNameIdsAndClassTypePKs);
	}

	public boolean hasClassNameIdAndClassTypePK(
		long classNameId, long classTypePK) {

		return isClassNameIdAndClassTypePKSpecified(
			classNameId, classTypePK, getClassNameIdsAndClassTypePKs());
	}

	public boolean isClassNameIdAndClassTypePKRequired(
		long classNameId, long classTypePK) {

		return isClassNameIdAndClassTypePKSpecified(
			classNameId, classTypePK, getRequiredClassNameIdsAndClassTypePKs());
	}

	public boolean isMultiValued() {
		String value = _properties.getProperty(_KEY_MULTI_VALUED);

		return GetterUtil.getBoolean(value, true);
	}

	public void setClassNameIdsAndClassTypePKs(
		long[] classNameIds, long[] classTypePKs, boolean[] requireds) {

		Set<String> requiredClassNameIds = new LinkedHashSet<>();
		Set<String> selectedClassNameIds = new LinkedHashSet<>();

		for (int i = 0; i < classNameIds.length; ++i) {
			long classNameId = classNameIds[i];
			long classTypePK = classTypePKs[i];
			boolean required = requireds[i];

			String classNameIdAndClassTypePK = getClassNameIdAndClassTypePK(
				classNameId, classTypePK);

			if (classNameIdAndClassTypePK.equals(
					AssetCategoryConstants.
						ALL_CLASS_NAME_IDS_AND_CLASS_TYPE_PKS)) {

				if (required) {
					requiredClassNameIds.clear();

					requiredClassNameIds.add(classNameIdAndClassTypePK);
				}

				selectedClassNameIds.clear();

				selectedClassNameIds.add(classNameIdAndClassTypePK);

				break;
			}
			else {
				if (required) {
					requiredClassNameIds.add(classNameIdAndClassTypePK);
				}

				selectedClassNameIds.add(classNameIdAndClassTypePK);
			}
		}

		_properties.setProperty(
			_KEY_REQUIRED_CLASS_NAME_IDS_AND_CLASS_TYPE_PKS,
			StringUtil.merge(requiredClassNameIds));
		_properties.setProperty(
			_KEY_SELECTED_CLASS_NAME_IDS_AND_CLASS_TYPE_PKS,
			StringUtil.merge(selectedClassNameIds));
	}

	public void setMultiValued(boolean multiValued) {
		_properties.setProperty(_KEY_MULTI_VALUED, String.valueOf(multiValued));
	}

	@Override
	public String toString() {
		return _properties.toString();
	}

	protected long getClassNameId(String classNameIdAndClassTypePK) {
		String[] parts = StringUtil.split(
			classNameIdAndClassTypePK, CharPool.COLON);

		return GetterUtil.getLong(parts[0]);
	}

	protected String getClassNameIdAndClassTypePK(
		long classNameId, long classTypePK) {

		return String.valueOf(classNameId).concat(StringPool.COLON).concat(
			String.valueOf(classTypePK));
	}

	protected long[] getClassNameIds(String[] classNameIdsAndClassTypePKs) {
		long[] classNameIds = new long[classNameIdsAndClassTypePKs.length];

		for (int i = 0; i < classNameIdsAndClassTypePKs.length; i++) {
			long classNameId = getClassNameId(classNameIdsAndClassTypePKs[i]);

			classNameIds[i] = classNameId;
		}

		return classNameIds;
	}

	protected String[] getClassNameIdsAndClassTypePKs() {
		String value = _properties.getProperty(
			_KEY_SELECTED_CLASS_NAME_IDS_AND_CLASS_TYPE_PKS);

		if (Validator.isNull(value)) {
			return new String[] {
				getClassNameIdAndClassTypePK(
					AssetCategoryConstants.ALL_CLASS_NAME_ID,
					AssetCategoryConstants.ALL_CLASS_TYPE_PK)
			};
		}

		return StringUtil.split(value);
	}

	protected long getClassTypePK(String classNameIdAndClassTypePK) {
		String[] parts = StringUtil.split(
			classNameIdAndClassTypePK, CharPool.COLON);

		if (parts.length == 1) {
			return AssetCategoryConstants.ALL_CLASS_TYPE_PK;
		}
		else {
			return GetterUtil.getLong(parts[1]);
		}
	}

	protected long[] getClassTypePKs(String[] classNameIdsAndClassTypePKs) {
		long[] classTypePKs = new long[classNameIdsAndClassTypePKs.length];

		for (int i = 0; i < classNameIdsAndClassTypePKs.length; i++) {
			long classTypePK = getClassTypePK(classNameIdsAndClassTypePKs[i]);

			classTypePKs[i] = classTypePK;
		}

		return classTypePKs;
	}

	protected String[] getRequiredClassNameIdsAndClassTypePKs() {
		String value = _properties.getProperty(
			_KEY_REQUIRED_CLASS_NAME_IDS_AND_CLASS_TYPE_PKS);

		if (Validator.isNull(value)) {
			return new String[0];
		}

		return StringUtil.split(value);
	}

	protected boolean isClassNameIdAndClassTypePKSpecified(
		long classNameId, long classTypePK,
		String[] classNameIdsAndClassTypePKs) {

		if (classNameIdsAndClassTypePKs.length == 0) {
			return false;
		}

		if (classNameIdsAndClassTypePKs[0].equals(
				AssetCategoryConstants.ALL_CLASS_NAME_IDS_AND_CLASS_TYPE_PKS)) {

			return true;
		}

		if (classTypePK == AssetCategoryConstants.ALL_CLASS_TYPE_PK) {
		    Boolean exists = false;
            for (String classNameIdsAndClassTypePK : classNameIdsAndClassTypePKs) {
                if (classNameIdsAndClassTypePK.startsWith(classNameId + StringPool.COLON)) {
                    exists = true;
                    break;
                }
            }
			return exists;
		}
		else {
			String classNameIdAndClassTypePK = getClassNameIdAndClassTypePK(
				classNameId, classTypePK);

			if (ArrayUtil.contains(
					classNameIdsAndClassTypePKs, classNameIdAndClassTypePK)) {

				return true;
			}

			String classNameIdAndAllClassTypePK = getClassNameIdAndClassTypePK(
				classNameId, AssetCategoryConstants.ALL_CLASS_TYPE_PK);

			return ArrayUtil.contains(
				classNameIdsAndClassTypePKs, classNameIdAndAllClassTypePK);
		}
	}

	private static final String _KEY_MULTI_VALUED = "multiValued";

	private static final String
		_KEY_REQUIRED_CLASS_NAME_IDS_AND_CLASS_TYPE_PKS =
			"requiredClassNameIds";

	private static final String
		_KEY_SELECTED_CLASS_NAME_IDS_AND_CLASS_TYPE_PKS =
			"selectedClassNameIds";

	private final UnicodeProperties _properties;

}
