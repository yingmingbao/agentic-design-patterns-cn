package com.strategist.ai.service.component;

import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

public class StreamTask {

    public Sinks.Many<String> sink;
    public Disposable disposable;
    public StringBuffer accumulator;

    public StreamTask(Sinks.Many<String> sink, Disposable disposable, StringBuffer accumulator) {
        this.sink = sink;
        this.disposable = disposable;
        this.accumulator = accumulator;
    }
}
