package com.studio.plugin.acm.profiler;


import com.studio.plugin.acm.profiler.asm.AopClassVisitor;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Set;


/**
 * AOP执行的调用入口
 *
 * @author yangyp
 */
public class AopEngine {

    public static final int DEFAULT_EXECUTE_TIMEOUT = 1000 >> 4;

    public interface FileRecurseCallback {
        void onFileRecurse(File file);
    }

    static void GLog(String message, Object... args) {
        System.out.println("[AopEngine] " + String.format(message, args));
    }

    public static void doAspect(final File classDir, final String includePackage, final Set<String> excludeClass, final String aopClassName, final int executeTimeout) {
        eachFileRecurse(classDir, file -> {
            if (!isAppClass(file.getPath())) {
                return;
            }

            String className = getClassName(classDir.getPath(), file.getPath());
            if (isIncluded(className, includePackage)) {
                if (isExcluded(className, excludeClass)
                        || (aopClassName != null && className.startsWith(aopClassName))) {
                    GLog("[N]: %s", file);
                } else {
                    GLog("[Y]: %s", file);
                    AopEngine.doAspect(file, /*includePackage*/aopClassName, executeTimeout);
                }
            }
        });
    }

    private static String getClassName(String classDir, String classPath) {
        String className = classPath.substring(classPath.indexOf(classDir) + classDir.length() + 1);
        className = className.replace('\\', '.');
        return className;
    }

    public static void eachFileRecurse(File classDir, FileRecurseCallback callback) {
        if (classDir.exists()) {
            File[] childFiles = classDir.listFiles();
            for (File file : childFiles) {
                if (file.isDirectory()) {
                    eachFileRecurse(file, callback);
                }
                if (callback != null) {
                    callback.onFileRecurse(file);
                }
            }
        }
    }

    private static boolean isAppClass(String classPath) {
        classPath = classPath.replace('\\', '/');
        if (classPath.endsWith(".class") && !(
                classPath.contains("/R$")
                        || classPath.endsWith("/R.class")
                        || classPath.endsWith("/BuildConfig.class")
                        || classPath.contains("/android/support/"))) {
            return true;
        }
        return false;
    }

    public static boolean isIncluded(String path, String includePackage) {
        if (includePackage != null) {
            return path.contains(includePackage);
        }
        return true;
    }

    public static boolean isExcluded(String path, Set<String> excludeClass) {
        if (excludeClass != null) {
            for (String exclude : excludeClass) {
                if (path.contains(exclude)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 调用aop
     *
     * @param classFile    类文件
     * @param aopClassName 目标包名，要被修改的类的包名
     * @return
     */
    public static void doAspect(File classFile, String aopClassName, int executeTimeout) {
        InputStream is = null;
        FileOutputStream fout = null;
        try {
            is = new FileInputStream(classFile);
            byte[] tBytes = doAspect(is, aopClassName, executeTimeout);
            fout = new FileOutputStream(classFile);
            fout.write(tBytes);
            fout.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
            }
            try {
                if (fout != null) {
                    fout.close();
                }
            } catch (Exception e1) {
            }
        }
    }

    /**
     * 调用aop
     *
     * @param aopClassName     目标包名，要被修改的类的包名
     * @param classInputStream 类的输入流
     * @return
     * @throws Exception
     */
    public static byte[] doAspect(InputStream classInputStream, String aopClassName, int executeTimeout) throws Exception {
        return doAspect(new ClassReader(classInputStream), aopClassName, executeTimeout);
    }

    /**
     * 调用aop
     *
     * @param aopClassName 目标包名，要被修改的类的包名
     * @param classBytes   类的二进制数据
     * @return
     * @throws Exception
     */
    public static byte[] doAspect(byte[] classBytes, String aopClassName) throws Exception {
        return doAspect(new ClassReader(classBytes), aopClassName, DEFAULT_EXECUTE_TIMEOUT);
    }

    private static byte[] doAspect(ClassReader classReader, String aopClassName, int executeTimeout) {
        executeTimeout = executeTimeout <= 0 ? DEFAULT_EXECUTE_TIMEOUT : executeTimeout;
        ClassWriter tClassWrite = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        AopClassVisitor tAopClassVisitor = new AopClassVisitor(Opcodes.ASM5, tClassWrite, aopClassName, executeTimeout);
        classReader.accept(tAopClassVisitor, ClassReader.EXPAND_FRAMES);
        byte[] tAspectedClassByte = tClassWrite.toByteArray();
        return tAspectedClassByte;
    }

}