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

package com.duy.pascal.frontend.view.code_view;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.KeyEvent;

import com.duy.pascal.frontend.EditorControl;
import com.duy.pascal.frontend.keyboard.KeyListener;
import com.duy.pascal.frontend.keyboard.KeySettings;
import com.duy.pascal.frontend.utils.UndoRedoHelper;
import com.duy.pascal.frontend.utils.clipboard.ClipboardManagerCompat;
import com.duy.pascal.frontend.utils.clipboard.ClipboardManagerCompatFactory;
import com.google.firebase.crash.FirebaseCrash;

//import android.util.Log;

/**
 * EditText with undo and redo support
 * <p>
 * Created by Duy on 15-Mar-17.
 */

public abstract class UndoRedoSupportEditText extends HighlightEditor {

    private static final boolean DEBUG = true;
    private static final String TAG = UndoRedoSupportEditText.class.getSimpleName();

    private UndoRedoHelper mUndoRedoHelper;
    private KeySettings mSettings;
    private KeyListener mKeyListener;
    private ClipboardManagerCompat mClipboardManager;
    private EditorControl editorControl;

    public UndoRedoSupportEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UndoRedoSupportEditText(Context context) {
        super(context);
        init();
    }

    public UndoRedoSupportEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mUndoRedoHelper = new UndoRedoHelper(this);
        mUndoRedoHelper.setMaxHistorySize(100);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mSettings = new KeySettings(mPrefs, getContext());
        mKeyListener = new KeyListener();
        mClipboardManager = ClipboardManagerCompatFactory.getManager(getContext());
    }


    /**
     * undo text
     */
    public void undo() {
        mUndoRedoHelper.undo();
    }

    /**
     * redo text
     */
    public void redo() {
        mUndoRedoHelper.redo();
    }

    /**
     * @return <code>true</code> if stack not empty
     */
    public boolean canUndo() {
        return mUndoRedoHelper.getCanUndo();
    }

    /**
     * @return <code>true</code> if stack not empty
     */
    public boolean canRedo() {
        return mUndoRedoHelper.getCanRedo();
    }

    /**
     * clear history
     */
    public void clearHistory() {
        mUndoRedoHelper.clearHistory();
    }

    public void saveHistory(String key) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        mUndoRedoHelper.storePersistentState(editor, key);
        editor.apply();
    }

    public void restoreHistory(String key) {
        try {
            mUndoRedoHelper.restorePersistentState(
                    PreferenceManager.getDefaultSharedPreferences(getContext()), key);
        } catch (Exception ignored) {

        }

    }

    /**
     * @param keyCode - key code event
     * @param down    - is down
     * @return - <code>true</code> if is ctrl key
     */
    private boolean handleControlKey(int keyCode, KeyEvent event, boolean down) {
        if (keyCode == mSettings.getControlKeyCode()
                || event.isCtrlPressed()) {
//            Log.w(TAG, "handler control key: ");
            mKeyListener.handleControlKey(down);
            return true;
        }
        return false;
    }

    /**
     * CTRL + C copy
     * CTRL + V paste
     * CTRL + B: compile
     * CTRL + R run
     * CTRL + X cut
     * CTRL + Z undo
     * CTRL + Y redo
     * CTRL + Q quit
     * CTRL + S save
     * CTRL + O open
     * CTRL + F find
     * CTRL + H find and replace
     * CTRL + L format code
     * CTRL + G: goto line
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (Dlog.DEBUG) Log.w(TAG, "onKeyDown: " + keyCode + " " + event);
        if (handleControlKey(keyCode, event, false)) {
            return true;
        }
        if (event.isCtrlPressed() || (event.isShiftPressed() && event.isAltPressed())
                || mKeyListener.mControlKey.isActive()) {
//            Log.i(TAG, "onKeyDown: process");
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                    selectAll();
                    return true;
                case KeyEvent.KEYCODE_X:
                    cut();
                    return true;
                case KeyEvent.KEYCODE_C:
                    copy();
                    return true;
                case KeyEvent.KEYCODE_V:
                    paste();
                    return true;
                case KeyEvent.KEYCODE_R: //run
                    if (editorControl != null)
                        editorControl.runProgram();
                    return true;
                case KeyEvent.KEYCODE_B: //build
                    if (editorControl != null)
                        editorControl.doCompile();
                    return true;
                case KeyEvent.KEYCODE_G: //go to line
                    if (editorControl != null)
                        editorControl.goToLine();
                    return true;
                case KeyEvent.KEYCODE_L: //format
                    if (editorControl != null)
                        editorControl.formatCode();
                    return true;
                case KeyEvent.KEYCODE_Z:
                    if (canUndo()) {
                        undo();
                    }
                    return true;
                case KeyEvent.KEYCODE_Y:
                    if (canRedo()) {
                        redo();
                    }
                    return true;
                case KeyEvent.KEYCODE_S:
                    if (editorControl != null)
                        editorControl.saveFile();
                    return true;
                case KeyEvent.KEYCODE_N:
                    if (editorControl != null)
                        editorControl.saveAs();
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
        } else {
            switch (keyCode) {
                case KeyEvent.KEYCODE_TAB:
                    String textToInsert = "\t";
                    int start, end;
                    start = Math.max(getSelectionStart(), 0);
                    end = Math.max(getSelectionEnd(), 0);
                    getText().replace(Math.min(start, end), Math.max(start, end),
                            textToInsert, 0, textToInsert.length());
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        if (DEBUG) Log.w(TAG, "onKeyUp " + event);
        if (handleControlKey(keyCode, event, false)) {
            return true;
        }
        if (event.isCtrlPressed() || mKeyListener.mControlKey.isActive()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_A:
                case KeyEvent.KEYCODE_X:
                case KeyEvent.KEYCODE_C:
                case KeyEvent.KEYCODE_V:
                case KeyEvent.KEYCODE_Z:
                case KeyEvent.KEYCODE_Y:
                case KeyEvent.KEYCODE_S:
                case KeyEvent.KEYCODE_R:
                case KeyEvent.KEYCODE_F:
                case KeyEvent.KEYCODE_L:
                    return true;
            }
        } else {
            switch (keyCode) {
                case KeyEvent.KEYCODE_TAB:
                    return true;
            }
        }
        return super.onKeyUp(keyCode, event);

    }

    public void cut() {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        selectionStart = Math.max(0, selectionStart);
        selectionEnd = Math.max(0, selectionEnd);
        selectionStart = Math.min(selectionStart, getText().length() - 1);
        selectionEnd = Math.min(selectionEnd, getText().length() - 1);
        try {
            mClipboardManager.setText(getText().subSequence(selectionStart, selectionEnd));
            getEditableText().delete(selectionStart, selectionEnd);
        } catch (Exception e) {
            FirebaseCrash.report(e);
        }
    }

    public void paste() {
        if (mClipboardManager.hasText()) {
            insert(mClipboardManager.getText().toString());
        }
    }

    /**
     * insert text
     */
    public void insert(CharSequence delta) {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        selectionStart = Math.max(0, selectionStart);
        selectionEnd = Math.max(0, selectionEnd);
        selectionStart = Math.min(selectionStart, selectionEnd);
        selectionEnd = Math.max(selectionStart, selectionEnd);
        try {

            getText().delete(selectionStart, selectionEnd);
            getText().insert(selectionStart, delta);
        } catch (Exception ignored) {
            FirebaseCrash.report(ignored);
        }
    }

    public void copy() {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        selectionStart = Math.max(0, selectionStart);
        selectionEnd = Math.max(0, selectionEnd);
        selectionStart = Math.min(selectionStart, getText().length() - 1);
        selectionEnd = Math.min(selectionEnd, getText().length() - 1);
        try {
            mClipboardManager.setText(getText().subSequence(selectionStart, selectionEnd));
        } catch (Exception ignored) {
            FirebaseCrash.report(ignored);
        }
    }

    public void setEditorControl(EditorControl editorControl) {
        this.editorControl = editorControl;
    }


    public void copyAll() {
        mClipboardManager.setText(getText());
    }
}
