/*
 *  Copyright (c) 2017 Tran Le Duy
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

package com.duy.pascal.frontend.view.editor_view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;

import com.duy.pascal.backend.core.PascalCompiler;
import com.duy.pascal.backend.exceptions.ParsingException;
import com.duy.pascal.backend.linenumber.LineInfo;
import com.duy.pascal.frontend.R;
import com.duy.pascal.frontend.theme.CodeThemeUtils;
import com.duy.pascal.frontend.theme.ThemeFromAssets;
import com.js.interpreter.core.ScriptSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.duy.pascal.frontend.data.PatternsUtils.comments;
import static com.duy.pascal.frontend.data.PatternsUtils.functions;
import static com.duy.pascal.frontend.data.PatternsUtils.keywords;
import static com.duy.pascal.frontend.data.PatternsUtils.numbers;
import static com.duy.pascal.frontend.data.PatternsUtils.strings;
import static com.duy.pascal.frontend.data.PatternsUtils.symbols;

public class HighlightEditor extends CodeSuggestsEditText
        implements View.OnKeyListener {
    public static final String TAG = HighlightEditor.class.getSimpleName();
    public static final int SHORT_DELAY = 500;
    public static final int LONG_DELAY = 1000;
    private static final int CHARS_TO_COLOR = 2500;

    private final Handler updateHandler = new Handler();
    private final Object objectThread = new Object();
    public boolean showlines = true;
    public float textSize = 13;
    public boolean wordWrap = true;
    //    public boolean flingToScroll = true;
    public LineInfo lineError = null;
    private final Runnable compileProgram = new Runnable() {
        @Override
        public void run() {
            try {
                new PascalCompiler(null).loadPascal("temp", new StringReader(getCleanText()),
                        new ArrayList<ScriptSource>(), new ArrayList<ScriptSource>(), null);
                lineError = null;
            } catch (ParsingException e) {
                if (e.line != null) {
                    synchronized (objectThread) {
                        lineError = e.line;
                    }
                }
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    protected Paint mPaintNumbers;
    protected Paint mPaintHighlight;
    protected int mPaddingDP = 4;
    protected int mPadding, mLinePadding;
    protected float mScale;
    protected int mHighlightedLine;
    protected int mHighlightStart;
    protected Rect mDrawingRect, mLineBounds;
    //Colors
    protected int COLOR_ERROR;
    protected int COLOR_NUMBER;
    protected int COLOR_KEYWORD;
    protected int COLOR_COMMENT;
    protected int COLOR_OPT;
    protected int COLOR_BOOLEANS;
    protected int COLOR_STRINGS;
    private boolean autoCompile = false;
    private Context mContext;
    private boolean modified = true;
    private boolean canEdit = true;
    @Nullable
    private ScrollView verticalScroll;
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            highlightWithoutChange(getText());
        }
    };
    private int lastPinLine = -1;
    private LineUtils lineUtils;
    private boolean[] isGoodLineArray;
    private int[] realLines;
    private int lineCount;

    public HighlightEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public HighlightEditor(Context context) {
        super(context);
        setup(context);
    }

    public HighlightEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);

    }

    public boolean isAutoCompile() {
        return autoCompile;
    }

    public void setAutoCompile(boolean autoCompile) {
        this.autoCompile = autoCompile;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    private void setup(Context context) {
        this.mContext = context;
        init();

        lineUtils = new LineUtils();
        mPaintNumbers = new Paint();
        mPaintNumbers.setColor(getResources().getColor(R.color.color_number_color));
        mPaintNumbers.setAntiAlias(true);

        mPaintHighlight = new Paint();

        mScale = context.getResources().getDisplayMetrics().density;
        mPadding = (int) (mPaddingDP * mScale);
        mHighlightedLine = mHighlightStart = -1;
        mDrawingRect = new Rect();
        mLineBounds = new Rect();

        updateFromSettings();
    }

    public void extendSelection(int index) {
        Selection.extendSelection(getText(), index);
    }

    @Override
    public void selectAll() {
        super.selectAll();
    }

    public void setTheme(int id) {
        ThemeFromAssets theme = ThemeFromAssets.getTheme(id, mContext);
        setBackgroundColor(theme.getBackground());
        setTextColor(theme.getColor(0));
        COLOR_ERROR = theme.getColor(7);
        COLOR_NUMBER = theme.getColor(1);
        COLOR_KEYWORD = theme.getColor(2);
        COLOR_COMMENT = theme.getColor(6);
        COLOR_STRINGS = theme.getColor(3);
        COLOR_BOOLEANS = theme.getColor(8);
        COLOR_OPT = theme.getColor(9);

        int style = CodeThemeUtils.getCodeTheme(mContext, "");
        TypedArray typedArray = mContext.obtainStyledAttributes(style,
                R.styleable.CodeTheme);
        this.canEdit = typedArray.getBoolean(R.styleable.CodeTheme_can_edit, true);
        typedArray.recycle();
//        setTypeface(FontManager.getInstance(mContext));
    }

    public void setTheme(String name) {
        /*
          load theme from xml
         */

        int style = CodeThemeUtils.getCodeTheme(mContext, name);
        TypedArray typedArray = mContext.obtainStyledAttributes(style,
                R.styleable.CodeTheme);
        typedArray.getInteger(R.styleable.CodeTheme_bg_editor_color,
                R.color.color_bg_editor_color);
        COLOR_ERROR = typedArray.getInteger(R.styleable.CodeTheme_error_color,
                R.color.color_error_color);
        COLOR_NUMBER = typedArray.getInteger(R.styleable.CodeTheme_number_color,
                R.color.color_number_color);
        COLOR_KEYWORD = typedArray.getInteger(R.styleable.CodeTheme_key_word_color,
                R.color.color_key_word_color);
        COLOR_COMMENT = typedArray.getInteger(R.styleable.CodeTheme_comment_color,
                R.color.color_comment_color);
        COLOR_STRINGS = typedArray.getInteger(R.styleable.CodeTheme_string_color,
                R.color.color_string_color);
        COLOR_BOOLEANS = typedArray.getInteger(R.styleable.CodeTheme_boolean_color,
                R.color.color_boolean_color);
        COLOR_OPT = typedArray.getInteger(R.styleable.CodeTheme_opt_color,
                R.color.color_opt_color);
        setBackgroundColor(typedArray.getInteger(R.styleable.CodeTheme_bg_editor_color,
                R.color.color_bg_editor_color));
        setTextColor(typedArray.getInteger(R.styleable.CodeTheme_normal_text_color,
                R.color.color_normal_text_color));

        this.canEdit = typedArray.getBoolean(R.styleable.CodeTheme_can_edit, true);
        typedArray.recycle();
    }

    public void setLineError(@NonNull LineInfo lineError) {
        this.lineError = lineError;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        int lineX, baseline;
        if (lineCount != getLineCount()) {
            lineCount = getLineCount();
            lineUtils.updateHasNewLineArray(lineCount, getLayout(), getText().toString());
            isGoodLineArray = lineUtils.getGoodLines();
            realLines = lineUtils.getRealLines();
        }
        if (showlines) {
            int padding = (int) (Math.floor(Math.log10(lineCount)) + 1);
            padding = (int) ((padding * mPaintNumbers.getTextSize())
                    + mPadding + (textSize * mScale * 0.5));
            if (mLinePadding != padding) {
                mLinePadding = padding;
                setPadding(mLinePadding, mPadding, mPadding, mPadding);
            }
        }

        getDrawingRect(mDrawingRect);
        lineX = (int) (mDrawingRect.left + mLinePadding - (textSize * mScale * 0.5));
        int min = 0;
        int max = lineCount;
        getLineBounds(0, mLineBounds);
        int startBottom = mLineBounds.bottom;
        int startTop = mLineBounds.top;
        getLineBounds(lineCount - 1, mLineBounds);
        int endBottom = mLineBounds.bottom;
        int endTop = mLineBounds.top;
        if (lineCount > 1 && endBottom > startBottom && endTop > startTop) {
            min = Math.max(min, ((mDrawingRect.top - startBottom) * (lineCount - 1)) / (endBottom - startBottom));
            max = Math.min(max, ((mDrawingRect.bottom - startTop) * (lineCount - 1)) / (endTop - startTop) + 1);
        }
        for (int i = min; i < max; i++) {
            baseline = getLineBounds(i, mLineBounds);
            if ((i == mHighlightedLine) && (!wordWrap)) {
                canvas.drawRect(mLineBounds, mPaintHighlight);
            }
            if (showlines && isGoodLineArray[i]) {
                int realLine = realLines[i];
                canvas.drawText("" + (realLine), mDrawingRect.left + mPadding, baseline, mPaintNumbers);
            }
            if (showlines) {
                canvas.drawLine(lineX, mDrawingRect.top, lineX, mDrawingRect.bottom, mPaintNumbers);
            }
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    public void updateFromSettings() {
        String name = mEditorSetting.getString(mContext.getString(R.string.key_code_theme));
        try {
            Integer id = Integer.parseInt(name);
            setTheme(id);
        } catch (Exception e) {
            setTheme(name);
        }
        setTypeface(mEditorSetting.getFont());
        setHorizontallyScrolling(!mEditorSetting.isWrapText());
        setTextSize(mEditorSetting.getTextSize());
        mPaintNumbers.setTextSize(getTextSize() * 0.85f);
        showlines = mEditorSetting.isShowLines();
        postInvalidate();
        refreshDrawableState();

//        if (flingToScroll) {
//            mMaxSize = new Point();
//        } else {
//            mMaxSize = null;
//        }

        mLinePadding = mPadding;
        int count = getLineCount();
        if (showlines) {
            mLinePadding = (int) (Math.floor(Math.log10(count)) + 1);
            mLinePadding = (int) ((mLinePadding * mPaintNumbers.getTextSize())
                    + mPadding /*+ (textSize * mScale * 0.5)*/);
            setPadding(mLinePadding, mPadding, mPadding, mPadding);
        } else {
            setPadding(mPadding, mPadding, mPadding, mPadding);
        }

        autoCompile = mEditorSetting.isAutoCompile();
    }

    @Override
    protected boolean getDefaultEditable() {
        return true;
    }

    @Override
    protected MovementMethod getDefaultMovementMethod() {
        return ArrowKeyMovementMethod.getInstance();
    }


    /**
     * This method used to set text and high light text
     *
     * @param text
     */
    public void setTextHighlighted(CharSequence text) {
        lineError = null;
        modified = false;
        setText(highlight(new SpannableStringBuilder(text), false));
        modified = true;
    }

    public void refresh() {
        updateHighlightWithDelay(20);
    }

    public void updateHighlightWithDelay(int delay) {
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.postDelayed(updateRunnable, delay);
    }

    public String getCleanText() {
        return getText().toString();
    }

    private void init() {
        addTextChangedListener(new TextWatcher() {
            int start = 0, end = 0;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                this.start = start;
                this.end = start + count;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                if (!modified || hasSelection())
                    return;
                if (!autoCompile) {
                    lineError = null;
                }
                applyTabWidth(e, start, end);
                updateHighlightWithDelay(LONG_DELAY);
                startCompile(200);
            }
        });
    }

    private void startCompile(int longDelay) {
        if (isAutoCompile()) {
            updateHandler.removeCallbacks(compileProgram);
            updateHandler.postDelayed(compileProgram, longDelay);
        }
    }


    private void highlightWithoutChange(Editable e) {
        modified = false;
        highlight(e, false);
        modified = true;
    }

    /**
     * Gets the first line that is visible on the screen.
     */
    public int getFirstLineIndex() {
        int scrollY = 0;
        if (verticalScroll != null) {
            scrollY = verticalScroll.getScrollY();
        } else {
            scrollY = getScrollY();
        }
        Layout layout = getLayout();
        if (layout != null) {
            return layout.getLineForVertical(scrollY);
        }
        return -1;
    }

    /**
     * Gets the last visible line number on the screen.
     *
     * @return last line that is visible on the screen.
     */
    public int getLastLineIndex() {
        int height = 0;
        if (verticalScroll != null) {
            height = verticalScroll.getHeight();
        } else {
            height = getHeight();
        }
        int scrollY = 0;
        if (verticalScroll != null) {
            scrollY = verticalScroll.getScrollY();
        } else {
            scrollY = getScrollY();
        }
        Layout layout = getLayout();
        if (layout != null) {
            return layout.getLineForVertical(scrollY + height);
        }
        return -1;
    }

    private Editable highlight(Editable editable, boolean all) {
        if (all) {
            return highlight(editable, 0, editable.length());
        } else {
            int editorHeight = getHeightVisible();
            int firstVisibleIndex;
            int lastVisibleIndex;
            if (verticalScroll != null && editorHeight > 0 && getLayout() != null) {
                Layout layout = getLayout();
                firstVisibleIndex = layout.getLineStart(getFirstLineIndex());
                lastVisibleIndex = layout.getLineEnd(getLastLineIndex());
            } else {
                firstVisibleIndex = 0;
                lastVisibleIndex = CHARS_TO_COLOR;
            }
            // normalize
            if (firstVisibleIndex < 0)
                firstVisibleIndex = 0;
            if (lastVisibleIndex > editable.length())
                lastVisibleIndex = editable.length();
            if (firstVisibleIndex > lastVisibleIndex)
                firstVisibleIndex = lastVisibleIndex;
            return highlight(editable, firstVisibleIndex, lastVisibleIndex);
        }
    }

    /**
     * high light text from start to end
     *
     * @param start - start index
     * @param end   - end index
     */
    private Editable highlight(Editable e, int start, int end) {
        try {
            //clear spannable
            clearSpans(e, start, end);

            CharSequence input = e.subSequence(start, end);

            //high light number
            for (Matcher m = numbers.matcher(input); m.find(); ) {
                e.setSpan(new ForegroundColorSpan(COLOR_NUMBER),
                        start + m.start(),
                        start + m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            for (Matcher m = keywords.matcher(input); m.find(); ) {
                e.setSpan(new ForegroundColorSpan(COLOR_KEYWORD),
                        start + m.start(),
                        start + m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            for (Matcher m = functions.matcher(input); m.find(); ) {
                e.setSpan(new ForegroundColorSpan(COLOR_KEYWORD),
                        start + m.start(),
                        start + m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            //find it
            for (Matcher m = symbols.matcher(input); m.find(); ) {
                //if match, you can replace text with other style
                e.setSpan(new ForegroundColorSpan(COLOR_OPT),
                        start + m.start(),
                        start + m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            }
            for (Matcher m = strings.matcher(input); m.find(); ) {
                ForegroundColorSpan spans[] = e.getSpans(start + m.start(), start + m.end(),
                        ForegroundColorSpan.class);

                for (int n = spans.length; n-- > 0; )
                    e.removeSpan(spans[n]);

                e.setSpan(new ForegroundColorSpan(COLOR_STRINGS),
                        start + m.start(),
                        start + m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            for (Matcher m = comments.matcher(input); m.find(); ) {
                ForegroundColorSpan spans[] = e.getSpans(start + m.start(), start + m.end(),
                        ForegroundColorSpan.class);
                for (int n = spans.length; n-- > 0; )
                    e.removeSpan(spans[n]);

                e.setSpan(new ForegroundColorSpan(COLOR_COMMENT),
                        start + m.start(),
                        start + m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            highlightLineError(e);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return e;
    }

    private void highlightLineError(Editable e) {
        try {
            //high light error line
            if (lineError != null) {
                Layout layout = getLayout();
                int line = lineError.line;
                int temp = line;
                while (realLines[temp] < line) temp++;
                line = temp;
                if (layout != null && line < getLineCount()) {
                    int lineStart = getLayout().getLineStart(line);
                    int lineEnd = getLayout().getLineEnd(line);
                    lineStart += lineError.column;
                    if (lineStart < lineEnd) {
                        e.setSpan(new BackgroundColorSpan(COLOR_ERROR),
                                lineStart,
                                lineEnd,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (!isAutoCompile()) {
                            lineStart = Math.max(0, lineStart);
                            setSelection(lineStart);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * remove span from start to end
     */
    private void clearSpans(Editable e, int start, int end) {
        {
            ForegroundColorSpan spans[] = e.getSpans(start, end, ForegroundColorSpan.class);
            for (int n = spans.length; n-- > 0; )
                e.removeSpan(spans[n]);
        }
        {
            BackgroundColorSpan spans[] = e.getSpans(start, end, BackgroundColorSpan.class);
            for (int n = spans.length; n-- > 0; )
                e.removeSpan(spans[n]);
        }
//        {
//            StyleSpan[] spans = e.getSpans(start, end, StyleSpan.class);
//            for (int n = spans.length; n-- > 0; )
//                e.removeSpan(spans[n]);
//        }
        {
            UnderlineSpan[] spans = e.getSpans(start, end, UnderlineSpan.class);
            for (int n = spans.length; n-- > 0; )
                e.removeSpan(spans[n]);
        }
    }

    public void replaceAll(String what, String replace, boolean regex, boolean matchCase) {
        Pattern pattern;
        if (regex) {
            if (matchCase) {
                pattern = Pattern.compile(what);
            } else {
                pattern = Pattern.compile(what, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            }
        } else {
            if (matchCase) {
                pattern = Pattern.compile(Pattern.quote(what));
            } else {
                pattern = Pattern.compile(Pattern.quote(what), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            }
        }
        String clone = getText().toString();
        //replace with white space
//        Matcher m = pattern.matcher(clone);
        setText(getText().toString().replaceAll(pattern.toString(), replace));
    }

    /**
     * move cursor to line
     *
     * @param line - line in editor, start at 0
     */
    public void goToLine(int line) {
        Layout layout = getLayout();
        line = Math.min(line - 1, getLineCount() - 1);
        line = Math.max(0, line);
        if (layout != null) {
            int index = layout.getLineEnd(line);
            setSelection(index);
        }
    }

    @Override
    public void onPopupChangePosition() {
        try {
            Layout layout = getLayout();
            if (layout != null) {
                int pos = getSelectionStart();
                int line = layout.getLineForOffset(pos);
                int baseline = layout.getLineBaseline(line);
                int ascent = layout.getLineAscent(line);

                float x = layout.getPrimaryHorizontal(pos);
                float y = baseline + ascent;

                int offsetHorizontal = (int) x + mLinePadding;
                setDropDownHorizontalOffset(offsetHorizontal);

                int heightVisible = getHeightVisible();
                int offsetVertical = 0;
                if (verticalScroll != null) {
                    offsetVertical = (int) ((y + mCharHeight) - verticalScroll.getScrollY());
                } else {
                    offsetVertical = (int) ((y + mCharHeight) - getScrollY());
                }

                int tmp = offsetVertical + getDropDownHeight() + mCharHeight;
                if (tmp < heightVisible) {
                    tmp = offsetVertical + mCharHeight / 2;
                    setDropDownVerticalOffset(tmp);
                } else {
                    tmp = offsetVertical - getDropDownHeight() - mCharHeight;
                    setDropDownVerticalOffset(tmp);
                }
            }
        } catch (Exception ignored) {
        }
    }


    public void setVerticalScroll(@Nullable ScrollView verticalScroll) {
        this.verticalScroll = verticalScroll;
    }

    /**
     * highlight find word
     *
     * @param what     - input
     * @param regex    - is java regex
     * @param wordOnly - find word only
     */
    public void find(String what, boolean regex, boolean wordOnly, boolean matchCase) {
        Pattern pattern;
        if (regex) {
            if (matchCase) {
                pattern = Pattern.compile(what);
            } else {
                pattern = Pattern.compile(what, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            }
        } else {
            if (wordOnly) {
                if (matchCase) {
                    pattern = Pattern.compile("\\s" + what + "\\s");
                } else {
                    pattern = Pattern.compile("\\s" + Pattern.quote(what) + "\\s", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                }
            } else {
                if (matchCase) {
                    pattern = Pattern.compile(Pattern.quote(what));
                } else {
                    pattern = Pattern.compile(Pattern.quote(what), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                }
            }
        }
        Editable e = getEditableText();
        //remove all span
        BackgroundColorSpan spans[] = e.getSpans(0, e.length(), BackgroundColorSpan.class);
        for (int n = spans.length; n-- > 0; )
            e.removeSpan(spans[n]);
        //set span

        for (Matcher m = pattern.matcher(e); m.find(); ) {
            e.setSpan(new BackgroundColorSpan(COLOR_ERROR),
                    m.start(),
                    m.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void pinLine(LineInfo lineInfo) {
        if (lineInfo == null) return;
        Layout layout = getLayout();
        Editable e = getEditableText();
        if (layout != null && lineInfo.line < getLineCount()) {
            try {
                if (lastPinLine < getLineCount() && lastPinLine >= 0) {
                    int lineStart = getLayout().getLineStart(lastPinLine);
                    int lineEnd = getLayout().getLineEnd(lastPinLine);
                    BackgroundColorSpan[] backgroundColorSpan = e.getSpans(lineStart, lineEnd,
                            BackgroundColorSpan.class);
                    for (BackgroundColorSpan colorSpan : backgroundColorSpan) {
                        e.removeSpan(colorSpan);
                    }
                }

                int lineStart = getLayout().getLineStart(lineInfo.line);
                int lineEnd = getLayout().getLineEnd(lineInfo.line);
                lineStart = lineStart + lineInfo.column;
                if (lineStart < lineEnd) {
                    e.setSpan(new BackgroundColorSpan(COLOR_ERROR),
                            lineStart,
                            lineEnd,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                lastPinLine = lineInfo.line;
            } catch (Exception ignored) {
            }
        }
    }



    public interface OnTextChangedListener {
        void onTextChanged(String text);
    }

}