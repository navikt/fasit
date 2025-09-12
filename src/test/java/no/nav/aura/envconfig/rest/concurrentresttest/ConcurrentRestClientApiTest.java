package no.nav.aura.envconfig.rest.concurrentresttest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import no.nav.aura.envconfig.rest.RestTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConcurrentRestClientApiTest extends RestTest {

    private no.nav.aura.envconfig.rest.concurrentresttest.FasitRestClientWithSleep client;

    @BeforeEach
    public void setUpData() {
        client = new no.nav.aura.envconfig.rest.concurrentresttest.FasitRestClientWithSleep("http://localhost:" + jetty.getPort() + "/conf", "admin", "admin");
    }

    @Test
    public void calling_service_once_should_be_trivial() {
        try {
            String result = client.sleep(50);
            assertThat(result, is("I slept 0.05 seconds, yay!"));
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void calling_service_twice_should_be_trivial() {
        try {
            client.sleep(5);
            client.sleep(5);
        } catch (RuntimeException e) {
            fail();
        }
    }

    @Test
    public void calling_service_multithreaded_should_not_choke_it() throws InterruptedException {
        final Map<String, Exception> exceptionMap = new HashMap();
        List<Thread> threads = new ArrayList();
        for (int i = 10; i > 0; i--) {
            final int milliseconds = i;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        client.sleep(milliseconds);
                    } catch (RuntimeException e) {
                        exceptionMap.put(Thread.currentThread().getName(), e);
                    }
                }
            });
            t.setName("Thread number " + i);
            threads.add(t);
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertTrue(exceptionMap.isEmpty(), "Should have no exceptions, but got " + exceptionMap.size() + ": \n" + listExceptions(exceptionMap));
    }

    private String listExceptions(Map<String, Exception> exceptionMap) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, Exception> exceptionEntry : exceptionMap.entrySet()) {
            sb.append(exceptionEntry.getKey() + " got: " + exceptionEntry.getValue() + "\n---\n");
        }
        return sb.toString();
    }
}
