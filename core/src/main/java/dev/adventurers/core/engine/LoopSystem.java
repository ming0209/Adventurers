package dev.adventurers.core.engine;

import java.util.List;

/** Every domain uses exactly these six hooks and the same scheduler. */
public interface LoopSystem<P, N, D> {
    record Definition(String id, long period, List<String> components, List<String> inputs,
                      List<String> needs, List<String> outputs) {
        public Definition {
            if (id == null || id.isBlank() || period < 1) throw new IllegalArgumentException();
            components = List.copyOf(components);
            inputs = List.copyOf(inputs);
            needs = List.copyOf(needs);
            outputs = List.copyOf(outputs);
        }
    }
    Definition definition();
    P perceive(LoopContext context);
    N evaluate(LoopContext context, P perception);
    D decide(LoopContext context, P perception, N needs);
    void execute(LoopContext context, D decision);
    void feedback(LoopContext context, D decision);
    void evolve(LoopContext context, D decision);
}
