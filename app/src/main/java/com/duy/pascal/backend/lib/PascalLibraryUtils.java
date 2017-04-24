/*
 *  Copyright 2017 Tran Le Duy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duy.pascal.backend.lib;

import com.duy.pascal.backend.lib.android.AndroidBatteryLib;
import com.duy.pascal.backend.lib.android.AndroidBluetoothLib;
import com.duy.pascal.backend.lib.android.AndroidMediaPlayerLib;
import com.duy.pascal.backend.lib.android.AndroidSensorLib;
import com.duy.pascal.backend.lib.android.AndroidSettingLib;
import com.duy.pascal.backend.lib.android.AndroidTextToSpeakLib;
import com.duy.pascal.backend.lib.android.AndroidToneGeneratorLib;
import com.duy.pascal.backend.lib.android.AndroidUtilsLib;
import com.duy.pascal.backend.lib.android.AndroidWifiLib;
import com.duy.pascal.backend.lib.android.utils.AndroidLibraryManager;
import com.duy.pascal.backend.lib.android.utils.AndroidLibraryUtils;
import com.duy.pascal.backend.lib.annotations.PascalMethod;
import com.duy.pascal.backend.lib.graph.GraphLib;
import com.duy.pascal.backend.lib.io.InOutListener;
import com.duy.pascal.backend.lib.math.MathLib;
import com.duy.pascal.frontend.activities.ExecHandler;
import com.duy.pascal.frontend.activities.RunnableActivity;
import com.google.common.collect.ListMultimap;
import com.js.interpreter.ast.AbstractFunction;
import com.js.interpreter.ast.MethodDeclaration;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Created by Duy on 08-Apr-17.
 */

public class PascalLibraryUtils {
    public static final ArrayList<String> SUPPORT_LIB;

    static {
        SUPPORT_LIB = new ArrayList<>();
        SUPPORT_LIB.add("crt");
        SUPPORT_LIB.add("dos");
        SUPPORT_LIB.add("math");
        SUPPORT_LIB.add("graph");
        SUPPORT_LIB.add("strutils");
        SUPPORT_LIB.add("abattery");
        SUPPORT_LIB.add("amedia");
        SUPPORT_LIB.add("asensor");
        SUPPORT_LIB.add("autils");
        SUPPORT_LIB.add("atone");
        SUPPORT_LIB.add("atextspeak");
        SUPPORT_LIB.add("awifi");
        SUPPORT_LIB.add("asetting");
        SUPPORT_LIB.add("abluetooth");
    }

    /**
     * get method of class, call by java reflect
     *
     * @param classes  - list class
     * @param modifier - allow method modifier
     */
    public static void addMethodFromClass(ArrayList<Class<?>> classes, int modifier,
                                          RunnableActivity handler,
                                          ListMultimap<String, AbstractFunction> callableFunctions) {

        AndroidLibraryManager facadeManager = new AndroidLibraryManager(AndroidLibraryUtils.getSdkLevel(),
                handler, AndroidLibraryUtils.getFacadeClasses());

        for (Class<?> pascalPlugin : classes) {
            Constructor constructor;
            Object o = null;
            try {
                constructor = pascalPlugin.getConstructor(InOutListener.class);
                o = constructor.newInstance(handler);
            } catch (Exception ignored) {
            }
            if (o == null) {
                try {
                    constructor = pascalPlugin.getConstructor(ExecHandler.class);
                    o = constructor.newInstance(handler);
                } catch (Exception ignored) {
                }
            }
            if (o == null) {
                try {
                    constructor = pascalPlugin.getConstructor(AndroidLibraryManager.class);
                    o = constructor.newInstance(facadeManager);
                } catch (Exception ignored) {
                }
            }
            if (o == null) {
                try {
                    constructor = pascalPlugin.getConstructor();
                    o = constructor.newInstance();
                } catch (Exception ignored) {
                }
            }
            for (Method m : pascalPlugin.getDeclaredMethods()) {
                if (m.isAnnotationPresent(PascalMethod.class)) {
                    MethodDeclaration tmp = new MethodDeclaration(o, m);
                    callableFunctions.put(tmp.name().toLowerCase(), tmp);
                }
            }

        }
    }

    public static void addMethodFromClass(Class<?> pascalPlugin, int modifier, RunnableActivity handler, ListMultimap<String, AbstractFunction> callableFunctions) {
        Object o = null;
        try {
            Constructor constructor = pascalPlugin.getConstructor(InOutListener.class);
            o = constructor.newInstance(handler);
        } catch (Exception ignored) {
        }
        if (o == null) {
            try {
                Constructor constructor;
                constructor = pascalPlugin.getConstructor(ExecHandler.class);
                o = constructor.newInstance(handler);
            } catch (Exception ignored) {
            }
        }
        if (o == null) {
            try {
                Constructor constructor;
                constructor = pascalPlugin.getConstructor();
                o = constructor.newInstance();
            } catch (Exception ignored) {
            }
        }
        for (Method m : pascalPlugin.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                MethodDeclaration tmp = new MethodDeclaration(o, m);
                callableFunctions.put(tmp.name().toLowerCase(), tmp);
            }
        }
    }

    /**
     * load library
     *
     * @param newLibraries
     * @param callableFunctions
     */
    public static void loadLibrary(ArrayList<String> source, ArrayList<String> newLibraries,
                                   RunnableActivity handler, ListMultimap<String, AbstractFunction> callableFunctions) {
        source.addAll(newLibraries);
        ArrayList<Class<?>> classes = new ArrayList<>();
        for (String name : newLibraries) {
            if (name.equalsIgnoreCase("crt")) {
                classes.add(CrtLib.class);
            } else if (name.equalsIgnoreCase("dos")) {
                classes.add(DosLib.class);
            } else if (name.equalsIgnoreCase("math")) {
                classes.add(MathLib.class);
            } else if (name.equalsIgnoreCase("graph")) {
                classes.add(GraphLib.class);
            } else if (name.equalsIgnoreCase("graph")) {
                classes.add(GraphLib.class);
            } else if (name.equalsIgnoreCase("strUtils")) {
                classes.add(StrUtilsLibrary.class);
            } else if (name.equalsIgnoreCase("ABattery")) {
                classes.add(AndroidBatteryLib.class);
            } else if (name.equalsIgnoreCase("AMediaPlayer")) {
                classes.add(AndroidMediaPlayerLib.class);
            } else if (name.equalsIgnoreCase("ASensor")) {
                classes.add(AndroidSensorLib.class);
            } else if (name.equalsIgnoreCase("AUtils")) {
                classes.add(AndroidUtilsLib.class);
            } else if (name.equalsIgnoreCase("AToneGenerator")) {
                classes.add(AndroidToneGeneratorLib.class);
            } else if (name.equalsIgnoreCase("ATextToSpeak")) {
                classes.add(AndroidTextToSpeakLib.class);
            } else if (name.equalsIgnoreCase("AWifi")) {
                classes.add(AndroidWifiLib.class);
            } else if (name.equalsIgnoreCase("ASetting")) {
                classes.add(AndroidSettingLib.class);
            } else if (name.equalsIgnoreCase("ABluetooth")) {
                classes.add(AndroidBluetoothLib.class);
            }
        }
        PascalLibraryUtils.addMethodFromClass(classes, Modifier.PUBLIC, handler, callableFunctions);
    }

}