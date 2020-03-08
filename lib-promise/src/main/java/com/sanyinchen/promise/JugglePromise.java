package com.sanyinchen.promise;

import androidx.annotation.NonNull;

import com.sanyinchen.promise.base.Iterator;

import java.util.Map;
import java.util.WeakHashMap;

public class JugglePromise {
    private Promise head;

    public JugglePromise(@NonNull Object ref) {
        ThreadLocal<Map<Object, Promise>> localPromise = new ThreadLocal<>();
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
        if (head == null) {
            return;
        }
        Iterator<Promise> iterator = head.iterator();
        while (head.isCompleted() && iterator.hasNext()) {
            head = iterator.next();
        }
        head.append(newPromise);

    }


}
