package com.ableneo.liferay.portal.setup;

/*
 * #%L
 * Liferay Portal DB Setup core
 * %%
 * Original work Copyright (C) 2016 - 2018 mimacom ag
 * Modified work Copyright (C) 2018 - 2020 ableneo, s. r. o.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.UpgradeException;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

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
                File configurationFile =
                        new File(BasicSetupUpgradeProcess.class.getClassLoader().getResource(fileName).toURI());
                LiferaySetup.setup(configurationFile);
            } catch (FileNotFoundException | URISyntaxException e) {
                throw new UpgradeException(
                        String.format("Failed to process liferay setup configuration (%1$s)", fileName), e);
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
