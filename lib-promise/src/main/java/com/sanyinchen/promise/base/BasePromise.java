package com.sanyinchen.promise.base;

import androidx.annotation.NonNull;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.exceptions.OnErrorThrowable;
import rx.functions.*;
import rx.subjects.ReplaySubject;

import java.util.Iterator;
import java.util.NoSuchElementException;


public abstract class BasePromise<T> extends DefaultObserver<T> {

    /**
     * 定义当前Promise状态
     */
    public static class STATE {

        /** 完成状态掩码 */
        static final int COMPLETED_MASK = 0x100;

        /** pending 状态 */
        static final int PENDING = 0x1;

        /** 拒绝状态 */
        static final int REJECTED = 0x10 | COMPLETED_MASK;

        /** 完成状态 */
        static final int COMPLETED = COMPLETED_MASK;
    }

    /** 当前状态 */
    private int state = STATE.PENDING;
    /** 当前传递值 */
    private T value = null;
    /** reject 异常 */
    private Throwable reason;
    /** 基础Subject */
    private ReplaySubject<T> innerSubject;
    /** Observable */
    private Observable<T> obs;
    /** 下一个Promise 节点 */
    private BasePromise next;
    /** 是否已经取消订阅 */
    private volatile boolean isUnSubscribe = false;

    public BasePromise() {
        this.innerSubject = ReplaySubject.create();
        next = null;
        obs = innerSubject.last();
        obs.subscribe(new Observer<T>() {
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

    /**
     * 当前是否是pending状态
     *
     * @return
     */
    public boolean isPending() {
        return (this.state & STATE.PENDING) == STATE.PENDING;
    }

    /**
     * 当前是否是reject状态
     *
     * @return
     */
    public boolean isRejected() {
        return (this.state & STATE.REJECTED) == STATE.REJECTED;
    }

    /**
     * 当前是否是完成状态
     *
     * @return
     */
    public boolean isCompleted() {
        return (this.state & STATE.COMPLETED) == STATE.COMPLETED;
    }

    /**
     * Promise 链表
     *
     * @param onNext
     * @param onRejected
     * @param <O>
     * @return
     */
    protected <O> BasePromise<O> innerThen(
            final Function onNext,
            final Function onRejected) {
        final BasePromise<O> next = create();
        this.next = next;
        this.obs.subscribe(new NextObserver<>(onNext, onRejected));

        return next;
    }

    /**
     * callFunction 调用封装
     *
     * @param function
     * @param value
     * @param <O>
     * @return
     * @throws IllegalArgumentException
     */
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

        throw new IllegalArgumentException("Could not correctly invoke callback function with type "
                + function.getClass().toString());
    }

    /**
     * 实例化下一个节点
     *
     * @param <O>
     * @return
     */
    protected abstract <O> BasePromise<O> create();

    /**
     * reject 操作
     *
     * @param reason
     */
    public void reject(Throwable reason) {
        if (reason instanceof Exception) {
            this.innerSubject.onError(reason);
        } else {
            this.innerSubject.onError(OnErrorThrowable.from(reason));
        }
    }

    /**
     * 启动调用链
     *
     * @param value
     * @return
     */
    public BasePromise<T> emit(T value) {
        if (isUnSubscribe()) {
            return this;
        }
        this.innerSubject.onNext(value);
        this.innerSubject.onCompleted();
        return this;
    }

    /**
     * 启动调用链
     *
     * @return
     */
    public BasePromise<T> emit() {
        return emit(null);
    }

    /**
     * 切换后续observer执行线程
     *
     * @param scheduler
     * @return
     */
    public BasePromise<T> observerOn(@NonNull Scheduler scheduler) {
        this.obs = this.obs.observeOn(scheduler);
        return this;
    }

    /**
     * 当前promise是否已经取消订阅
     *
     * @return
     */
    private boolean isUnSubscribe() {
        return isUnSubscribe;
    }

    /**
     * 设置取消订阅状态
     */
    private void markUnSubscribeInner() {
        isUnSubscribe = true;
    }

    /**
     * 取消订阅
     */
    public void unSubscribe() {
        Iterator<BasePromise> iterator = iterator();
        while (iterator.hasNext()) {
            BasePromise temp = iterator.next();
            if (temp != null) {
                temp.markUnSubscribeInner();
            }
        }
    }

    /**
     * Promise 遍历迭代器， 不支持remove操作
     *
     * @param <T>
     */
    public static class ListIterator<T extends BasePromise> implements Iterator<T> {
        private T current;

        public ListIterator(T first) {
            current = first;
        }

        public boolean hasNext() {
            return current != null;
        }

        public T next() {
            if (!hasNext()) {
                return null;
            }
            T item = current;
            current = (T) current.getNext();
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

    /**
     * 添加Promise节点
     *
     * @param next
     */
    public void append(@NonNull final BasePromise next) {
        Iterator<BasePromise> iterator = iterator();
        BasePromise head = this;
        while (iterator.hasNext()) {
            head = iterator.next();
        }
        head.next = next;
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
    }

    /**
     * 获取当前Promise迭代器
     *
     * @param <O>
     * @return
     */
    public <O extends BasePromise> Iterator<O> iterator() {
        return new ListIterator<O>((O) this);
    }

    /**
     * get size
     *
     * @return
     */
    public int size() {
        Iterator iterator = new ListIterator(this);
        int size = 0;
        while (iterator.hasNext()) {
            iterator.next();
            size++;
        }
        return size;
    }

    /**
     * 获取下一个Promise节点
     *
     * @param <O>
     * @return
     */
    public <O> BasePromise<O> getNext() {
        return next;
    }

    /**
     * 创建Observer类
     *
     * @param <T>
     */
    private class NextObserver<T> extends DefaultObserver<T> {

        /** onNext callback */
        private final Function onNext;
        /** onRejected callback */
        private final Function onRejected;

        public NextObserver(Function onNext, Function onRejected) {
            this.onNext = onNext;
            this.onRejected = onRejected;
        }

        @Override
        public void onCompleted() {
            super.onCompleted();
            this.evaluate();
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            this.evaluate();
        }

        /**
         * Observer回调封装
         */
        private void evaluate() {
            try {
                if (isUnSubscribe()) {
                    return;
                }
                if (BasePromise.this.state == STATE.REJECTED) {
                    this.evaluateRejected();
                    return;
                }

                this.evaluateFulfilled();

            } catch (Throwable e) {
                e.printStackTrace();
                next.reject(e);
            }
        }

        /**
         * 执行onNext回调
         */
        private <O> void evaluateFulfilled() {
            if (onNext != null) {
                O result = (O) callFunction(onNext, BasePromise.this.value);
                evalResult(result);
            } else {
                next.emit((O) BasePromise.this.value);
            }
        }

        /**
         * 执行onRejected回调
         */
        private <O> void evaluateRejected() {
            if (onRejected != null) {
                O result = (O) callFunction(onRejected, BasePromise.this.reason);
                evalResult(result);
            } else {
                next.reject(BasePromise.this.reason);
            }
        }

        private <O> void evalResult(O result) {
            next.emit(result);
        }
    }

    ;
}
