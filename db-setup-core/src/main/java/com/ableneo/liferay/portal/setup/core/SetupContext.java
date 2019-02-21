package com.ableneo.liferay.portal.setup.core;

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

public class SetupContext implements Cloneable {
    private String adminRoleName;
    private String defaultGroupName;
    private long runAsUserId = -1l;
    private long runInCompanyId = -1l;
    private long runInGroupId = -1l;

    public SetupContext(String adminRoleName, String defaultGroupName, long runAsUserId, long runInCompanyId,
            long runInGroupId) {
        this.adminRoleName = adminRoleName;
        this.defaultGroupName = defaultGroupName;
        this.runAsUserId = runAsUserId;
        this.runInCompanyId = runInCompanyId;
        this.runInGroupId = runInGroupId;
    }

    public SetupContext(String adminRoleName, String defaultGroupName) {
        this.adminRoleName = adminRoleName;
        this.defaultGroupName = defaultGroupName;
    }

    @Override
    public SetupContext clone() {
        return new SetupContext(getAdminRoleName(), getDefaultGroupName(), getRunAsUserId(), getRunInCompanyId(),
                getRunInGroupId());
    }

    public String getAdminRoleName() {
        return adminRoleName;
    }

    public String getDefaultGroupName() {
        return defaultGroupName;
    }

    public long getRunAsUserId() {
        return runAsUserId;
    }

    public SetupContext setRunAsUserId(long runAsUserId) {
        this.runAsUserId = runAsUserId;
        return this;
    }

    public long getRunInCompanyId() {
        return runInCompanyId;
    }

    public long getRunInGroupId() {
        return runInGroupId;
    }

    public SetupContext setRunInGroupId(long runInGroupId) {
        this.runInGroupId = runInGroupId;
        return this;
    }

    public SetupContext setRunInCompanyId(long runInCompanyId) {
        this.runInCompanyId = runInCompanyId;
        return this;
    }

}
