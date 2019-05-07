package com.yangyp.profiler.plugin;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main();
            }
        });

        findViewById(R.id.mainAnr).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainAnr();
            }
        });


        findViewById(R.id.thread).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mainAnr();
                    }
                }).start();
            }
        });
    }

    static void main() {
        System.out.println("a");
    }

    static void mainAnr() {
        System.out.println("b");
        try {
            Thread.sleep(6 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        thread();
    }

    static void thread() {
        System.out.println("aa");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        main();
    }
}
