package com.yangyp.profiler.plugin;

import com.studio.plugin.acm.log.LogAopInvoker;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        LogAopInvoker logAopInvoker1 = new LogAopInvoker(null,null,null,0);

        LogAopInvoker logAopInvoker2 = new LogAopInvoker(null,null,null,0);

        LogAopInvoker logAopInvoker3 = new LogAopInvoker(null,null,null,0);

        assertEquals(4, 2 + 2);
    }
}