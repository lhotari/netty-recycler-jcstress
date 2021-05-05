package io.github.lhotari.nettyrecyclerjcstress;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

@State
@JCStressTest
@Outcome(id = "3, 3", expect = Expect.ACCEPTABLE, desc = "Normal case")
public class ConcurrentSkipListMapTest {
    private final ConcurrentSkipListMap<String, Integer> map = new ConcurrentSkipListMap<>();
    private final List<String> removedKeys = new CopyOnWriteArrayList<>();

    public ConcurrentSkipListMapTest() {
        map.put("a", 1);
        map.put("b", 1);
        map.put("c", 1);
    }

    @Actor
    public void actor1() {
        removeNext();
    }

    @Actor
    public void actor2() {
        removeNext();
    }

    @Actor
    public void actor3() {
        removeNext();
    }

    @Actor
    public void actor4() {
        removeNext();
    }

    private void removeNext() {
        Map.Entry<String, Integer> entry = map.pollFirstEntry();
        if (entry != null) {
            removedKeys.add(entry.getKey());
        }
    }

    @Arbiter
    public void arbiter(II_Result result) {
        result.r1 = removedKeys.size();
        result.r2 = new HashSet<>(removedKeys).size();
    }
}
