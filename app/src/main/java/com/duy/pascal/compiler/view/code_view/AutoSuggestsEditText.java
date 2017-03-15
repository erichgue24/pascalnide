package com.duy.pascal.compiler.view.code_view;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;

import com.duy.pascal.compiler.R;
import com.duy.pascal.compiler.data.KeyWordAndPattern;

import java.util.ArrayList;
import java.util.Collections;

/**
 * AutoSuggestsEditText
 * show hint when typing
 * Created by Duy on 28-Feb-17.
 */

public abstract class AutoSuggestsEditText extends android.support.v7.widget.AppCompatMultiAutoCompleteTextView implements View.OnClickListener {
    private static final String TAG = AutoSuggestsEditText.class.getName();
    public int mCharHeight = 0;
    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> list = new ArrayList<>();
    private Context context;

    public AutoSuggestsEditText(Context context) {
        super(context);
        init(context);
    }

    public AutoSuggestsEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);

    }

    public AutoSuggestsEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * slipt string in edittext and put it to list keyword
     */
    public void invalidateKeyWord(String source) {
//        Log.d(TAG, "invalidateKeyWord: " + source);
        list.clear();
        Collections.addAll(list, KeyWordAndPattern.LIST_KEY_WORD);
        String[] words = source.split("[^a-zA-Z']+");
        Collections.addAll(list, words);
//        Log.d(TAG, "invalidateKeyWord: " + list.toString());
        mAdapter = new ArrayAdapter<>(context, R.layout.code_hint, R.id.txt_title, list);
        setAdapter(mAdapter);
    }

    public void addKeyWord(String key) {
        list.add(key);
    }

    public void removeKeyWord(String key) {
        list.remove(key);
    }

    private void init(Context context) {
        this.context = context;
        invalidateKeyWord("");
        setTokenizer(new CodeTokenizer());
        setThreshold(1);
        mCharHeight = (int) Math.ceil(getPaint().getFontSpacing());
    }


    @Override
    public void onClick(View v) {

    }


    public class CodeTokenizer implements Tokenizer {
        String token = "!@#$%^&*()_+-={}|[]:'<>/<.?1234567890 \n\t";

        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && !token.contains(Character.toString(text.charAt(i - 1)))) {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }
            return i;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();

            while (i < len) {
                if (token.contains(Character.toString(text.charAt(i - 1)))) {
                    return i;
                } else {
                    i++;
                }
            }

            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();

            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }

            if (i > 0 && token.contains(Character.toString(text.charAt(i - 1)))) {
                return text;
            } else {
                if (text instanceof Spanned) {
                    SpannableString sp = new SpannableString(text);
                    TextUtils.copySpansFrom((Spanned) text, 0, text.length(), Object.class, sp, 0);
                    return sp;
                } else {
                    return text;
                }
            }
        }
    }


}
