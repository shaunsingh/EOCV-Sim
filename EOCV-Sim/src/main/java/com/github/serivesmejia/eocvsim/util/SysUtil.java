/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.util;

import org.opencv.core.Core;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.stream.Collectors;

public class SysUtil {

    public static OperatingSystem OS = SysUtil.getOS();
    public static int MB = 1024 * 1024;
    public static String GH_NATIVE_LIBS_URL = "https://github.com/serivesmejia/OpenCVNativeLibs/raw/master/";

    public static OperatingSystem getOS() {

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains("nux")) {
            return OperatingSystem.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return OperatingSystem.MACOS;
        }

        return OperatingSystem.UNKNOWN;

    }

    public static void loadCvNativeLib() {

        String os = null;
        String fileExt = null;

        switch (OS) { //getting os prefix
            case WINDOWS:
                os = "win";
                fileExt = "dll";
                break;
            case LINUX:
                os = "linux";
                fileExt = "so";
                break;
            case MACOS:
                os = "mac";
                fileExt = "dylib";
                break;
        }

        boolean is64bit = System.getProperty("sun.arch.data.model").contains("64"); //Checking if JVM is 64 bits or not

        loadLib(os, fileExt, is64bit, Core.NATIVE_LIBRARY_NAME, 0);

    }

    public static void loadLib(String os, String fileExt, boolean is64bit, String name, int attempts) {

        String arch = is64bit ? "64" : "32"; //getting os arch

        String libName = os + arch + "_" + name; //resultant lib name from those two
        String libNameExt = libName + "." + fileExt; //resultant lib name from those two

        File nativeLibFile = new File(getAppData() + File.separator + libNameExt);

        if (!nativeLibFile.exists()) {
            Log.info("SysUtil", "Downloading native lib from " + GH_NATIVE_LIBS_URL + libNameExt);
            try {
                download(GH_NATIVE_LIBS_URL + libNameExt, nativeLibFile.getAbsolutePath());
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            Log.blank();
        }

        Log.info("SysUtil", "Loading native lib \"" + libNameExt + "\"");

        try {

            System.load(nativeLibFile.getAbsolutePath()); //Loading OpenCV native library
            Log.info("SysUtil", "Successfully loaded native lib \"" + libName + "\"");

        } catch (UnsatisfiedLinkError ex) {

            ex.printStackTrace();

            if (attempts < 4) {
                ex.printStackTrace();
                Log.error("SysUtil", "Failure loading lib \"" + libName + "\", retrying with different architecture... (" + attempts + " attempts)");
                loadLib(os, fileExt, !is64bit, Core.NATIVE_LIBRARY_NAME, attempts + 1);
            } else {
                ex.printStackTrace();
                Log.error("SysUtil", "Failure loading lib \"" + libName + "\" 4 times, the application will exit now.");
                System.exit(1);
            }

        }

    }

    public static CopyFileIsData copyFileIs(InputStream is, File toPath, boolean replaceIfExisting) throws IOException {

        boolean alreadyExists = true;

        if (toPath.exists()) {
            if (replaceIfExisting) {
                Files.copy(is, toPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                alreadyExists = false;
            }
        } else {
            Files.copy(is, toPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        is.close();

        CopyFileIsData data = new CopyFileIsData();
        data.alreadyExists = alreadyExists;
        data.file = toPath;

        return data;

    }

    public static CopyFileIsData copyFileIsTemp(InputStream is, String fileName, boolean replaceIfExisting) throws IOException {

        String tmpDir = System.getProperty("java.io.tmpdir");

        File tempFile = new File(tmpDir + File.separator + fileName);

        return copyFileIs(is, tempFile, replaceIfExisting);

    }

    public static long getMemoryUsageMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MB;
    }

    public static String loadIsStr(InputStream is, Charset charset) throws UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(is, String.valueOf(charset)))
                .lines().collect(Collectors.joining("\n"));
    }

    public static String loadFileStr(File f) {

        String content = "";

        try {
            content = new String(Files.readAllBytes(f.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;

    }

    public static boolean saveFileStr(File f, String contents) {

        try {
            FileWriter fw = new FileWriter(f);
            fw.append(contents);
            fw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    public static void download(String url, String fileName) throws Exception {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            Files.copy(in, Paths.get(fileName));
        }
    }

    public static File getAppData() {
        return new File(System.getProperty("user.home") + File.separator);
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public enum OperatingSystem {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    public static class CopyFileIsData {

        public File file = null;
        public boolean alreadyExists = false;

    }

}
