package com.sanyinchen.promise.base;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.exceptions.OnErrorThrowable;
import rx.functions.*;
import rx.subjects.ReplaySubject;

import java.util.NoSuchElementException;


public abstract class BasePromise<T> extends DefaultObserver<T> {
    public static class STATE {
        static final int COMPLETED_MASK = 0x100;

        static final int PENDING = 0x1;

        static final int REJECTED = 0x10 | COMPLETED_MASK;

        static final int COMPLETED = COMPLETED_MASK;
    }

    private int state = STATE.PENDING;
    private T value = null;
    private Throwable reason;
    private ReplaySubject<T> innerSubject;
    private Observable<T> obs;
    private BasePromise next;
    private BasePromise pre;

    public BasePromise() {
        this.innerSubject = ReplaySubject.create();
        this.next = null;
        // promise states
        this.obs = this.innerSubject.last();
        this.obs.subscribe(new Observer<T>() {
            @Override
            public void onCompleted() {
                BasePromise.this.state = STATE.COMPLETED;
            }

            @Override
            public void onError(Throwable reason) {
                BasePromise.this.state = STATE.REJECTED;
                BasePromise.this.reason = reason;
            }

            @Override
            public void onNext(T value) {
                BasePromise.this.value = value;
            }
        });
    }

    public boolean isPending() {
        return (this.state & STATE.PENDING) == STATE.PENDING;
    }

    public boolean isRejected() {
        return (this.state & STATE.REJECTED) == STATE.REJECTED;
    }

    public boolean isCompleted() {
        return (this.state & STATE.COMPLETED) == STATE.COMPLETED;
    }

    protected <O> BasePromise<O> innerThen(
            final Function onNext,
            final Function onRejected) {
        // This is the next promise in the chain.
        // The handlers you see below will resolve their values and forward them
        // to this promise.
        final BasePromise<O> next = create();
        // Create the Observer
        final Observer<T> observer = new DefaultObserver<T>() {
            @Override
            public void onCompleted() {
                super.onCompleted();
                evaluate();
            }

            @Override
            public void onError(Throwable e) {
                super.onError(e);
                evaluate();
            }

            private void evaluate() {
                try {

                    if (BasePromise.this.state == STATE.REJECTED) {
                        evaluateRejected();
                        return;
                    }

                    evaluateFulfilled();

                } catch (Throwable e) {
                    // On any exception in the handlers above, we should throw the
                    // exception to the next promise
                    e.printStackTrace();
                    next.reject(e);
                }
            }

            private void evaluateFulfilled() {
                if (onNext != null) {
                    O result = (O) callFunction(onNext, BasePromise.this.value);
                    evalResult(result);
                } else {
                    // Sends the value forward. We assume that the casting will pass
                    next.emit((O) BasePromise.this.value);
                }
            }

            private void evaluateRejected() {
                if (onRejected != null) {
                    // Allow this handler to recover from the rejection
                    O result = (O) callFunction(onRejected, BasePromise.this.reason);
                    evalResult(result);
                } else {
                    // Forward it to the next promise
                    next.reject(BasePromise.this.reason);
                }
            }

            private void evalResult(O result) {
                next.emit(result);
            }
        };

        this.next = next;
        next.pre = this;
        this.obs.subscribe(observer);

        return next;
    }

    private <O> Object callFunction(Function function, O value) throws IllegalArgumentException {
        if (function instanceof Action0) {
            ((Action0) function).call();
            return null;
        }

        if (function instanceof Action1<?>) {
            ((Action1<O>) function).call(value);
            return null;
        }

        if (function instanceof Func0<?>) {
            return ((Func0<?>) function).call();
        }

        if (function instanceof Func1<?, ?>) {
            return ((Func1<O, ? super Object>) function).call(value);
        }

        throw new IllegalArgumentException("Could not correctly invoke callback function with type " + function.getClass().toString());
    }

    protected abstract <O> BasePromise<O> create();

    public void reject(Throwable reason) {
        if (reason instanceof Exception) {
            this.innerSubject.onError(reason);
        } else {
            this.innerSubject.onError(OnErrorThrowable.from(reason));
        }
    }


    public BasePromise<T> emit(T value) {
        this.innerSubject.onNext(value);
        this.innerSubject.onCompleted();
        return this;
    }

    // Scheduler @NotNull
    public BasePromise<T> observerOn(Scheduler scheduler) {
        this.obs = this.obs.observeOn(scheduler);
        return this;
    }

    public BasePromise<T> emit() {
        return emit(null);
    }

    public <O> void autoEmit() {
        autoEmit(null);
    }

    public <O> void autoEmit(O value) {
        ListIterator iterator = iterator();
        BasePromise head = this;
        while (iterator.hasPre()) {
            head = iterator.pre();
        }
        head.emit(value);
    }

    private class ListIterator<O> implements Iterator<BasePromise<O>> {
        private BasePromise<O> current;

        public ListIterator(BasePromise<O> first) {
            current = first;
        }

        public boolean hasNext() {
            return current != null;
        }

        public boolean hasPre() {
            return current != null;
        }


        public BasePromise<O> next() {
            if (!hasNext()) {
                return null;
            }
            BasePromise<O> item = current;
            current = current.next();
            return item;
        }

        public BasePromise<O> pre() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            BasePromise<O> item = current;
            current = current.pre();
            return item;
        }
    }

    @Override
    public void onCompleted() {
        this.emit(this.value);
    }

    @Override
    public void onError(Throwable e) {
        this.reject(e);
    }

    @Override
    public void onNext(T value) {
        this.value = value;
    }

    public <O> BasePromise<O> append(final BasePromise<O> next) {
        ListIterator iterator = iterator();
        BasePromise head = this;
        while (iterator.hasNext()) {
            head = iterator.next();
        }
        head.next = next;
        next.pre = head;
        head.obs.subscribe(new DefaultObserver() {
            @Override
            public void onCompleted() {
                next.emit();
            }

            @Override
            public void onError(Throwable e) {
                next.emit();
            }
        });
        ListIterator nextIterator = iterator();
        BasePromise nextEnd = next;
        while (nextIterator.hasNext()) {
            nextEnd = nextIterator.next();
        }
        return nextEnd;
    }


    public ListIterator<?> iterator() {
        return new ListIterator<>(this);
    }

    public int size() {
        Iterator iterator = new ListIterator(this);
        int size = 0;
        while (iterator.hasNext()) {
            iterator.next();
            size++;
        }
        return size;
    }

    public <O> BasePromise<O> next() {
        return this.next;
    }

    public <O> BasePromise<O> pre() {
        return this.pre;
    }
}
