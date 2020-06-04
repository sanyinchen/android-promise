# android-promise dependeny on rxjava V1.x

## 基础类
#### Promise
```
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
   
```
1. observerOn: 指定下一个then Action执行的线程
2. then: 设置当前需要执行的promise Action
   1. PromiseAction 无参Action
   2. PromiseAction1<I> 有参Action且类型为I
   3. PromiseFunction<O> 无参转换Func,结果类型为O
   4. PromiseFunction1<I, O> 将I类型参数转为O类型

#### JugglePromise:Promise组装器，将加入的promise串行执行   
 ```
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
        
 ```


## Screenshots
![](https://github.com/sanyinchen/android-promise/blob/master/source/1583687326551.gif) 
