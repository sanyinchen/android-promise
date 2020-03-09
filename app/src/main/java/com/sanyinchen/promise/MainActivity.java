package com.sanyinchen.promise;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.sanyinchen.promise.function.PromiseAction;
import com.sanyinchen.promise.function.PromiseAction1;
import com.sanyinchen.promise.function.PromiseFunction1;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private TextView textView2;
    private int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("src_test", "onCreate===========>");
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.text);
        textView2 = findViewById(R.id.text2);

        textView.setText("hello init ");
        textView2.setText("hello text2");

        testMock();
//        testMock();
//        testMock();
//        testMock();
//        testMock2();
//        testMock();
//        testMock();
//        testMock();
//        testMock2();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                testMock2();
            }
        }, 10000);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                testMock2();
//                while (true) {
//                    try {
//                        //  Log.i("src_test", " promise size:" + uiJugglePromise.size());
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("src_test", "onPause=========>");
        long time1 = System.currentTimeMillis();
        JugglePromise.getInstance(this).release();
        Log.i("src_test", "cost:" + (System.currentTimeMillis() - time1));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("src_test", "onResume=========>");
//        testMock();
//        testMock2();

    }

    private void testMock() {
        textView.setText("hello mock block ");
        Promise promise = new Promise();
        promise.observerOn(Schedulers.io()).then(new PromiseAction() {
            @Override
            public void call() {
                Log.i("src_test", " testMock 1 in :" + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).then(new PromiseFunction1<Void, String>() {

            @Override
            public String call(Void aVoid) {
                return "1234";
            }
        }).observerOn(AndroidSchedulers.mainThread()).then(new PromiseAction1() {
            @Override
            public void call(Object o) {
                Log.i("src_test", " testMock 2 in :" + Thread.currentThread().getName() + " " + i);
                textView.setText(o + "mock finish :" + (i++));
            }

        });
        JugglePromise.getInstance(this).append(promise);
    }

    private void testMock2() {
        textView2.setText("hello mock block ");
        Promise promise = new Promise();
        promise.observerOn(Schedulers.newThread()).then(new PromiseAction() {
            @Override
            public void call() {
                Log.i("src_test", " testMock2 1 in :" + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).observerOn(AndroidSchedulers.mainThread()).then(new PromiseAction() {
            @Override
            public void call() {
                Log.i("src_test", " testMock2 2 in :" + Thread.currentThread().getName() + " " + i);
                textView2.setText("mock finish :" + (i++));

            }
        });
        JugglePromise.getInstance(this).append(promise);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("src_test", "onDestroy=========>");
    }
}
