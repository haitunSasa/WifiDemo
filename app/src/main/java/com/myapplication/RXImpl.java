package com.myapplication;

import android.util.Log;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Created by Administrator on 2017/3/22.
 */

public class RXImpl implements Subscriber {
    @Override
    public void onSubscribe(Subscription s) {

    }

    @Override
    public void onNext(Object o) {

    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onComplete() {
        Log.i("onComplete","onComplete");
    }
}
