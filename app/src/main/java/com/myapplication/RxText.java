package com.myapplication;


import android.util.Log;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

/**
 * Created by Administrator on 2017/3/22.
 */

public class RxText {
    public void aVoid() {
        Subscriber subscriber= new RXImpl();

        Flowable.create(new FlowableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(FlowableEmitter<Boolean> e) throws Exception {

            }
        }, BackpressureStrategy.BUFFER)

                .subscribe(subscriber);

    }


}
