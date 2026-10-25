package com.slopgobbler.vaultfinder;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Running VaultFinder Pro JUnit Test Suite...");
        System.out.println("==================================================");

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectPackage("com.slopgobbler.vaultfinder"))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));

        if (summary.getTotalFailureCount() > 0) {
            System.err.println("\n[ERROR] Tests failed: " + summary.getTotalFailureCount() + " failures!");
            System.exit(1);
        } else {
            System.out.println("\n[SUCCESS] All " + summary.getTestsSucceededCount() + " tests passed!");
            System.exit(0);
        }
    }
}
