package com.techint.rfid.Auxiliares.Auxiliares;


import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public abstract class WeakHandler<T> extends Handler {

    private final WeakReference<T> weakRef;
    public WeakHandler(T t) {
        super();
        weakRef = new WeakReference<T>(t);
    }


    @Override
    public final void handleMessage(Message msg) {
        final T strongRef = weakRef.get();
        if (strongRef != null) {
            handleMessage(msg, strongRef);
        }
    }


    public abstract void handleMessage(Message msg, T t);

}