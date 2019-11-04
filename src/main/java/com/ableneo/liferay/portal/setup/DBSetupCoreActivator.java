package com.ableneo.liferay.portal.setup;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(
    immediate = true,
    service = DBSetupCoreActivator.class
)
public class DBSetupCoreActivator {

    @Activate
    void activate() throws Exception {
        System.out.println("Activating DBSetupCore module");
    }

}
