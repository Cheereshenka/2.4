package edu.labs;

/** Типизированный синхронный канал между фильтрами; вывод этапа — ввод следующего. */
public final class Pipeline<I, O> {
    private final Filter<I, O> filter;
    public Pipeline(Filter<I, O> filter) { this.filter = filter; }
    public <N> Pipeline<I, N> then(Filter<O, N> next) { return new Pipeline<>(input -> next.process(filter.process(input))); }
    public O execute(I input) throws Exception { return filter.process(input); }
}
