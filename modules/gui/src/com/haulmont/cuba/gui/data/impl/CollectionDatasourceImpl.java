/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 25.12.2008 15:06:46
 * $Id$
 */
package com.haulmont.cuba.gui.data.impl;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.cuba.core.Locator;
import com.haulmont.cuba.core.app.BasicService;
import com.haulmont.cuba.core.global.BasicInvocationContext;
import com.haulmont.cuba.core.global.BasicServiceRemote;
import com.haulmont.cuba.gui.data.*;
import com.haulmont.cuba.gui.xml.ParametersHelper;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.io.StringWriter;
import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.cache.StringTemplateLoader;

public class CollectionDatasourceImpl<T, K> extends DatasourceImpl<T> implements CollectionDatasource<T, K> {
    private String query;
    private ParametersHelper.ParameterInfo[] queryParameters;
    private DatasourceListener parentDsListener;

    private Collection<T> collection = Collections.emptyList();

    public CollectionDatasourceImpl(DsContext context, String id, MetaClass metaClass, String viewName) {
        super(context, id, metaClass, viewName);
        parentDsListener = new DatasourceListener() {
            public void itemChanged(Datasource ds, Object prevItem, Object item) {
                refresh();
            }

            public void stateChanged(Datasource ds, State prevState, State state) {}
            public void valueChanged(Object source, String property, Object prevValue, Object value) {}
        };
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.collection = Collections.emptyList();
    }

    @Override
    public void setItem(T item) {
        if (State.VALID.equals(state)) {
            Object prevItem = this.item;

            if (!ObjectUtils.equals(prevItem, item)) {
                if (this.item != null) {
                    detatchListener((Instance) this.item);
                }

                if (item instanceof Instance) {
                    final MetaClass aClass = ((Instance) item).getMetaClass();
                    if (!aClass.equals(metaClass)) {
                        throw new IllegalStateException(String.format("Invalid item metaClass"));
                    }
                    attachListener((Instance) item);
                }
                this.item = item;

                forceItemChanged(prevItem);
            }
        }
    }

    @Override
    public synchronized void refresh() {
        invalidate();
        loadData();

        State prevState = state;
        if (!prevState.equals(State.VALID)) {
            state = State.VALID;
            forceStateChanged(prevState);
        }

        forceCollectionChanged(new CollectionDatasourceListener.CollectionOperation<T>(CollectionDatasourceListener.CollectionOperation.Type.REFRESH, null));
    }

    public T getItem(K key) {
        if (State.NOT_INITIALIZAED.equals(state)) {
            throw new IllegalStateException("Invalid datasource state " + state);
        } else {
            return (T) key;
        }
    }

    public Collection<K> getItemIds() {
        if (State.NOT_INITIALIZAED.equals(state)) {
            return Collections.emptyList();
        } else {
            return (Collection<K>) getCollection();
        }
    }

    public int size() {
        if (State.NOT_INITIALIZAED.equals(state)) {
            return 0;
        } else {
            return getCollection().size();
        }
    }

    public void addItem(T item) throws UnsupportedOperationException {
        getCollection().add(item);
    }

    public void removeItem(T item) throws UnsupportedOperationException {
        getCollection().remove(item);
    }

    public boolean containsItem(K itemId) {
        return getCollection().contains(itemId);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        if (!ObjectUtils.equals(this.query, query)) {
            this.query = query;
            invalidate();

            queryParameters = ParametersHelper.parseQuery(query);
            for (ParametersHelper.ParameterInfo info : queryParameters) {
                final ParametersHelper.ParameterInfo.Type type = info.getType();
                if (ParametersHelper.ParameterInfo.Type.DATASOURCE.equals(type)) {
                    final String name = info.getName();

                    final String[] strings = name.split("\\.");
                    String source = strings[0];

                    final Datasource ds = dsContext.get(source);
                    if (ds != null) {
                        ds.addListener(parentDsListener);
                    } else {
                        // TODO create lazy task
                        throw new UnsupportedOperationException();
                    }
                }
            }

        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected Collection<T> getCollection() {
        if (query == null) {
            throw new IllegalStateException();
        } else {
            return collection;
        }
    }

    protected Collection<T> loadData() {
        BasicService service = Locator.lookupLocal(BasicService.JNDI_NAME);

        final BasicInvocationContext ctx = new BasicInvocationContext();
        ctx.setEntityClass(metaClass);

        final Map<String, Object> parameters = getQueryParameters();

        final BasicInvocationContext.Query query = ctx.setQueryString(getJPQLQuery(this.query, parameters));
        query.setParameters(parameters);

        ctx.setView(view);

        collection = (Collection) service.loadList(ctx);

        return collection;
    }

    private Map<String, Object> getQueryParameters() {
        final Map<String, Object> map = new HashMap<String, Object>();
        for (ParametersHelper.ParameterInfo info : queryParameters) {
            String name = info.getType().getPrefix() + "." + info.getName();
            name = name.replaceAll("\\.", "_");

            switch (info.getType()) {
                case DATASOURCE: {
                    final Datasource datasource = dsContext.get(info.getName());
                    if (Datasource.State.VALID.equals(datasource.getState())) {
                        map.put(name, datasource.getItem());
                    } else {
                        map.put(name, null);
                    }
                    
                    break;
                }
                case CONTEXT: {
                    final Object value =
                            dsContext.getContext() == null ?
                                    null : dsContext.getContext().getValue(info.getName());
                    map.put(name, value);
                    break;
                }
                case COMPONENT: {
                    final Object value =
                            dsContext.getContext() == null ?
                                    null : dsContext.getContext().getValue(info.getName());
                    map.put(name, value);
                    break;
                }
                default: {
                    throw new UnsupportedOperationException();
                }
            }
        }

        return map;
    }

    private String getJPQLQuery(String query, Map<String, Object> parameterValues) {
        for (ParametersHelper.ParameterInfo info : queryParameters) {
            final String paramInfo = "\\$\\{" + info.getType().getPrefix() + "\\:" + info.getName() + "\\}";

            String paramName = info.getType().getPrefix() + "." + info.getName();
            paramName = paramName.replaceAll("\\.", "_");

            query = query.replaceAll(paramInfo, paramName);
        }

        query = processTemplate(query, parameterValues);

        for (ParametersHelper.ParameterInfo info : queryParameters) {
            String paramName = info.getType().getPrefix() + "." + info.getName();
            paramName = paramName.replaceAll("\\.", "_");
            query = query.replaceAll(paramName, ":" + paramName);
        }

        return query;
    }

    protected String processTemplate(String query, Map<String, Object> parameterValues) {
        final StringWriter writer = new StringWriter();

        try {
            final Configuration configuration = new Configuration();

            final StringTemplateLoader templateLoader = new StringTemplateLoader();
            templateLoader.putTemplate("query", query);
            configuration.setTemplateLoader(templateLoader);

            final Template template = configuration.getTemplate("query");
            template.process(parameterValues, writer);

            return writer.toString();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected void forceCollectionChanged(CollectionDatasourceListener.CollectionOperation operation) {
        for (DatasourceListener dsListener : dsListeners) {
            if (dsListener instanceof CollectionDatasourceListener) {
                ((CollectionDatasourceListener) dsListener).collectionChanged(this, operation);
            }
        }
    }
}
