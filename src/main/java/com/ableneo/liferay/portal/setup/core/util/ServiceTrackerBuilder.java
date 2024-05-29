package com.ableneo.liferay.portal.setup.core.util;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public class ServiceTrackerBuilder<T> {

    private final Class hostClass;
    private final Class<T> clazz;

    public ServiceTrackerBuilder(Class<T> serviceClass, Class hostClass) {
        this.clazz = serviceClass;
        this.hostClass = hostClass;
    }

    public ServiceTracker<T, T> build() {
        Bundle bundle = FrameworkUtil.getBundle(hostClass);

        if (bundle != null) {
            ServiceTracker<T, T> serviceTracker =
                new ServiceTracker<>(bundle.getBundleContext(),
                    clazz, null);

            serviceTracker.open();

            return serviceTracker;
        } else {
            throw new IllegalStateException(String.format("Host bundle not found for class %s. It is not possible to reliably track service %s instances without host bundle. It's best to use bundle containing class that needs the tracker to track the service.", hostClass.getName(), clazz.getName()));
        }
    }

    public ServiceTracker<T, T> build(String filter) {
        Bundle bundle = FrameworkUtil.getBundle(hostClass);
        String filterWithClass = String.format("(&(objectClass=%s)%s)", clazz.getName(), filter);
        Filter serviceFilter = null;
        try {
            serviceFilter = bundle.getBundleContext().createFilter(filterWithClass);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(String.format("Invalid filter syntax: %s", filterWithClass), e);
        }


        if (bundle != null) {
            ServiceTracker<T, T> serviceTracker =
                new ServiceTracker<>(bundle.getBundleContext(), serviceFilter,
                     null);

            serviceTracker.open();

            return serviceTracker;
        } else {
            throw new IllegalStateException(String.format("Host bundle not found for class %s. It is not possible to reliably track service %s instances without host bundle. It's best to use bundle containing class that needs the tracker to track the service.", hostClass.getName(), clazz.getName()));
        }
    }

}
