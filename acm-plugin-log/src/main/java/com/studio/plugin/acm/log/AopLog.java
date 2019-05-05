package com.studio.plugin.acm.log;

import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2017/11/8.
 */
public class AopLog {

    private static final String TAG_FORMAT = "%s/%s[%s]";
    private static final String TAG_CLASS_FORMAT = "%s.%s_%d";
    private static final String DEFAULT_DIR_NAME = "log";
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(new ThreadFactory() {
        private final AtomicInteger poolNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "OSLog #" + poolNumber.getAndIncrement());
            /*if (thread.isDaemon()) {
                thread.setDaemon(false);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }*/

            /*thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {

                }
            });*/
            return thread;
        }
    });

    private static final Map<Integer, String> logTagMap = new HashMap<>();

    static {
        logTagMap.put(Log.VERBOSE, "V/");
        logTagMap.put(Log.DEBUG, "D/");
        logTagMap.put(Log.INFO, "I/");
        logTagMap.put(Log.WARN, "W/");
        logTagMap.put(Log.ERROR, "E/");
        logTagMap.put(Log.ASSERT, "A/");
    }

    private static final HashMap<String, AopLog> INSTANCE = new HashMap<>();
    private static final SimpleDateFormat sdfLog = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdfFile = new SimpleDateFormat("yyMMdd");
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
    private static final String rootDir = "Android/data/calm/";
    private String dirName;
    private Class<?> logClass;
    private static int retentionTime = 7;

    private AopLog(String dirName, Class<?> logClass) {
        this.dirName = dirName;
        this.logClass = logClass;
    }

    public static AopLog getLog() {
        return getLog(DEFAULT_DIR_NAME);
    }

    public static AopLog getLog(String dirName) {
        return getLog(dirName, AopLog.class);
    }

    public static AopLog getLog(String dirName, Class<?> logClass) {
        AopLog log = INSTANCE.get(dirName);
        if (log == null) {
            log = new AopLog(dirName, logClass);
            INSTANCE.put(dirName, log);
        }
        return log;
    }

    public static void setRetentionTime(int retentionTime) {
        AopLog.retentionTime = retentionTime;
    }

    public File getOutputDir() {
        return new File(new File(Environment.getExternalStorageDirectory(), rootDir), dirName);
    }

    public File[] getLogFiles() {
        File dir = getOutputDir();
        if (dir.exists()) {
            return dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return checkFileName(dir, name);
                }
            });
        }
        return null;
    }

    private String getTag() {
        return getTag(dirName, logClass);
    }

    private static String getTag(String dirName, Class<?> logClass) {
        dirName = dirName != null ? dirName : DEFAULT_DIR_NAME;
        return String.format(TAG_FORMAT, dirName, createStackElementTag(logClass), getThreadName());
    }

    private static String getThreadName() {
        return Looper.getMainLooper() == Looper.myLooper() ? "main" : "thread-" + Thread.currentThread().getName();
    }

    private String getLogTag(int priority) {
        return logTagMap.get(priority) + getTag();
    }

    private static String createStackElementTag(Class<?> logClass) {
        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        for (StackTraceElement element : stackTraceElements) {
            String elementClassName = element.getClassName();
            if (!(elementClassName.equals(AopLog.class.getName())
                    || elementClassName.equals(logClass.getName()))) {
                String tag = element.getClassName();
                /*Matcher m = ANONYMOUS_CLASS.matcher(tag);
                if (m.find()) {
                    tag = m.replaceAll("");
                }*/
                //return tag.substring(tag.lastIndexOf('.') + 1);
                return String.format(TAG_CLASS_FORMAT, tag.substring(tag.lastIndexOf('.') + 1), element.getMethodName(), element.getLineNumber());
            }
        }
        return "UNKNOWN";
    }

    public void log(String msg, Object... args) {
        log(null, msg, args);
    }

    public void log(Throwable t, String msg, Object... args) {
        logInner(false, t, msg, args);
    }

    public void logImmediate(Throwable t, String msg, Object... args) {
        logInner(true, t, msg, args);
    }

    private void logInner(boolean immediate, Throwable t, String msg, Object... args) {
        final int priority = t != null ? Log.ERROR : Log.INFO;
        final String message = getMessage(t, msg, args);
        final File outDir = getOutputDir();
        if (immediate) {
            final String logTag = getLogTag(priority);
            writeLog(logTag, message, outDir, retentionTime);
        } else {
            final Date logDate = new Date();
            final String logTag = getLogTag(priority);
            EXECUTOR_SERVICE.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        writeLog(logDate, logTag, message, outDir, retentionTime);
                    } catch (Throwable throwable) {
                    }
                }
            });
        }
    }

    private static String getMessage(Throwable t, String msg, Object[] args) {
        if (TextUtils.isEmpty(msg)) {
            if (t != null) {
                msg = getStackTraceString(t);
            }
        } else {
            if (args != null && args.length > 0) {
                msg = String.format(msg, args);
            }

            if (t != null) {
                msg += "\n" + getStackTraceString(t);
            }
        }
        return msg;
    }

    public static String getStackTraceString(Throwable t) {
        if (t != null) {
            StringWriter sw = new StringWriter(256);
            PrintWriter pw = new PrintWriter(sw, false);
            t.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        }
        return null;
    }

    private synchronized void writeLog(String tag, String msg, File outDir, int retentionTime) {
        writeLog(new Date(), tag, msg, outDir, retentionTime);
    }

    private synchronized void writeLog(Date logDate, String tag, String msg, File outDir, int retentionTime) {
        try {
            checkIfExistsOrCreate(outDir);
            deleteDaysBeforeLogFile(retentionTime, outDir);
            File file = new File(outDir, String.format(outDir.getName() + "_%s.log", sdfFile.format(logDate)));
            FileOutputStream raf = new FileOutputStream(file, true);
            raf.write(String.format("[%s] %s: %s", sdfLog.format(logDate), tag, msg).getBytes());
            raf.write("\r\n".getBytes());
            raf.getFD().sync();
            raf.close();
        } catch (Throwable e) {
            Log.e(getTag(), "file write error:" + e);
        }
    }

    private void checkIfExistsOrCreate(File outDir) {
        if (outDir.exists() && !outDir.isDirectory()) {
            outDir.delete();
        }

        if (!outDir.exists()) {
            Log.i(getTag(), "mkdirs dir: " + outDir.getPath());
            outDir.mkdirs();
        }
    }

    private synchronized void deleteDaysBeforeLogFile(int beforeDay, File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (checkFileName(dir, file.getName())) {
                    String dateString = file.getName().replaceAll("[^0-9]", "");
                    try {
                        Date dateNow = sdfFile.parse(sdfFile.format(new Date()));
                        Date dateFile = sdfFile.parse(dateString);
                        if (dateFile.getTime() < dateNow.getTime() - TimeUnit.DAYS.toMillis(beforeDay)) {
                            Log.i(getTag(), "delete log file: " + file.getPath());
                            file.delete();
                        }
                    } catch (Exception e) {
                        Log.e(getTag(), "file delete error:" + e);
                    }
                }
            }
        }
    }

    private boolean checkFileName(File dir, String filename) {
        return Pattern.matches(dir.getName() + "_\\d{6}.log", filename);
    }
}
