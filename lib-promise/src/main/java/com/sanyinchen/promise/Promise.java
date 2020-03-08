package com.sanyinchen.promise;

import com.sanyinchen.promise.base.BasePromise;
import com.sanyinchen.promise.function.PromiseAction;
import com.sanyinchen.promise.function.PromiseAction1;
import com.sanyinchen.promise.function.PromiseFunction;
import com.sanyinchen.promise.function.PromiseFunction1;

import rx.Scheduler;
import rx.functions.Function;

public class Promise<T> extends BasePromise<T> {
    public Promise() {
        super();
    }


    @Override
    public <O> BasePromise<O> create() {
        return new Promise<>();
    }

    public <O> Promise<O> then(PromiseFunction1<T, O> onNext) {
        return this.promise(onNext, null);
    }

    public <O> Promise<O> then(PromiseFunction<O> onNext) {
        return this.promise(onNext, null);
    }

    public Promise<Void> then(PromiseAction onNext,
                              PromiseAction1<Exception> onRejected) {
        return this.promise(onNext, onRejected);
    }

    public Promise<Void> then(PromiseAction1<T> onNext,
                              PromiseAction1<Exception> onRejected) {
        return this.promise(onNext, onRejected);
    }

    public Promise<Void> then(PromiseAction onNext) {
        return this.promise(onNext, null);
    }

    public Promise<Void> then(PromiseAction1<T> onNext) {
        return this.promise(onNext, null);
    }

    public <O> Promise<O> then(PromiseFunction1<T, O> onNext,
                               PromiseAction1<Exception> onRejected) {
        return this.promise(onNext, onRejected);
    }

    @Override
    public Promise<T> observerOn(Scheduler scheduler) {
        super.observerOn(scheduler);
        return this;
    }

    protected <O> Promise<O> promise(Function onNext, Function onRejected) {
        return (Promise<O>) innerThen(onNext, onRejected);
    }

    @Override
    public <O> Promise<O> next() {
        return (Promise<O>) super.next();
    }

    @Override
    public <O> Promise<O> pre() {
        return (Promise<O>) super.pre();
    }

    @Override
    public <O> Promise<O> append(BasePromise<O> next) {
        return (Promise<O>) super.append(next);
    }
}

