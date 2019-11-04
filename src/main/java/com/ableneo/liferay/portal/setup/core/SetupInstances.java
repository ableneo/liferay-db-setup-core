package com.ableneo.liferay.portal.setup.core;

/*-
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Copyright (C) 2016 - 2019 ableneo s. r. o.
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

import com.ableneo.liferay.portal.setup.domain.Instance;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.util.PrefsPropsUtil;

import javax.portlet.PortletPreferences;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;
import java.io.IOException;
import java.util.List;

public class SetupInstances {
    private static final Log LOG = LogFactoryUtil.getLog(SetupInstances.class);


    public static void setupInstances(List<Instance> instances) {
        instances.forEach(i ->
        {
            try {
                addUpdateInstance(i);
            } catch (PortalException | ReadOnlyException | IOException | ValidatorException e) {
                LOG.error("Error processing instance", e);
            }
        });
    }

    private static void addUpdateInstance(Instance instance) throws PortalException, ReadOnlyException, IOException, ValidatorException {
        LOG.info("Processing instance " + instance.getCompanywebid());
        Company company = CompanyLocalServiceUtil.getCompanyByWebId(instance.getCompanywebid());
        if (company == null) {
            company = CompanyLocalServiceUtil.addCompany(instance.getCompanywebid(), instance.getVirtualHost(), instance.getMx(), false, company.getMaxUsers(), true);
        }
        if  (instance.getHomeUrl() != null) {
            company.setHomeURL(instance.getHomeUrl());
        }
        PortletPreferences preferences = PrefsPropsUtil.getPreferences(company.getCompanyId());
        updateSitePreferences(instance, company, preferences);
        company.persist();
        preferences.store();
    }

    private static void updateSitePreferences(Instance instance, Company company, PortletPreferences preferences) throws ReadOnlyException {
        preferences.setValue(PropsKeys.COMPANY_SECURITY_AUTO_LOGIN, instance.isAllowAutologin().toString());
        preferences.setValue(PropsKeys.COMPANY_SECURITY_SEND_PASSWORD, instance.isAllowRequestPassword().toString());
        preferences.setValue(PropsKeys.COMPANY_SECURITY_SEND_PASSWORD_RESET_LINK, instance.isAllowRequestPassword().toString());
        preferences.setValue(PropsKeys.COMPANY_SECURITY_STRANGERS, instance.isAllowStrangersCreateAccount().toString());
        preferences.setValue(PropsKeys.COMPANY_SECURITY_STRANGERS_WITH_MX, instance.isAllowStrangersCreateAccountCompanyEmail().toString());
        preferences.setValue(PropsKeys.COMPANY_SECURITY_STRANGERS_VERIFY, instance.isAllowStrangersCreateAccountCompanyEmail().toString());
        preferences.setValue(PropsKeys.USERS_SCREEN_NAME_ALWAYS_AUTOGENERATE, instance.isAutogenerateUserScreenNames().toString());
        preferences.setValue(PropsKeys.FIELD_ENABLE_COM_LIFERAY_PORTAL_MODEL_CONTACT_BIRTHDAY, instance.isEnableBirthday().toString());
        preferences.setValue(PropsKeys.FIELD_ENABLE_COM_LIFERAY_PORTAL_MODEL_CONTACT_MALE, instance.isEnableGender().toString());
        preferences.setValue(PropsKeys.TERMS_OF_USE_REQUIRED, instance.isTermsOfUseRequired().toString());
        preferences.setValue(PropsKeys.MEMBERSHIP_POLICY_ROLES, instance.getDefaultRoles());
        preferences.setValue(PropsKeys.ADMIN_EMAIL_FROM_NAME, instance.getEmailNotificationsName());
        preferences.setValue(PropsKeys.ADMIN_EMAIL_FROM_ADDRESS, instance.getEmailNotificationsAddress());
        preferences.setValue(PropsKeys.SITES_CONTENT_SHARING_THROUGH_ADMINISTRATORS_ENABLED, instance.isAllowSiteAdministratorsDisplayContentFromOtherSites().toString());
        preferences.setValue(PropsKeys.SITES_CONTENT_SHARING_WITH_CHILDREN_ENABLED, instance.getAllowSubsitesDisplayContentFromParentSites());
        preferences.setValue(PropsKeys.COMPANY_SECURITY_SITE_LOGO, instance.isAllowUseOwnLogo().toString());
        preferences.setValue(PropsKeys.COMPANY_SECURITY_AUTH_TYPE, instance.getAuthType());
        preferences.setValue(PropsKeys.DEFAULT_LANDING_PAGE_PATH, instance.getDefaultLandingPage());
        preferences.setValue(PropsKeys.DEFAULT_LOGOUT_PAGE_PATH, instance.getDefaultLogoutPage());
        preferences.setValue(PropsKeys.DEFAULT_REGULAR_THEME_ID, instance.getDefaultThemeId());
        preferences.setValue(PropsKeys.CONTROL_PANEL_LAYOUT_REGULAR_THEME_ID, instance.getDefaultControlPanelThemeId());
    }
}
