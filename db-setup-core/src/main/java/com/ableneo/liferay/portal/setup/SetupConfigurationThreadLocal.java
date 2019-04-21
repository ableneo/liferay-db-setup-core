package com.ableneo.liferay.portal.setup;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2019 Pawel Kruszewski
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

import com.liferay.petra.lang.CentralizedThreadLocal;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;

public class SetupConfigurationThreadLocal {
    private static final Log LOG = LogFactoryUtil.getLog(SetupConfigurationThreadLocal.class);
    private static final ThreadLocal<Long> _runAsUserId =
            new CentralizedThreadLocal<>(SetupConfigurationThreadLocal.class + "._runAsUserId", () -> {
                try {
                    return UserLocalServiceUtil.getDefaultUserId(PortalUtil.getDefaultCompanyId());
                } catch (PortalException e) {
                    LOG.error("Failed to get default user id", e);
                }
                return null;
            });
    private static final ThreadLocal<Long> _runInCompanyId = new CentralizedThreadLocal<>(
            SetupConfigurationThreadLocal.class + "._runInCompanyId", PortalUtil::getDefaultCompanyId);
    private static final ThreadLocal<Long> _runInGroupId =
            new CentralizedThreadLocal<>(SetupConfigurationThreadLocal.class + "._runInGroupId", () -> {
                try {
                    return GroupLocalServiceUtil.getGroup(PortalUtil.getDefaultCompanyId(), GroupConstants.GUEST)
                            .getGroupId();
                } catch (PortalException e) {
                    LOG.error("Failed to get Guest group id for default company", e);
                }
                return null;
            });
    private static final ThreadLocal<String> _defaultGroupName = new CentralizedThreadLocal<>(
            SetupConfigurationThreadLocal.class + "._defaultGroupName", () -> GroupConstants.GUEST);

    private SetupConfigurationThreadLocal() {}

    public static Long getRunAsUserId() {
        return _runAsUserId.get();
    }

    public static void setRunAsUserId(Long runAsUserId) {
        _runAsUserId.set(runAsUserId);
    }

    public static Long getRunInCompanyId() {
        return _runInCompanyId.get();
    }

    public static void setRunInCompanyId(Long runInCompanyId) {
        _runInCompanyId.set(runInCompanyId);
    }

    public static Long getRunInGroupId() {
        return _runInGroupId.get();
    }

    public static void setRunInGroupId(Long runInGroupId) {
        _runInGroupId.set(runInGroupId);
    }

    public static void setDefaultGroupName(String defaultGroupName) {
        _defaultGroupName.set(defaultGroupName);
    }

}
