package com.sanyinchen.promise;

import androidx.annotation.NonNull;

import com.sanyinchen.promise.base.BasePromise;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class JugglePromise {
    private Promise head;
    private final ThreadLocal<Map<Object, Promise>> localPromise;
    private WeakReference<Object> mRef;

    public JugglePromise(@NonNull Object ref) {
        localPromise = new ThreadLocal<>();
        mRef = new WeakReference<>(ref);
    }

    private void init() {
        Object ref = mRef.get();
        if (ref == null) {
            return;
        }
        Map<Object, Promise> mapCache = localPromise.get();
        if (mapCache == null) {
            mapCache = new WeakHashMap<Object, Promise>();
            localPromise.set(mapCache);
        }
        head = mapCache.get(ref);
        if (mapCache.get(ref) == null) {
            head = new Promise();
            mapCache.put(ref, head);
        }
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
        localPromise.set(null);
        head.unSubscribe();
        head = null;
    }


}
