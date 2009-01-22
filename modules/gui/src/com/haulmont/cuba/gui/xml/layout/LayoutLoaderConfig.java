/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 19.12.2008 15:29:12
 * $Id$
 */
package com.haulmont.cuba.gui.xml.layout;

import com.haulmont.cuba.gui.xml.layout.loaders.*;

import java.util.Map;
import java.util.HashMap;

public class LayoutLoaderConfig {
    private Map<String, Class<? extends com.haulmont.cuba.gui.xml.layout.ComponentLoader>> loaders =
            new HashMap<String, Class<? extends com.haulmont.cuba.gui.xml.layout.ComponentLoader>>();

    private static LayoutLoaderConfig windowLoaders = new LayoutLoaderConfig();
    private static LayoutLoaderConfig frameLoaders = new LayoutLoaderConfig();

    static {
        windowLoaders.registerLoader("window", WindowLoader.class);
        registerComponents(windowLoaders);

        frameLoaders.registerLoader("frame", FrameLoader.class);
        registerComponents(frameLoaders);
    }

    private static void registerComponents(LayoutLoaderConfig config) {
        config.registerLoader("hbox", HBoxLoader.class);
        config.registerLoader("vbox", VBoxLoader.class);
        config.registerLoader("button", ButtonLoader.class);
        config.registerLoader("group-box", GroupBoxLoader.class);
        config.registerLoader("label", LabelLoader.class);
        config.registerLoader("text-field", AbstractFieldLoader.class);
        config.registerLoader("text-area", AbstractFieldLoader.class);
        config.registerLoader("date-field", AbstractFieldLoader.class);
        config.registerLoader("lookup-field", AbstractFieldLoader.class);
        config.registerLoader("table", TableLoader.class);
        config.registerLoader("iframe", IFrameLoader.class);
        config.registerLoader("split", SplitPanelLoader.class);
    }

    public static LayoutLoaderConfig getWindowLoaders() {
        return windowLoaders;
    }

    public static LayoutLoaderConfig getFrameLoaders() {
        return frameLoaders;
    }

    public Class<? extends com.haulmont.cuba.gui.xml.layout.ComponentLoader> getLoader(String name) {
        return loaders.get(name);
    }

    public void registerLoader(String name, Class<? extends com.haulmont.cuba.gui.xml.layout.ComponentLoader> loader)
    {
        loaders.put(name, loader);
    }
}
