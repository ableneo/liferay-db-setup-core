package com.ableneo.liferay.portal.setup;

/*-
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.GroupLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import mockit.Expectations;
import mockit.Mocked;

class SetupConfigurationThreadLocalTest {

    // Liferay code is full of static methods. JMockit supports static method mocks.
    // Mockit will not support static method mocking (read the discussions), PowerMock doesn't support junit5 yet.
    @Mocked
    PortalUtil portalUtil;
    @Mocked
    GroupLocalServiceUtil groupLocalServiceUtil;
    @Mocked
    Group group;

    @Test
    void getRunInCompanyId() throws PortalException {
        Long groupId = 888l;
        final long companyId = 123l;
        new Expectations(PortalUtil.class) {
            {
                portalUtil.getDefaultCompanyId();
                result = companyId;
                groupLocalServiceUtil.getGroup(withAny(1l), withAny("")); // any of provided type
                result = group;
               group.getGroupId();
                result = groupId;
            }
        };
        assertEquals(companyId, PortalUtil.getDefaultCompanyId());
        assertEquals(groupId, SetupConfigurationThreadLocal.getRunInGroupId());
    }

    @Test
    void setRunInCompanyId() {
        SetupConfigurationThreadLocal.setRunAsUserId(1l);
        assertEquals(new Long(1l), SetupConfigurationThreadLocal.getRunAsUserId());
    }
}
