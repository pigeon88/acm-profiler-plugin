package com.yangyp.profiler.plugin;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread());
            }
        }).start();*/
    }
}
