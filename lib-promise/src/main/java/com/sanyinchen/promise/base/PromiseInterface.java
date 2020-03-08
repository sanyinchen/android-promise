package com.sanyinchen.promise.base;

import rx.Scheduler;

public interface PromiseInterface<T> {

    <O> void append(BasePromise<O> next);

    BasePromise<T> observerOn(Scheduler scheduler);


}
