package dev.adventurers.core.systems;

import dev.adventurers.core.engine.*;
import java.util.*;

/** Reusable population-wide parameterized loop. Rules return commands; engine phases remain explicit. */
abstract class DomainSystem<T> implements LoopSystem<List<T>, List<DomainSystem.Need<T>>, List<DomainSystem.Work<T>>> {
    record Need<T>(T subject, double urgency) {}
    record Work<T>(T subject, double urgency) {}
    private final Definition definition;
    protected DomainSystem(String id, long period, String components, String inputs, String needs, String outputs) {
        definition = new Definition(id, period, List.of(components.split(",")), List.of(inputs.split(",")),
                List.of(needs.split(",")), List.of(outputs.split(",")));
    }
    @Override public final Definition definition() { return definition; }
    protected abstract double demand(LoopContext context, T subject);
    protected abstract void act(LoopContext context, T subject, double urgency);
    protected void respond(LoopContext context, T subject) {}
    protected void adapt(LoopContext context, T subject) {}
    @Override public List<Need<T>> evaluate(LoopContext context, List<T> perception) {
        return perception.stream().map(subject -> new Need<>(subject, demand(context, subject))).toList();
    }
    @Override public List<Work<T>> decide(LoopContext context, List<T> perception, List<Need<T>> needs) {
        return needs.stream().filter(n -> n.urgency() > 0).map(n -> new Work<>(n.subject(), n.urgency())).toList();
    }
    @Override public void execute(LoopContext context, List<Work<T>> decision) {
        for (var work : decision) act(context, work.subject(), work.urgency());
    }
    @Override public void feedback(LoopContext context, List<Work<T>> decision) {
        for (var work : decision) respond(context, work.subject());
    }
    @Override public void evolve(LoopContext context, List<Work<T>> decision) {
        for (var work : decision) adapt(context, work.subject());
    }
}
