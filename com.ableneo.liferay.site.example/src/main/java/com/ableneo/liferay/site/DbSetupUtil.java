package com.ableneo.liferay.site;

import com.ableneo.liferay.portal.setup.LiferaySetup;
import com.ableneo.liferay.portal.setup.MarshallUtil;
import com.ableneo.liferay.portal.setup.domain.Setup;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import org.osgi.framework.FrameworkUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

public final class DbSetupUtil {

    private DbSetupUtil() {
    }

    public static void runDbSetupConfiguration(String dbSetupConfigurationFilePath) throws UpgradeException {
        boolean setupSuccess = false;

        try {
            InputStream setupFile = getInputStream(dbSetupConfigurationFilePath);
            final Setup setup = MarshallUtil.unmarshall(setupFile);
            setupSuccess = LiferaySetup.setup(setup, FrameworkUtil.getBundle(DbSetupUtil.class));
        } catch (Exception e) {
            throw new UpgradeException(
                String.format("Failed to load or process file: %1$s", dbSetupConfigurationFilePath),
                e
            );
        }
        if (!setupSuccess) {
            throw new UpgradeException("Failed to setup data successfully, please review error log, fix code or data and try again.");
        }
    }

    public static InputStream getInputStream(String dbSetupConfigurationFilePath) throws IOException {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(dbSetupConfigurationFilePath);
        return Objects.requireNonNull(resource).openStream();
    }
}
