package edu.labs;

@FunctionalInterface
public interface Filter<I, O> { O process(I input) throws Exception; }
