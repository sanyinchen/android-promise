package com.sanyinchen.promise;

import androidx.annotation.NonNull;

import com.sanyinchen.promise.base.BasePromise;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class JugglePromise {
    private Promise head;
    private WeakReference<Object> mRef;

    private static final Map<Object, JugglePromise> promiseCache = new WeakHashMap<>();


    public static JugglePromise getInstance(@NonNull Object ref) {
        JugglePromise jugglePromise = promiseCache.get(ref);
        if (jugglePromise == null) {
            jugglePromise = new JugglePromise(ref);
            promiseCache.put(ref, jugglePromise);
        }
        return jugglePromise;
    }

    private JugglePromise(@NonNull Object ref) {
        mRef = new WeakReference<>(ref);
        init();
    }

    private void init() {
        head = new Promise();
        head.emit();
    }

    public int size() {
        if (head == null) {
            return 0;
        }
        return head.size();
    }

    public void append(Promise newPromise) {
        if (!isLiving()) {
            return;
        }
        if (head == null) {
            init();
        }
        Iterator<Promise> iterator = head.iterator();
        while (head.isCompleted() && iterator.hasNext()) {
            head = iterator.next();
        }
        head.append(newPromise);

    }

    private boolean isLiving() {
        return mRef.get() != null;
    }

    public void release() {
        if (head == null) {
            return;
        }
        head.unSubscribe();
        head = null;
    }



}
