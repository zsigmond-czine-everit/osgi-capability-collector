/**
 * This file is part of Everit - OSGi Service Collector Tests.
 *
 * Everit - OSGi Service Collector Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Service Collector Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Service Collector Tests.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.servicecollector.tests;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.servicecollector.DuplicateReferenceIdException;
import org.everit.osgi.servicecollector.ReferenceItem;
import org.everit.osgi.servicecollector.ReferenceTracker;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

@Component
@Properties({
        @Property(name = "eosgi.testId", value = "serviceCollectorTest"),
        @Property(name = "eosgi.testEngine", value = "junit4")
})
@Service(value = ReferenceTrackerTestComponent.class)
@TestDuringDevelopment
public class ReferenceTrackerTestComponent {

    private BundleContext context;

    @SuppressWarnings("unchecked")
    private static ReferenceItem<Object>[] EMPTY_ITEMS = new ReferenceItem[0];

    @Activate
    public void activate(BundleContext context) {
        this.context = context;

    }

    @Test(expected = NullPointerException.class)
    public void testNullContext() {
        new ReferenceTracker<Object>(null, Object.class, EMPTY_ITEMS, false, new DefaultActionHandler<Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullActionHandler() {
        new ReferenceTracker<Object>(context, Object.class, EMPTY_ITEMS, false, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullReferenceType() {
        new ReferenceTracker<Object>(context, null, EMPTY_ITEMS, false, new DefaultActionHandler<Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullItems() {
        new ReferenceTracker<Object>(context, Object.class, null, false, new DefaultActionHandler<Object>());
    }

    /**
     * In case of zero items in the description, the "satisfied" action should be called as soon as the tracker is
     * opened and the "unsatisfied" action should be called as soon as the tracker is closed.
     */
    @Test
    public void testZeroItems() {
        DefaultActionHandler<Object> actionHandler = new DefaultActionHandler<Object>();

        ReferenceTracker<Object> tracker = new ReferenceTracker<Object>(context, Object.class, EMPTY_ITEMS, false,
                actionHandler);

        tracker.open(false);

        Assert.assertTrue(actionHandler.isSatisfied());

        tracker.close();

        Assert.assertFalse(actionHandler.isSatisfied());
    }

    @Test(expected = DuplicateReferenceIdException.class)
    public void testDuplicateReferenceItemId() {
        @SuppressWarnings("unchecked")
        ReferenceItem<Object>[] items = new ReferenceItem[] { new ReferenceItem<Object>("test", null),
                new ReferenceItem<Object>("test", null) };

        new ReferenceTracker<Object>(context, Object.class, items, false, new DefaultActionHandler<Object>());
    }

    @Test(expected = NullPointerException.class)
    public void testNullItem() {
        @SuppressWarnings("unchecked")
        ReferenceItem<Object>[] items = new ReferenceItem[] { null };

        new ReferenceTracker<Object>(context, Object.class, items, false, new DefaultActionHandler<Object>());
    }

    @Test
    public void testNullFilter() {

        @SuppressWarnings("unchecked")
        ReferenceItem<DummyClass>[] items = new ReferenceItem[] { new ReferenceItem<Object>("test", null) };

        DefaultActionHandler<DummyClass> actionHandler = new DefaultActionHandler<DummyClass>();

        ReferenceTracker<DummyClass> referenceTracker = new ReferenceTracker<DummyClass>(context, DummyClass.class,
                items, false, actionHandler);

        referenceTracker.open(false);
        Assert.assertFalse(actionHandler.isSatisfied());

        ServiceRegistration<DummyClass> serviceRegistration = context.registerService(DummyClass.class,
                new DummyClass(), null);

        Assert.assertTrue(actionHandler.isSatisfied());
        serviceRegistration.unregister();
        Assert.assertFalse(actionHandler.isSatisfied());

        referenceTracker.close();

    }

    private Filter createFilter(String filterString) {
        try {
            return context.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Dictionary<String, Object> createServiceProps(String... strings) {
        Dictionary<String, Object> result = new Hashtable<String, Object>();
        for (int i = 0, n = strings.length; i < n; i = i + 2) {
            result.put(strings[i], strings[i + 1]);
        }
        return result;
    }
}
