package com.sanyinchen.promise;


import android.util.Log;

import com.sanyinchen.promise.base.Iterator;
import com.sanyinchen.promise.function.PromiseFunction1;

public class JugglePromise {
    private Promise head;

    public JugglePromise() {
        ThreadLocal<Promise> localPromise = new ThreadLocal<>();
        if (localPromise.get() != null) {
            localPromise.remove();
        }
        localPromise.set(new Promise<>());
        head = localPromise.get();
        head.emit();
    }

    private PromiseFunction1<Object, Void> createDefaultFormatPromiseFunc() {
        return new PromiseFunction1<Object, Void>() {
            @Override
            public Void call(Object o) {
                return null;
            }
        };
    }

    public int size() {
        return head.size();
    }

    public JugglePromise append(Promise newPromise) {
        Iterator<Promise> iterator = head.iterator();
        int size = head.size();
        while (head.isCompleted() && iterator.hasNext()) {
            head = iterator.next();
        }
        System.out.println("===========>head:" + head);
        int size2 = head.size();
        head.append(newPromise);
        int size3 = head.size();
        Log.i("src_test_2", "size1:" + size + " size2:" + size2 + " size3:" + size3);
        return this;
    }


}
