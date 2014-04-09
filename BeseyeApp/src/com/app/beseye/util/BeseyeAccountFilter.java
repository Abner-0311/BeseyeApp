package com.app.beseye.util;

import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

public class BeseyeAccountFilter implements InputFilter{
	static final private Character D_QUOTE = Character.valueOf((char) 0x22);
	static final private Character QUOTE = Character.valueOf((char) 0x27);
	static final private Character SEMICOLON = Character.valueOf((char) 0x3B);
	
	static final private Character START = Character.valueOf((char) 0x21);
	static final private Character END = Character.valueOf((char) 0x7E);
	@Override
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend5) {
		
		 if (source instanceof SpannableStringBuilder) {
	        SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder)source;
            for (int i = end - 1; i >= start; i--) { 
                char currentChar = source.charAt(i);
                 if (START > currentChar || END < currentChar || D_QUOTE.equals(currentChar) || QUOTE.equals(currentChar) || SEMICOLON.equals(currentChar)) {    
                     sourceAsSpannableBuilder.delete(i, i+1);
                 }     
            }
            return source;
        } else {
            StringBuilder filteredStringBuilder = new StringBuilder();
            for (int i = 0; i < end; i++) { 
                char currentChar = source.charAt(i);
                if (START <= currentChar && END >= currentChar && !D_QUOTE.equals(currentChar) && !QUOTE.equals(currentChar) && !SEMICOLON.equals(currentChar)) {    
                    filteredStringBuilder.append(currentChar);
                }     
            }
            return filteredStringBuilder.toString();
        }
	}
}
