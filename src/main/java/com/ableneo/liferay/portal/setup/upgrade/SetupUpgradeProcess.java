package com.ableneo.liferay.portal.setup.upgrade;

/**
 * Basic class for upgrade process.
 */
public abstract class SetupUpgradeProcess extends BasicSetupUpgradeProcess {

    /**
     * @return paths to setup xml files.
     */
    protected final String[] getSetupFileNames() {
        String[] retVal = new String[1];
        retVal[0] = getSetupFileName();
        return retVal;
    }

    /**
     * @return path to setup xml file.
     */
    protected abstract String getSetupFileName();
}
