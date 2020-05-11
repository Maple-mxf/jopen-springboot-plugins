package io.jopen.springboot.plugin.common;

/**
 * @author maxuefeng
 * @since 2020/5/8
 */
public class FileUtil {

    public static String getPrintSize(long size) {
        long rest;
        if (size < 1024) {
            return size + "B";
        } else {
            size /= 1024;
        }

        if (size < 1024) {
            return size + "KB";
        } else {
            rest = size % 1024;
            size /= 1024;
        }

        if (size < 1024) {
            size = size * 100;
            return size / 100 + "." + rest * 100 / 1024 % 100 + "MB";
        } else {
            size = size * 100 / 1024;
            return size / 100 + "." + size % 100 + "GB";
        }
    }

    public static void main(String[] args) {
        System.err.println(getPrintSize(48226L));
    }
}
