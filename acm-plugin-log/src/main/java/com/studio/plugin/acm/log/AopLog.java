package com.studio.plugin.acm.log;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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

    private static final String TAG_FORMAT = "%s[%s]";
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

    private static final HashMap<String, AopLog> INSTANCE = new HashMap<>();
    private static final SimpleDateFormat sdfLog = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat sdfFile = new SimpleDateFormat("yyMMdd");
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
    private static final String rootDir = "Android/data/";
    static final int MIN_RETENTION_TIME = 7;
    private String dirName;
    private int retentionTime;

    private AopLog(String dirName, int retentionTime) {
        this.dirName = dirName;
        this.retentionTime = retentionTime;
    }

    public static AopLog getLog() {
        return getLog(DEFAULT_DIR_NAME);
    }

    public static AopLog getLog(String dirName) {
        return getLog(dirName, MIN_RETENTION_TIME);
    }

    public static synchronized AopLog getLog(String dirName, int retentionTime) {
        AopLog log = INSTANCE.get(dirName);
        if (log == null) {
            log = new AopLog(dirName, retentionTime <= 0 ? MIN_RETENTION_TIME : retentionTime);
            INSTANCE.put(dirName, log);
        } else {
            if (log.retentionTime < retentionTime) {
                log.retentionTime = retentionTime;
            }
        }
        return log;
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
        return getTag(null);
    }

    private static String getTag(String dirName) {
        String tag = "";
        if (dirName != null) {
            tag = dirName + "/";
        }
        return tag + String.format(TAG_FORMAT, createStackElementTag(), Thread.currentThread().getName());
    }

    private static String createStackElementTag() {
        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        for (StackTraceElement element : stackTraceElements) {
            if (!element.getClassName().equals(AopLog.class.getName())) {
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

    public static void info(String msg, Object... args) {
        getLog().println(Log.INFO, null, msg, args);
    }

    public static void error(String msg, Object... args) {
        error(null, msg, args);
    }

    public static void error(Throwable t, String msg, Object... args) {
        getLog().println(Log.ERROR, t, msg, args);
    }

    public void println(int priority, String msg, Object... args) {
        println(priority, null, msg, args);
    }

    public void println(int priority, Throwable t, String msg, Object... args) {
        final String message = getMessage(t, msg, args);
        Log.println(priority, getTag(dirName), message != null ? message : "{null}");
    }

    public void log(String msg, Object... args) {
        log(null, msg, args);
    }

    public void log(Throwable t, String msg, Object... args) {
        logInner(false, t, msg, args);
    }

    public void logImmediate(Throwable t, String msg, Object... args) {
        logInner(true, t, msg, args);
        INSTANCE.clear();
        EXECUTOR_SERVICE.shutdown();
    }

    private void logInner(boolean immediate, Throwable t, String msg, Object... args) {
        final int priority = t != null ? Log.ERROR : Log.INFO;
        final String message = getMessage(t, msg, args);
        println(priority, message);
        final File outDir = getOutputDir();
        if (immediate) {
            final String logTag = getTag();
            writeLog(logTag, message, outDir, retentionTime);
        } else {
            final Date logDate = new Date();
            final String logTag = getTag();
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
            FileOutputStream stream = new FileOutputStream(file, true);
            BufferedOutputStream raf = new BufferedOutputStream(new FileOutputStream(file, true));
            raf.write(String.format("[%s] %s: %s", sdfLog.format(logDate), tag, msg).getBytes());
            raf.write("\r\n".getBytes());
            stream.getFD().sync();
            raf.flush();
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
