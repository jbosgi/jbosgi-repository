package org.jboss.test.osgi.repository.tb2;

import org.jboss.test.osgi.repository.tb1.pkg1.TestInterface;

public class TestClass implements TestInterface {
    public void doit() {
        System.out.println("Doing it");
    }
}
