package ca.phon.ui.nativedialogs.ffm.mac;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Run in its own JVM by {@link GcdTest}: a fault here ends the process.
 *
 * <p>Submits work with {@link Gcd#onMainAsync} while other threads force garbage
 * collections and stack walks. Half of the work outlasts the submitting call, as a
 * dialog does; the other half finishes at once. Exits 0 when every submission ran.
 */
public final class GcdAsyncStress {

    public static void main(String[] args) throws Exception {
        final long end = System.nanoTime() + Long.parseLong(args[0]) * 1_000_000L;
        final AtomicLong ran = new AtomicLong();
        long submitted = 0;

        final Thread walker = new Thread(() -> {
            while (System.nanoTime() < end) Thread.getAllStackTraces();
        }, "stack-walker");
        final Thread collector = new Thread(() -> {
            while (System.nanoTime() < end) {
                System.gc();
                try { Thread.sleep(5); } catch (InterruptedException e) { return; }
            }
        }, "collector");
        walker.setDaemon(true);
        collector.setDaemon(true);
        walker.start();
        collector.start();

        while (System.nanoTime() < end) {
            if (submitted % 2 == 0) {
                final AtomicBoolean callerReturned = new AtomicBoolean();
                Gcd.onMainAsync(() -> {
                    while (!callerReturned.get()) Thread.onSpinWait();
                    ran.incrementAndGet();
                });
                callerReturned.set(true);
            } else {
                Gcd.onMainAsync(ran::incrementAndGet);
            }
            submitted++;
            while (submitted - ran.get() > 20) Thread.onSpinWait();
        }

        final long deadline = System.nanoTime() + 10_000_000_000L;
        while (ran.get() < submitted && System.nanoTime() < deadline) Thread.sleep(10);
        System.out.println("submitted=" + submitted + " ran=" + ran.get());
        System.exit(ran.get() == submitted ? 0 : 3);
    }
}
