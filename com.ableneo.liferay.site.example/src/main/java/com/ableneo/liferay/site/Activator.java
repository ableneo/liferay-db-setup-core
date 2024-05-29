package com.ableneo.liferay.site;

import com.liferay.portal.kernel.upgrade.UpgradeException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = Activator.class)
public class Activator {
    @Activate
    public void activate() {
        try {
            DbSetupUtil.runDbSetupConfiguration("setup-ableneo-site.xml");
        } catch (UpgradeException e) {
            throw new RuntimeException(e);
        }
    }
}
