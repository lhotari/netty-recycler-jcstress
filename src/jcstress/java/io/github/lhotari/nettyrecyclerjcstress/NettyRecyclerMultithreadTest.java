package io.github.lhotari.nettyrecyclerjcstress;

import io.netty.util.Recycler;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.bookkeeper.common.util.SafeRunnable;
import org.jctools.queues.MpmcArrayQueue;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IIIIIIII_Result;
import org.openjdk.jcstress.infra.results.IIIIIII_Result;

@JCStressTest
@State
@Description("Test Netty Recycler behavior with multiple threads.")
@Outcome(id = "-1, -1, -1, -1, -1, -1, -1", expect = Expect.ACCEPTABLE, desc = "Queue underrun")
@Outcome(id = "1, 1, 1, 2, 2, 2, 1", expect = Expect.ACCEPTABLE, desc = "Normal case")
public class NettyRecyclerMultithreadTest {
    private static class Holder {
        private final Recycler.Handle<Holder> handle;
        int a;

        Holder(Recycler.Handle<Holder> handle) {
            this.handle = handle;
        }

        void recycle() {
            handle.recycle(this);
        }
    }

    static class ResultHolder {
        private final int r1;
        private final int r2;
        private final int r3;
        private final int r4;
        private final int r5;
        private final int r6;
        private final int r7;

        ResultHolder(IIIIIII_Result r) {
            r1 = r.r1;
            r2 = r.r2;
            r3 = r.r3;
            r4 = r.r4;
            r5 = r.r5;
            r6 = r.r6;
            r7 = r.r7;
        }

        void applyToResult(IIIIIII_Result r) {
            r.r1 = r1;
            r.r2 = r2;
            r.r3 = r3;
            r.r4 = r4;
            r.r5 = r5;
            r.r6 = r6;
            r.r7 = r7;
        }
    }

    private static final MpmcArrayQueue<ResultHolder> QUEUE = new MpmcArrayQueue<>(8192);
    private static final ScheduledExecutorService EXECUTOR =
            Executors.newScheduledThreadPool(1, new DefaultThreadFactory("EXECUTOR", true));
    private static final ScheduledExecutorService EXECUTOR2 =
            Executors.newScheduledThreadPool(1, new DefaultThreadFactory("EXECUTOR2", true));
    private static final ScheduledExecutorService EXECUTOR3 =
            Executors.newScheduledThreadPool(1, new DefaultThreadFactory("EXECUTOR3", true));

    private static final Recycler<Holder> RECYCLER = new Recycler<Holder>() {
        @Override
        protected Holder newObject(Handle<Holder> handle) {
            return new Holder(handle);
        }
    };

    @Actor
    public void actor() {
        EXECUTOR.schedule(SafeRunnable.safeRun(() -> {
            Holder h = RECYCLER.get();
            h.a = 1;
            EXECUTOR2.schedule(SafeRunnable.safeRun(() -> {
                IIIIIII_Result r = new IIIIIII_Result();
                r.r1 = h.a;
                r.r2 = h.a;
                r.r3 = h.a;
                AtomicInteger executed = new AtomicInteger(0);
                // execute in the background to break happens before
                SafeRunnable job = SafeRunnable.safeRun(() -> {
                    r.r4 = h.a;
                    r.r5 = h.a;
                    r.r6 = h.a;
                    r.r7 = executed.get();
                    h.recycle();
                    QUEUE.offer(new ResultHolder(r));
                });
                h.a = 2;
                executed.set(1);
                EXECUTOR3.schedule(job, 20L, TimeUnit.MILLISECONDS);
            }), 1L, TimeUnit.NANOSECONDS);
        }), 1L, TimeUnit.NANOSECONDS);
    }

    @Arbiter
    public void arbiter(IIIIIII_Result r) {
        ResultHolder rh = QUEUE.poll();
        if (rh != null) {
            rh.applyToResult(r);
        } else {
            r.r1 = -1;
            r.r2 = -1;
            r.r3 = -1;
            r.r4 = -1;
            r.r5 = -1;
            r.r6 = -1;
            r.r7 = -1;
            Thread.yield();
        }
    }
}
