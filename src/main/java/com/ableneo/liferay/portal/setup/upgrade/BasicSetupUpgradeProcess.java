package com.ableneo.liferay.portal.setup.upgrade;

import com.ableneo.liferay.portal.setup.LiferaySetup;
import com.ableneo.liferay.portal.setup.MarshallUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

/**
 * Created by mapa on 13.3.2015.
 */
public abstract class BasicSetupUpgradeProcess extends UpgradeProcess {
    /**
     * Logger.
     */
    private static final Log LOG = LogFactoryUtil.getLog(BasicSetupUpgradeProcess.class);

    /**
     * Does upgrade.
     *
     * @throws com.liferay.portal.kernel.upgrade.UpgradeException wrapped exception
     */
    @Override
    public final void upgrade() throws UpgradeException {
        String[] fileNames = getSetupFileNames();
        for (String fileName : fileNames) {
            try {
                File configurationFile = new File(
                    BasicSetupUpgradeProcess.class.getClassLoader().getResource(fileName).toURI()
                );
                LiferaySetup.setup(MarshallUtil.unmarshall(configurationFile));
            } catch (FileNotFoundException | URISyntaxException e) {
                throw new UpgradeException(
                    String.format("Failed to process liferay setup configuration (%1$s)", fileName),
                    e
                );
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Upgraded database with liferay setup configuration: %1$s", fileName));
            }
        }
    }

    @Override
    protected void doUpgrade() throws Exception {
        this.upgrade();
    }

    /**
     * @return paths to setup xml files.
     */
    protected abstract String[] getSetupFileNames();
}
