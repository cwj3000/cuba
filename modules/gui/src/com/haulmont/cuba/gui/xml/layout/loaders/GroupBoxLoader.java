/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 22.12.2008 17:57:24
 * $Id$
 */
package com.haulmont.cuba.gui.xml.layout.loaders;

import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.OrderedLayout;
import com.haulmont.cuba.gui.data.DsContext;
import org.dom4j.Element;

public class GroupBoxLoader  extends ContainerLoader implements com.haulmont.cuba.gui.xml.layout.ComponentLoader {
    public GroupBoxLoader(LayoutLoaderConfig config, ComponentsFactory factory, DsContext dsContext) {
        super(config, factory, dsContext);
    }

    public Component loadComponent(ComponentsFactory factory, Element element) throws InstantiationException, IllegalAccessException {
        final OrderedLayout component = factory.createComponent("group-box");

        final Element captionElement = element.element("caption");
        if (captionElement != null) {
            final String caption = captionElement.attributeValue("label");
            ((Component.HasCaption) component).setCaption(caption);
        }

        loadAlign(component, element);
        loadPack(component, element);

        loadSubcomponentsAndExpand(component, element, "caption");

        return component;
    }
}