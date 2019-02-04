package rocks.inspectit.oce.instrumentation.special;

import io.opencensus.common.Scope;
import io.opencensus.tags.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ThreadStartContextPropagationTest {

    private static final Tagger tagger = Tags.getTagger();

    /**
     * Abstract thread class.
     */
    private abstract class AbstractThread extends Thread {

        protected Runnable run;

        public AbstractThread(Runnable runnable) {
            run = runnable;
        }

        @Override
        public synchronized void start() {
            super.start();
        }

        @Override
        public void run() {
            run.run();
        }
    }

    private class SubThread extends AbstractThread {
        public SubThread(Runnable runnable) {
            super(runnable);
            setName("dummy-thread");
        }
    }

    @BeforeAll
    public static void before() throws InterruptedException {
        // Waiting for instrumentation
        Thread.sleep(5000);
    }

    @Test
    public void verifyContextProgapationViaAbstractThreads() throws InterruptedException {
        long rand = System.nanoTime();
        TagKey tagKey = TagKey.create("test-tag-key-" + rand);
        TagValue tagValue = TagValue.create("test-tag-value-" + rand);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Thread thread = new SubThread(() -> {
            Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
            refTags.set(iter);
            latch.countDown();
        });

        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            thread.start();
        }

        latch.await(5, TimeUnit.SECONDS);

        assertThat(refTags.get()).hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue));
    }

    @Test
    public void verifyContextProgapation() throws InterruptedException {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
            refTags.set(iter);
            latch.countDown();
        });

        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            thread.start();
        }

        latch.await(5, TimeUnit.SECONDS);

        assertThat(refTags.get()).hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue));
    }

    @Test
    public void verifyNoContextProgapationViaDirectRunInvocation() {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();


        Thread thread = new Thread() {
            @Override
            public synchronized void start() {
                //do not actually start a thread, just attach the context.
            }

            @Override
            public void run() {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                refTags.set(iter);
            }
        };

        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            thread.start();
        }

        thread.run();

        assertThat(refTags.get()).isEmpty();
    }

    @Test
    public void verifyContextProgapationUsingSubClasses() throws InterruptedException {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
            refTags.set(iter);
            latch.countDown();
        }) {
            @Override
            public synchronized void start() {
                super.start();
            }
        };

        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            thread.start();
        }

        latch.await(5, TimeUnit.SECONDS);

        assertThat(refTags.get()).hasSize(1)
                .extracting("key", "value")
                .contains(tuple(tagKey, tagValue));
    }

    @Test
    public void noContextProgapationViaConstructor() throws InterruptedException {
        TagKey tagKey = TagKey.create("test-tag-key");
        TagValue tagValue = TagValue.create("test-tag-value");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Iterator<Tag>> refTags = new AtomicReference<>();

        Thread thread;

        try (Scope s = tagger.currentBuilder().put(tagKey, tagValue).buildScoped()) {
            thread = new Thread(() -> {
                Iterator<Tag> iter = InternalUtils.getTags(tagger.getCurrentTagContext());
                refTags.set(iter);
                latch.countDown();
            });
        }

        thread.start();

        latch.await(5, TimeUnit.SECONDS);

        assertThat(refTags.get()).hasSize(0);
    }
}
