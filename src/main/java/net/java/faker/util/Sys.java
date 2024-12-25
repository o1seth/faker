/*
 * This file is part of faker - https://github.com/o1seth/faker
 * Copyright (C) 2024 o1seth
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.java.faker.util;

public class Sys {
    public static final int WINDOWS = 0;
    public static final int LINUX = 1;
    public static final int MACOS = 2;
    public static final int ANDROID = 3;
    public static final int x64 = 0;
    public static final int x32 = 1;
    public static final int aarch64 = 2;
    public static final int aarch32 = 3;
    public static final int riscv64 = 4;
    public static final int PROCESS_ARCH;
    public static final int OS;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        String osVersion = System.getProperty("os.version").toLowerCase();
        if (osVersion.contains("android")) {
            OS = ANDROID;
        } else if (os.contains("win")) {
            OS = WINDOWS;
        } else if (os.contains("linux")) {
            OS = LINUX;
        } else if (os.contains("mac") || os.contains("osx") || os.contains("os x")) {
            OS = MACOS;
        } else {
            OS = -1;
        }

        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.equals("arm") || arch.equals("arm32") || arch.equals("aarch32") || arch.contains("armv7")) {
            PROCESS_ARCH = aarch32;
        } else if (arch.equals("arm64") || arch.equals("aarch64")) {
            PROCESS_ARCH = aarch64;
        } else if (arch.equals("x86_64") || arch.equals("amd64") || arch.equals("ia64")) {
            PROCESS_ARCH = x64;
        } else if (arch.equals("x86") || arch.equals("x86_32") || arch.equals("i386") || arch.equals("i486") || arch.equals("i586")
                || arch.equals("i686")) {
            PROCESS_ARCH = x32;
        } else if (arch.equals("riscv64")) {
            PROCESS_ARCH = riscv64;
        } else {
            PROCESS_ARCH = -1;
        }
    }


    public static boolean is32Bit() {
        return PROCESS_ARCH == x32 || PROCESS_ARCH == aarch32;
    }

    public static boolean isARM() {
        return PROCESS_ARCH == aarch64 || PROCESS_ARCH == aarch32;
    }

    public static boolean isAarch64() {
        return PROCESS_ARCH == aarch64;
    }

    public static boolean isX32() {
        return PROCESS_ARCH == x32;
    }

    public static boolean isX64() {
        return PROCESS_ARCH == x64;
    }

    public static boolean isAarch32() {
        return PROCESS_ARCH == aarch32;
    }

    public static boolean isRISCV64() {
        return PROCESS_ARCH == riscv64;
    }

    public static boolean isWindows() {
        return OS == WINDOWS;
    }

    public static boolean isMac() {
        return OS == MACOS;
    }

    public static boolean isLinux() {
        return OS == LINUX;
    }

    public static boolean isAndroid() {
        return OS == ANDROID;
    }

    public static boolean isLinuxOrAndroid() {
        return isLinux() || isAndroid();
    }


}
