/*

 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ex.chips;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.text.style.ImageSpan;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.ex.chips.chip.ChipsUtil;
import com.android.ex.chips.chip.DrawableRecipientChip;
import com.android.ex.chips.chip.VisibleRecipientChip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * RecipientEditTextView is an auto complete text view for use with applications that use the new Chips UI for
 * addressing a message to recipients.
 */
public class CustomRecipientEditTextView extends MultiAutoCompleteTextView implements Callback, TextView.OnEditorActionListener {
	/**
	 * max pixels till touching up/down out of the initial touch coordinate is considered as scrolling
	 */
	private static final int SCROLLING_BOUNDS_THRESHOLD_IN_PIXELS = 2;

	private static final char COMMIT_CHAR_COMMA = ',';
	private static final char COMMIT_CHAR_SEMICOLON = ';';
	private static final char COMMIT_CHAR_SPACE = ' ';
	private static final String SEPARATOR = String.valueOf(COMMIT_CHAR_COMMA) + String.valueOf(COMMIT_CHAR_SPACE);
	private static final String TAG = "CustomRecipientEditText";
	private static final int DISMISS = "dismiss".hashCode();
	private static final long DISMISS_DELAY = 300;
	private static int sSelectedTextColor = -1;
	private static int sExcessTopPadding = -1;
	// VisibleForTesting
	private final Handler mHandler;
	// Resources for displaying chips.
	private Drawable mChipBackground = null;
	private Drawable mInvalidChipBackground;
	private Drawable mChipBackgroundPressed;
	private float mChipHeight;
	private float mChipFontSize;
	private float mLineSpacingExtra;
	private int mChipPadding;
	private int mChipMaxLines;
	private Tokenizer mTokenizer;
	private Validator mValidator;
	private DrawableRecipientChip mSelectedChip;
	private int mAlternatesLayout;
	//    private Bitmap mDefaultContactPhoto;
	private ImageSpan mMoreChip;
	private TextView mMoreItem;
	private boolean mNoChips = false;
	private ListPopupWindow mAddressPopup;
	private TextWatcher mTextWatcher;
	/**
	 * Obtain the enclosing scroll view, if it exists, so that the view can be scrolled to show the last line of chips
	 * content.
	 */
	private ScrollView mScrollView;
	private boolean mTriedGettingScrollView;
	private int mMaxLines;
	private int mActionBarHeight;
	private boolean mAttachedToWindow;
	private IChipListener mChipListener;
	private int mPreviousChipsCount = 0;
	/**
	 * used to store initial touch Y coordinate, in order to identify scrolling
	 */
	//    private int mStartTouchY = -1;
	//    private boolean mIsScrolling = false;
	private ListView mListView;
	private int mDropDownHeight;


	private static float getTextYOffset(final String text, final TextPaint paint, final int height) {
		final Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		final int textHeight = bounds.bottom - bounds.top;
		return height - (height - textHeight) / 2;// - (int) paint.descent();
	}


	private static String tokenizeAddress(final String destination) {
		final Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(destination);
		if(tokens != null && tokens.length > 0)
			return tokens[0].getAddress();
		return destination;
	}


	private static int findText(final Editable text, final int offset) {
		if(text.charAt(offset) != ' ')
			return offset;
		return -1;
	}


	public CustomRecipientEditTextView(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		setChipDimensions(context, attrs);
		if(sSelectedTextColor == -1)
			sSelectedTextColor = context.getResources().getColor(android.R.color.white);
		mAddressPopup = new ListPopupWindow(context);
		mDropDownHeight = getDropDownHeight();
		setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		setOnItemClickListener(null);
		setOnFocusChangeListener(null);
		setCustomSelectionActionModeCallback(this);
		mHandler = new Handler() {
			@Override
			public void handleMessage(final Message msg) {
				if(msg.what == DISMISS) {
					((ListPopupWindow) msg.obj).dismiss();
					clearSelectedChip();
					return;
				}
				super.handleMessage(msg);
			}
		};
		mTextWatcher = new RecipientTextWatcher();
		mTokenizer = new CustomRecipientEditTextView.CustomCommaTokenizer();
		addTextChangedListener(mTextWatcher);
		setOnEditorActionListener(this);
	}


	public void setListView(ListView listView) {
		mListView = listView;
		if(listView != null) {
			mListView.setOnItemClickListener(getOnItemClickListener());
			setDropDownHeight(0);
		} else {
			setDropDownHeight(mDropDownHeight);
		}
	}


	@Override
	public void setOnItemClickListener(final OnItemClickListener listener) {
		super.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				/**
				 * When an item in the suggestions list has been clicked, create a chip from the contact information of
				 * the selected item.
				 */
				int p = position;
				if(mListView != null) {
					p -= mListView.getHeaderViewsCount();
				}
				if(p < 0)
					return;
				getAdapter().toggleSelected(p);
				if(getAdapter().isSelected(p)) {
					submitItemAtPosition(p, true);
				} else {
					removeRecipient(getAdapter().getItem(p), true);
				}
				if(listener != null)
					listener.onItemClick(parent, view, p, id);
			}
		});
	}


	@Override
	public void setOnFocusChangeListener(final OnFocusChangeListener listener) {
		super.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus) {
					clearSelectedChip();
				}

				if(listener != null) {
					listener.onFocusChange(v, hasFocus);
				}
			}
		});
	}


	@Override
	public boolean onEditorAction(final TextView view, final int action, final KeyEvent keyEvent) {
		if(action == EditorInfo.IME_ACTION_DONE) {
			if(mSelectedChip != null) {
				clearSelectedChip();
				return true;
			} else if(focusNext())
				return true;
		}
		return false;
	}


	@Override
	public InputConnection onCreateInputConnection(final EditorInfo outAttrs) {
		final InputConnection connection = super.onCreateInputConnection(outAttrs);
		final int imeActions = outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION;
		if((imeActions & EditorInfo.IME_ACTION_DONE) != 0) {
			// clear the existing action
			outAttrs.imeOptions ^= imeActions;
			// set the DONE action
			outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
		}
		if((outAttrs.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0)
			outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
		outAttrs.actionId = EditorInfo.IME_ACTION_DONE;
		outAttrs.actionLabel = getContext().getString(R.string.done);
		return connection;
	}


	@Override
	public void onSelectionChanged(final int start, final int end) {
		// When selection changes, see if it is inside the chips area.
		// If so, move the cursor back after the chips again.
		final DrawableRecipientChip last = getLastChip();
		if(last != null && start < getSpannable().getSpanEnd(last))
			// Grab the last chip and set the cursor to after it.
			setSelection(Math.min(getSpannable().getSpanEnd(last) + 1, getText().length()));
		super.onSelectionChanged(start, end);
	}


	@Override
	public void onRestoreInstanceState(final Parcelable state) {
		if(!TextUtils.isEmpty(getText()))
			super.onRestoreInstanceState(null);
		else super.onRestoreInstanceState(state);
	}


	@Override
	public Parcelable onSaveInstanceState() {
		// If the user changes orientation while they are editing, just roll back the selection.
		clearSelectedChip();
		return super.onSaveInstanceState();
	}


	@Override
	public void performValidation() {
		// Do nothing. Chips handles its own validation.
	}


	@Override
	public void onSizeChanged(final int width, final int height, final int oldw, final int oldh) {
		super.onSizeChanged(width, height, oldw, oldh);
		if(width != 0 && height != 0)
			checkChipWidths();
		// Try to find the scroll view parent, if it exists.
		if(mScrollView == null && !mTriedGettingScrollView) {
			ViewParent parent = getParent();
			while(parent != null && !(parent instanceof ScrollView))
				parent = parent.getParent();
			if(parent != null) {
				mScrollView = (ScrollView) parent;
				mScrollView.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if(hasFocus()) {
							clearFocus();
						}
						return false;
					}
				});
			}
			mTriedGettingScrollView = true;
		} else if(mScrollView != null && mChipMaxLines != 0) {
			mScrollView.getLayoutParams().height = height >= mChipMaxLines * getActualChipHeight() ? (mChipMaxLines * getActualChipHeight()) +
					getPaddingBottom() + getPaddingTop() : height;
		}
	}


	@Override
	public void setTokenizer(final Tokenizer tokenizer) {
		if(tokenizer instanceof MultiAutoCompleteTextView.CommaTokenizer) {
			throw new RuntimeException("Instead of using MultiAutoCompleteTextView.CommaTokenizer use CustomRecipientEditTextView.CustomCommaTokenizer");
		} else {
			mTokenizer = tokenizer;
		}
		super.setTokenizer(mTokenizer);
	}


	@Override
	public void setValidator(final Validator validator) {
		mValidator = validator;
		super.setValidator(validator);
	}


	/**
	 * Dismiss any selected chips when the back key is pressed.
	 */
	@Override
	public boolean onKeyPreIme(final int keyCode, final KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && mSelectedChip != null) {
			clearSelectedChip();
			return true;
		}
		return super.onKeyPreIme(keyCode, event);
	}


	/**
	 * If there is a selected chip, delegate the key events to the selected chip.
	 */
	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if(mSelectedChip != null && keyCode == KeyEvent.KEYCODE_DEL) {
			removeChip(mSelectedChip, true, true);
		}
		switch(keyCode) {
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if(event.hasNoModifiers()) {
					if(mSelectedChip != null) {
						clearSelectedChip();
						return true;
					} else if(focusNext())
						return true;
				}
				break;
		}
		return super.onKeyDown(keyCode, event);
	}


	/**
	 * Monitor touch events in the RecipientEditTextView. If the view does not have focus, any tap on the view will just
	 * focus the view. If the view has focus, determine if the touch target is a recipient chip. If it is and the chip
	 * is not selected, select it and clear any other selected chips. If it isn't, then select that chip.
	 */
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		if(mAddressPopup != null && mAddressPopup.isShowing()) {
			mAddressPopup.dismiss();
			clearSelectedChip();
		}
		//        if (!isFocused())
		//            // Ignore any chip taps until this view is focused.
		//            return super.onTouchEvent(event);
		//        android.util.Log.d("AppLog", "scroll:" + getScrollY());
		boolean handled = super.onTouchEvent(event);
		final int action = event.getAction();
		boolean chipWasSelected = false;
		switch(action) {
			case MotionEvent.ACTION_DOWN:
				//                mIsScrolling = false;
				//                mStartTouchY = (int) event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				//                if (Math.abs(event.getY() - mStartTouchY) > SCROLLING_BOUNDS_THRESHOLD_IN_PIXELS)
				//                {
				//                    mIsScrolling = true;
				//                    clearSelectedChip();
				//                }
				break;
			case MotionEvent.ACTION_UP:
				//                if (mIsScrolling)
				//                {
				//                    mIsScrolling = false;
				//                    return true;
				//                }
				final float x = event.getX();
				final float y = event.getY();
				final int offset = putOffsetInRange(x, y);
				final DrawableRecipientChip currentChip = findChip(offset);
				if(currentChip != null) {
					if(mSelectedChip != null && mSelectedChip != currentChip) {
						clearSelectedChip();
						setFocusableInTouchMode(false);
						mSelectedChip = selectChip(currentChip);
					} else if(mSelectedChip == null) {
						setSelection(getText().length());
						setFocusableInTouchMode(false);
						mSelectedChip = selectChip(currentChip);
					} else onClick(mSelectedChip, offset, x, y);
					chipWasSelected = true;
					handled = true;
				} else {
					if(!hasFocus()) {
						setFocusableInTouchMode(true);
						setFocusable(true);
						postDelayed(new Runnable() {
							@Override
							public void run() {
								requestFocus();
							}
						}, 100);
					}
				}
				if(!chipWasSelected)
					clearSelectedChip();
				break;
		}
		return handled;
	}


	@Override
	public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
		return false;
	}


	@Override
	public void onDestroyActionMode(final ActionMode mode) {
	}


	//    private Bitmap createUnselectedChip(final IRecipientEntry contact, final TextPaint paint, final boolean leaveBlankIconSpacer)
	//    {
	//        // Ellipsize the text so that it takes AT MOST the entire width of the
	//        // autocomplete text entry area. Make sure to leave space for padding
	//        // on the sides.
	//        final int height = (int) mChipHeight;
	//        int iconWidth = height;
	//        final float[] widths = new float[1];
	//        paint.getTextWidths(" ", widths);
	//        final float availableWidth = calculateAvailableWidth();
	//        final String chipDisplayText = createChipDisplayText(contact);
	//        final CharSequence ellipsizedText = ellipsizeText(chipDisplayText, paint, availableWidth - iconWidth - widths[0]);
	//        // Make sure there is a minimum chip width so the user can ALWAYS
	//        // tap a chip without difficulty.
	//        final int width = Math.max(iconWidth * 2, (int) Math.floor(paint.measureText(ellipsizedText, 0, ellipsizedText.length())) + mChipPadding * 2 + iconWidth);
	//        // Create the background of the chip.
	//        final Bitmap tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	//        final Canvas canvas = new Canvas(tmpBitmap);
	//        final Drawable background = getChipBackground(contact);
	//        if (background != null)
	//        {
	//            background.setBounds(0, 0, width, height);
	//            background.draw(canvas);
	//            if (contact.drawPhotos())
	//            {
	//                byte[] photoBytes = contact.getPhotoBytes();
	//                Bitmap photo;
	//                if (photoBytes != null){
	//                    photo = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
	//                }
	//                // TODO: can the scaled down default photo be cached?
	//                else {
	//                    photo = mDefaultContactPhoto;
	//                }
	//
	//                // Draw the photo on the left side.
	//                if (photo != null)
	//                {
	//                    final RectF src = new RectF(0, 0, photo.getWidth(), photo.getHeight());
	//                    final Rect backgroundPadding = new Rect();
	//                    mChipBackground.getPadding(backgroundPadding);
	////                    final RectF dst = new RectF(backgroundPadding.left, 0 + backgroundPadding.top, width - backgroundPadding.right, height - backgroundPadding.bottom);
	//                    final RectF dst = new RectF(0, 0, height, height);
	//                    final Matrix matrix = new Matrix();
	//                    matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
	//                    canvas.drawBitmap(photo, matrix, paint);
	//                }
	//            }
	//            else if (!leaveBlankIconSpacer){
	//                iconWidth = 0;
	//            }
	//
	//            paint.setColor(getContext().getResources().getColor(android.R.color.black));
	//            // Vertically center the text in the chip.
	//            canvas.drawText(ellipsizedText, 0, ellipsizedText.length(), mChipPadding, getTextYOffset((String) ellipsizedText, paint, height), paint);
	//        }
	//        else Log.w(TAG, "Unable to draw a background for the chips as it was never set");
	//        return tmpBitmap;
	//    }


	@Override
	public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
		return false;
	}


	/**
	 * No chips are selectable.
	 */
	@Override
	public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
		return false;
	}


	/**
	 * Handle click events for a chip. When a selected chip receives a click event, see if that event was in the delete
	 * icon. If so, delete it. Otherwise, unselect the chip.
	 */
	public void onClick(final DrawableRecipientChip chip, final int offset, final float x, final float y) {
		clearSelectedChip();
	}


	@Override
	public void removeTextChangedListener(final TextWatcher watcher) {
		mTextWatcher = null;
		super.removeTextChangedListener(watcher);
	}


	@Override
	public SelectableArrayAdapter getAdapter() {
		return (SelectableArrayAdapter) super.getAdapter();
	}


	@Override
	public <T extends ListAdapter & Filterable> void setAdapter(@NonNull final T adapter) {
		super.setAdapter(adapter);
		if(!(adapter instanceof SelectableArrayAdapter)) {
			throw new RuntimeException("Adapter has to be instace of " + SelectableArrayAdapter.class.getSimpleName());
		}
		SelectableArrayAdapter selectableArrayAdapter = (SelectableArrayAdapter) adapter;
		selectableArrayAdapter.registerSelectedDataChangeListener(new SelectableArrayAdapter.SelectDataChangeListener() {

			@Override
			public void selectAll(boolean selected) {
				Collection<Long> dataIds = getDataIds();
				if(selected) {
					for(int i = 0; i < getAdapter().getSelectedObjects().size(); i++) {
						if(!dataIds.contains(getAdapter().getItem(i).getDataId()))
							submitItemAtPosition(i, false);
					}
				} else {
					if(getAdapter().getCount() == getAdapter().getObjects().size()) {
						removeAllRecipients(false, true);
					} else {
						for(int i = 0; i < getAdapter().getCount(); i++) {
							removeChipById(getAdapter().getItem(i).getDataId(), false);
						}
					}
				}

				if(mChipListener != null) {
					mChipListener.onDataChanged();
				}
			}


			@Override
			public void dataChanged() {
				if(mChipListener != null) {
					mChipListener.onDataChanged();
				}
			}
		});
	}


	// ////////////////////////////////////////////////added functionality///////////////////////////////////////
	public void setChipListener(final IChipListener chipListener) {
		mChipListener = chipListener;
	}


	/**
	 * adds a recipient to the view. note that it should be called when the view has determined its size
	 */
	public void addRecipient(final IRecipientEntry entry, final boolean alsoNotifyAboutDataChanges) {
		if(entry == null)
			return;
		// clearComposingText();
		final Editable editable = getText();
		// QwertyKeyListener.markAsReplaced(editable, start, end, "");
		final CharSequence chip = createChip(entry, false);
		if(!alsoNotifyAboutDataChanges)
			++mPreviousChipsCount;
		// expand();
		if(chip != null)
			editable.append(chip);
		sanitizeBetween();
		// shrink();
	}


	/**
	 * removes a chip of a recipient from the view
	 */
	public void removeRecipient(final IRecipientEntry entry, final boolean alsoNotifyAboutDataChanges) {
		if(entry == null)
			return;
		removeChipById(entry.getDataId(), alsoNotifyAboutDataChanges);
	}


	public void removeAllRecipients(boolean callbackEnabled, final boolean alsoNotifyAboutDataChanges) {
		final DrawableRecipientChip[] chips = getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
		for(final DrawableRecipientChip chip : chips)
			removeChip(chip, alsoNotifyAboutDataChanges, callbackEnabled);
	}


	/**
	 * returns a collection of all of the chips' items. key is the contact id, and the value is the recipient itself
	 */
	public Set<IRecipientEntry> getChosenRecipients() {
		final Set<IRecipientEntry> result = new HashSet<IRecipientEntry>();
		final DrawableRecipientChip[] chips = getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
		for(final DrawableRecipientChip chip : chips) {
			final IRecipientEntry recipientEntry = chip.getEntry();
			result.add(recipientEntry);
		}
		return result;
	}


	/**
	 * sets exact recipients to be chosen. <br/>
	 * each previous chip will stay only if it really matches one of the recipients. <br/>
	 * note that this function will not notify about data changes
	 */
	public void setChosenRecipients(final Set<IRecipientEntry> recipientsToSet) {
		final Set<IRecipientEntry> currentRecipients = getChosenRecipients();
		final List<IRecipientEntry> toRemove = new ArrayList<>(), toAdd = new ArrayList<IRecipientEntry>();
		// check which to remove - items that exist on the editText but not on the list
		for(final IRecipientEntry recipientEntry : currentRecipients)
			if(!recipientsToSet.contains(recipientEntry))
				toRemove.add(recipientEntry);
		// check which to add - items that exist on the list, but not on the editText
		for(final IRecipientEntry recipientEntry : recipientsToSet)
			if(!currentRecipients.contains(recipientEntry))
				toAdd.add(recipientEntry);
		for(final IRecipientEntry recipientEntry : toRemove)
			removeRecipient(recipientEntry, true);
		for(final IRecipientEntry recipientEntry : toAdd)
			addRecipient(recipientEntry, true);
	}


	@Override
	protected void onDetachedFromWindow() {
		mAttachedToWindow = false;
		super.onDetachedFromWindow();
	}


	@Override
	protected void onAttachedToWindow() {
		mAttachedToWindow = true;
	}


	protected void drawIconOnCanvas(Bitmap icon, Canvas canvas, Paint paint, RectF src, RectF dst) {
		Matrix matrix = new Matrix();
		matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
		canvas.drawBitmap(icon, matrix, paint);
	}


	/**
	 * We cannot use the default mechanism for replaceText. Instead, we override onItemClickListener so we can get all
	 * the associated contact information including display text, address, and id.
	 */
	@Override
	protected void replaceText(final CharSequence text) {
		return;
	}


	/**
	 * Instead of filtering on the entire contents of the edit box, this subclass method filters on the range from {@link Tokenizer#findTokenStart} to {@link #getSelectionEnd} if the length of that range meets or exceeds {@link #getThreshold} and makes sure that the range is not already a Chip.
	 */
	@Override
	protected void performFiltering(final CharSequence text, final int keyCode) {
		final boolean isCompletedToken = isCompletedToken(text);
		if(enoughToFilter() && !isCompletedToken) {
			final int end = getSelectionEnd();
			final int start = mTokenizer.findTokenStart(text, end);
			// If this is a RecipientChip, don't filter
			// on its contents.
			final Spannable span = getSpannable();
			final DrawableRecipientChip[] chips = span.getSpans(start, end, DrawableRecipientChip.class);
			if(chips != null && chips.length > 0)
				return;
		} else if(isCompletedToken)
			return;
		super.performFiltering(text, keyCode);
	}


	/* package */DrawableRecipientChip getLastChip() {
		DrawableRecipientChip last = null;
		final DrawableRecipientChip[] chips = getSortedRecipients();
		if(chips != null && chips.length > 0)
			last = chips[chips.length - 1];
		return last;
	}


	/**
	 * Get the background drawable for a RecipientChip.
	 */
	// Visible for testing.
  /* package */Drawable getChipBackground(final IRecipientEntry contact) {
		return contact.isValid() ? mChipBackground : mInvalidChipBackground;
	}


	// Visible for testing.
  /* package */void setChipBackground(final Drawable chipBackground) {
		mChipBackground = chipBackground;
	}


	// Visible for testing.
  /* package */void setChipHeight(final int height) {
		mChipHeight = height;
	}


	// Visible for testing.
  /* package */void sanitizeBetween() {
		// Find the last chip.
		final DrawableRecipientChip[] recips = getSortedRecipients();
		if(recips != null && recips.length > 0) {
			final DrawableRecipientChip last = recips[recips.length - 1];
			DrawableRecipientChip beforeLast = null;
			if(recips.length > 1)
				beforeLast = recips[recips.length - 2];
			int startLooking = 0;
			final int end = getSpannable().getSpanStart(last);
			if(beforeLast != null) {
				startLooking = getSpannable().getSpanEnd(beforeLast);
				final Editable text = getText();
				if(startLooking == -1 || startLooking > text.length() - 1)
					// There is nothing after this chip.
					return;
				if(text.charAt(startLooking) == ' ')
					startLooking++;
			}
			if(startLooking >= 0 && end >= 0 && startLooking < end)
				getText().delete(startLooking, end);
		}
	}


	// Visible for testing.
  /* package */Spannable getSpannable() {
		return getText();
	}


	// Visible for testing.
  /* package */boolean isCompletedToken(final CharSequence text) {
		if(TextUtils.isEmpty(text))
			return false;
		// Check to see if this is a completed token before filtering.
		final int end = text.length();
		final int start = mTokenizer.findTokenStart(text, end);
		final String token = text.toString().substring(start, end).trim();
		if(!TextUtils.isEmpty(token)) {
			final char atEnd = token.charAt(token.length() - 1);
			return atEnd == COMMIT_CHAR_COMMA || atEnd == COMMIT_CHAR_SEMICOLON;
		}
		return false;
	}


	// Visible for testing.
	// Use this method to generate text to add to the list of addresses.
  /* package */String createAddressText(final IRecipientEntry entry) {
		String display = entry.getDisplayName();
		String address = entry.getDestination();
		if(TextUtils.isEmpty(display) || TextUtils.equals(display, address))
			display = null;
		String trimmedDisplayText;
		if(address != null) {
			// Tokenize out the address in case the address already
			// contained the username as well.
			final Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(address);
			if(tokenized != null && tokenized.length > 0)
				address = tokenized[0].getAddress();
		}
		final Rfc822Token token = new Rfc822Token(display, address, null);
		trimmedDisplayText = token.toString().trim();
		final int index = trimmedDisplayText.indexOf(",");
		return mTokenizer != null && !TextUtils.isEmpty(trimmedDisplayText) && index < trimmedDisplayText.length() - 1 ? (String) mTokenizer.terminateToken(trimmedDisplayText) : trimmedDisplayText;
	}


	// Visible for testing.
	// Use this method to generate text to display in a chip.
  /* package */String createChipDisplayText(final IRecipientEntry entry) {
		String display = entry.getDisplayName();
		final String address = entry.getDestination();
		if(TextUtils.isEmpty(display) || TextUtils.equals(display, address))
			display = null;
		if(!TextUtils.isEmpty(display))
			return display;
		else if(!TextUtils.isEmpty(address))
			return address;
		else return new Rfc822Token(display, address, null).toString();
	}


	/**
	 * Returns a collection of data Id for each chip inside this View. May be null.
	 */
  /* package */Collection<Long> getDataIds() {
		final Set<Long> result = new HashSet<Long>();
		final DrawableRecipientChip[] chips = getSortedRecipients();
		if(chips != null)
			for(final DrawableRecipientChip chip : chips)
				result.add(chip.getDataId());
		return result;
	}


	// Visible for testing.
  /* package */DrawableRecipientChip[] getSortedRecipients() {
		final DrawableRecipientChip[] recips = getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
		final ArrayList<DrawableRecipientChip> recipientsList = new ArrayList<DrawableRecipientChip>(Arrays.asList(recips));
		final Spannable spannable = getSpannable();
		Collections.sort(recipientsList, new Comparator<DrawableRecipientChip>() {
			@Override
			public int compare(final DrawableRecipientChip first, final DrawableRecipientChip second) {
				final int firstStart = spannable.getSpanStart(first);
				final int secondStart = spannable.getSpanStart(second);
				if(firstStart < secondStart)
					return -1;
				else if(firstStart > secondStart)
					return 1;
				else return 0;
			}
		});
		return recipientsList.toArray(new DrawableRecipientChip[recipientsList.size()]);
	}


	// Visible for testing.
  /* package */int countTokens(final Editable text) {
		int tokenCount = 0;
		int start = 0;
		while(start < text.length()) {
			start = movePastTerminators(mTokenizer.findTokenEnd(text, start));
			tokenCount++;
			if(start >= text.length())
				break;
		}
		return tokenCount;
	}


	/**
	 * Remove the chip and any text associated with it from the RecipientEditTextView.
	 *
	 * @param alsoNotifyAboutDataChanges
	 * @param callbackEnabled
	 */
	// Visible for testing.
  /* package */void removeChip(final DrawableRecipientChip chip, final boolean alsoNotifyAboutDataChanges, boolean callbackEnabled) {
		if(!alsoNotifyAboutDataChanges)
			--mPreviousChipsCount;
		deleselectAdapterItem(chip);
		final Spannable spannable = getSpannable();
		final int spanStart = spannable.getSpanStart(chip);
		final int spanEnd = spannable.getSpanEnd(chip);
		final Editable text = getText();
		int toDelete = spanEnd;
		final boolean wasSelected = chip == mSelectedChip;
		// Clear that there is a selected chip before updating any text.
		if(wasSelected)
			mSelectedChip = null;
		// Always remove trailing spaces when removing a chip.
		while(toDelete >= 0 && toDelete < text.length() && text.charAt(toDelete) == ' ')
			toDelete++;
		spannable.removeSpan(chip);
		if(spanStart >= 0 && toDelete > 0)
			text.delete(spanStart, toDelete);
		if(wasSelected)
			clearSelectedChip();
		if(mChipListener != null && callbackEnabled) {
			mChipListener.onDataChanged();
		}
	}


	/**
	 * Replace this currently selected chip with a new chip that uses the contact data provided.
	 */
	// Visible for testing.
  /* package */void replaceChip(final DrawableRecipientChip chip, final IRecipientEntry entry) {
		if(chip == null) {
			return;
		}
		final boolean wasSelected = chip == mSelectedChip;
		if(wasSelected)
			mSelectedChip = null;
		final int start = getChipStart(chip);
		final int end = getChipEnd(chip);
		getSpannable().removeSpan(chip);
		final Editable editable = getText();
		final CharSequence chipText = createChip(entry, false);
		if(chipText != null)
			if(start == -1 || end == -1) {
				Log.e(TAG, "The chip to replace does not exist but should.");
				editable.insert(0, chipText);
			} else if(!TextUtils.isEmpty(chipText)) {
				// There may be a space to replace with this chip's new
				// associated space. Check for it
				int toReplace = end;
				while(toReplace >= 0 && toReplace < editable.length() && editable.charAt(toReplace) == ' ')
					toReplace++;
				editable.replace(start, toReplace, chipText);
			}
		if(wasSelected)
			clearSelectedChip();
	}


	// Visible for testing.
  /* package */int movePastTerminators(int tokenEnd) {
		if(tokenEnd >= length())
			return tokenEnd;
		final char atEnd = getText().toString().charAt(tokenEnd);
		if(atEnd == COMMIT_CHAR_COMMA || atEnd == COMMIT_CHAR_SEMICOLON)
			tokenEnd++;
		// This token had not only an end token character, but also a space
		// separating it from the next token.
		if(tokenEnd < length() && getText().toString().charAt(tokenEnd) == ' ')
			tokenEnd++;
		return tokenEnd;
	}


	private int getExcessTopPadding() {
		if(sExcessTopPadding == -1)
			sExcessTopPadding = (int) (mChipHeight + mLineSpacingExtra);
		return sExcessTopPadding;
	}


	private void scrollBottomIntoView() {
		if(mScrollView != null) {
			final int[] location = new int[2];
			getLocationOnScreen(location);
			final int height = getHeight();
			final int currentPos = location[1] + height;
			// Desired position shows at least 1 line of chips below the action
			// bar. We add excess padding to make sure this is always below other
			// content.
			final int desiredPos = (int) mChipHeight + mActionBarHeight + getExcessTopPadding();
			if(currentPos > desiredPos)
				mScrollView.scrollBy(0, currentPos - desiredPos);
		}
	}


	private CharSequence ellipsizeText(final CharSequence text, final TextPaint paint, final float maxWidth) {
		paint.setTextSize(mChipFontSize);
		if(maxWidth <= 0 && Log.isLoggable(TAG, Log.DEBUG))
			Log.d(TAG, "Max width is negative: " + maxWidth);
		final CharSequence ellipsize = TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END);
		return ellipsize;
	}


	private Bitmap createSelectedChip(IRecipientEntry contact, TextPaint paint) {
		paint.setColor(sSelectedTextColor);
		Bitmap photo;
		photo = getContactBitmap(contact);
		return createChipBitmap(contact, paint, photo, mChipBackgroundPressed);
	}


	private Bitmap getContactBitmap(final IRecipientEntry entry) {
		byte[] photoBytes = entry.getPhotoBytes();
		if(photoBytes != null) {
			return BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
		} else {
			entry.getPhotoBytesAsync(new OnPhotoLoadedListener() {
				@Override
				public void onPhotoLoaded(byte[] photoBytes) {
					if(photoBytes != null)
						replaceChip(findChip(entry), entry);
				}
			});
		}

		return BitmapFactory.decodeResource(getResources(), entry.getDefaultPhotoResourceId());
	}


	private DrawableRecipientChip findChip(IRecipientEntry entry) {
		DrawableRecipientChip[] allchips = getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
		if(allchips != null) {
			for(int i = 0; i < allchips.length; i++) {
				if(allchips[i].getDataId() == entry.getDataId()) {
					return allchips[i];
				}
			}
		}
		return null;
	}


	private Bitmap createUnselectedChip(IRecipientEntry contact, TextPaint paint,
										boolean leaveBlankIconSpacer) {
		Drawable background = getChipBackground(contact);
		Bitmap photo = getContactBitmap(contact);
		paint.setColor(getContext().getResources().getColor(android.R.color.black));
		return createChipBitmap(contact, paint, photo, background);
	}


	private Bitmap createChipBitmap(IRecipientEntry contact, TextPaint paint, Bitmap icon,
									Drawable background) {
		if(background == null) {
			Log.w(TAG, "Unable to draw a background for the chips as it was never set");
			return Bitmap.createBitmap(
					(int) mChipHeight * 2, (int) mChipHeight, Bitmap.Config.ARGB_8888);
		}

		Rect backgroundPadding = new Rect();
		background.getPadding(backgroundPadding);

		// Ellipsize the text so that it takes AT MOST the entire width of the
		// autocomplete text entry area. Make sure to leave space for padding
		// on the sides.
		int height = (int) mChipHeight + getResources().getDimensionPixelSize(R.dimen.extra_chip_height);
		// Since the icon is a square, it's width is equal to the maximum height it can be inside
		// the chip.
		int iconWidth = height - backgroundPadding.top - backgroundPadding.bottom;
		float[] widths = new float[1];
		paint.getTextWidths(" ", widths);
		CharSequence ellipsizedText = ellipsizeText(createChipDisplayText(contact), paint,
				calculateAvailableWidth() - iconWidth - widths[0] - backgroundPadding.left
						- backgroundPadding.right);
		int textWidth = (int) paint.measureText(ellipsizedText, 0, ellipsizedText.length());

		// Make sure there is a minimum chip width so the user can ALWAYS
		// tap a chip without difficulty.
		int width = Math.max(iconWidth * 2, textWidth + (mChipPadding * 2) + iconWidth
				+ backgroundPadding.left + backgroundPadding.right);

		// Create the background of the chip.
		Bitmap tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(tmpBitmap);

		// Draw the background drawable
		background.setBounds(height / 2, 0, width, height);
		background.draw(canvas);
		// Draw the text vertically aligned
		int textX = width - backgroundPadding.right - mChipPadding - textWidth;
		paint.setColor(0xFF5C5C5C);
		paint.setAntiAlias(true);
		canvas.drawText(ellipsizedText, 0, ellipsizedText.length(),
				textX, getTextYOffset(ellipsizedText.toString(), paint, height), paint);
		if(icon != null) {
			// Draw the icon
			icon = ChipsUtil.getClip(icon);
			int iconX = backgroundPadding.left;
			RectF src = new RectF(0, 0, icon.getWidth(), icon.getHeight());
			RectF dst = new RectF(0, 0, height, height);
			drawIconOnCanvas(icon, canvas, paint, src, dst);
		}
		return tmpBitmap;
	}


	private DrawableRecipientChip constructChipSpan(final IRecipientEntry contact, final boolean pressed, final boolean leaveIconSpace) throws NullPointerException {
		if(mChipBackground == null)
			throw new NullPointerException("Unable to render any chips as setChipDimensions was not called.");
		final TextPaint paint = getPaint();
		final float defaultSize = paint.getTextSize();
		final int defaultColor = paint.getColor();
		Bitmap tmpBitmap;
		if(pressed) {
			tmpBitmap = createSelectedChip(contact, paint);
		} else {
			tmpBitmap = createUnselectedChip(contact, paint, leaveIconSpace);
		}
		// Pass the full text, un-ellipsized, to the chip.
		final Drawable result = new BitmapDrawable(getResources(), tmpBitmap);
		result.setBounds(0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight());
		final DrawableRecipientChip recipientChip = new VisibleRecipientChip(result, contact);
		// Return text to the original size.
		paint.setTextSize(defaultSize);
		paint.setColor(defaultColor);
		return recipientChip;
	}


	/**
	 * Calculate the bottom of the line the chip will be located on using: 1) which line the chip appears on 2) the
	 * height of a chip 3) padding built into the edit text view
	 */
	private int calculateOffsetFromBottom(final int line) {
		// Line offsets start at zero.
		final int actualLine = getLineCount() - (line + 1);
		return -(actualLine * getActualChipHeight());
	}


	/**
	 * Get the max amount of space a chip can take up. The formula takes into account the width of the EditTextView, any
	 * view padding, and padding that will be added to the chip.
	 */
	private float calculateAvailableWidth() {
		return getWidth() - getPaddingLeft() - getPaddingRight() - mChipPadding * 2;
	}


	private void setChipDimensions(final Context context, final AttributeSet attrs) {
		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecipientEditTextView, 0, 0);
		final Resources r = getContext().getResources();
		mChipBackground = a.getDrawable(R.styleable.RecipientEditTextView_chipBackground);
		if(mChipBackground == null)
			mChipBackground = r.getDrawable(R.drawable.chip_background);
		mChipBackgroundPressed = a.getDrawable(R.styleable.RecipientEditTextView_chipBackgroundPressed);
		if(mChipBackgroundPressed == null)
			mChipBackgroundPressed = r.getDrawable(R.drawable.chip_background_selected);
		mChipPadding = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipPadding, -1);
		if(mChipPadding == -1)
			mChipPadding = (int) r.getDimension(R.dimen.chip_padding);
		mAlternatesLayout = a.getResourceId(R.styleable.RecipientEditTextView_chipAlternatesLayout, -1);
		if(mAlternatesLayout == -1)
			mAlternatesLayout = R.layout.chips_alternate_item;
//        mDefaultContactPhoto = BitmapFactory.decodeResource(r, R.drawable.ic_contact_picture);
		mMoreItem = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.more_item, null);
		mChipHeight = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipHeight, -1);
		if(mChipHeight == -1)
			mChipHeight = r.getDimension(R.dimen.chip_height);
		mChipMaxLines = a.getInteger(R.styleable.RecipientEditTextView_maxChipLines, 0);
		mChipFontSize = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipFontSize, -1);
		if(mChipFontSize == -1)
			mChipFontSize = getTextSize(); //r.getDimension(R.dimen.chip_text_size);
		mInvalidChipBackground = a.getDrawable(R.styleable.RecipientEditTextView_invalidChipBackground);
		if(mInvalidChipBackground == null)
			mInvalidChipBackground = r.getDrawable(R.drawable.chip_background_invalid);
		mLineSpacingExtra = r.getDimension(R.dimen.line_spacing_extra);
		final TypedValue tv = new TypedValue();
		if(context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
		a.recycle();
	}


	private int getActualChipHeight() {
		return (int) (mChipHeight + getResources().getDimensionPixelSize(R.dimen.extra_chip_height) + mLineSpacingExtra);
	}


	private void checkChipWidths() {
		// Check the widths of the associated chips.
		final DrawableRecipientChip[] chips = getSortedRecipients();
		if(chips != null) {
			Rect bounds;
			for(final DrawableRecipientChip chip : chips) {
				bounds = chip.getBounds();
				if(getWidth() > 0 && bounds.right - bounds.left > getWidth())
					// Need to redraw that chip.
					replaceChip(chip, chip.getEntry());
			}
		}
	}


	private boolean isValid(final String text) {
		return mValidator == null ? true : mValidator.isValid(text);
	}


	private boolean focusNext() {
		final View next = focusSearch(View.FOCUS_DOWN);
		if(next != null) {
			next.requestFocus();
			return true;
		}
		return false;
	}


	private boolean commitChip(final int start, final int end, final Editable editable) {
		final ListAdapter adapter = getAdapter();
		if(adapter != null && adapter.getCount() > 0 && enoughToFilter() && end == getSelectionEnd()) {
			// choose the first entry.
			submitItemAtPosition(0, true);
			dismissDropDown();
			return true;
		}
		return false;
	}


	private int getChipStart(final DrawableRecipientChip chip) {
		return getSpannable().getSpanStart(chip);
	}


	private int getChipEnd(final DrawableRecipientChip chip) {
		return getSpannable().getSpanEnd(chip);
	}


	private void clearSelectedChip() {
		if(mSelectedChip != null) {
			unselectChip(mSelectedChip);
			mSelectedChip = null;
		}
	}


	private void scrollLineIntoView(final int line) {
		if(mScrollView != null) {
			if(hasFocus()) {
				clearFocus();
			}
			mScrollView.smoothScrollTo(0, calculateOffsetFromTop(line));
		}
	}


	private int calculateOffsetFromTop(int line) {
		return getActualChipHeight() * line;
	}


	private ListAdapter createSingleAddressAdapter(final DrawableRecipientChip currentChip, SingleCustomRecipientArrayAdapter.OnDeleteItemListener onDeleteItemListener) {
		return new SingleCustomRecipientArrayAdapter(getContext(), mAlternatesLayout, currentChip.getEntry(), onDeleteItemListener);
	}


	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private int putOffsetInRange(final float x, final float y) {
		final int offset;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			offset = getOffsetForPosition(x, y);
		else offset = supportGetOffsetForPosition(x, y);
		return putOffsetInRange(offset);
	}


	// TODO: This algorithm will need a lot of tweaking after more people have used
	// the chips ui. This attempts to be "forgiving" to fat finger touches by favoring
	// what comes before the finger.
	private int putOffsetInRange(final int o) {
		int offset = o;
		final Editable text = getText();
		final int length = text.length();
		// Remove whitespace from end to find "real end"
		int realLength = length;
		for(int i = length - 1; i >= 0; i--)
			if(text.charAt(i) == ' ')
				realLength--;
			else break;
		// If the offset is beyond or at the end of the text,
		// leave it alone.
		if(offset >= realLength)
			return offset;
		final Editable editable = getText();
		while(offset >= 0 && findText(editable, offset) == -1 && findChip(offset) == null)
			// Keep walking backward!
			offset--;
		return offset;
	}


	private DrawableRecipientChip findChip(final int offset) {
		final DrawableRecipientChip[] chips = getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
		// Find the chip that contains this offset.
		for(int i = 0; i < chips.length; i++) {
			final DrawableRecipientChip chip = chips[i];
			final int start = getChipStart(chip);
			final int end = getChipEnd(chip);
			if(offset >= start && offset <= end)
				return chip;
		}
		return null;
	}


	private CharSequence createChip(final IRecipientEntry entry, final boolean pressed) {
		final String displayText = createAddressText(entry);
		if(TextUtils.isEmpty(displayText))
			return null;
		SpannableString chipText = null;
		// Always leave a blank space at the end of a chip.
		final int textLength = displayText.length() - 1;
		chipText = new SpannableString(displayText);
		if(!mNoChips)
			try {
				final DrawableRecipientChip chip = constructChipSpan(entry, pressed, false /*
																				* leave space for contact
                                                                                * icon
                                                                                */);
				chipText.setSpan(chip, 0, textLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				chip.setOriginalText(chipText.toString());
			} catch(final NullPointerException e) {
				Log.e(TAG, e.getMessage(), e);
				return null;
			}
		return chipText;
	}


	private void submitItemAtPosition(final int position, boolean callbackEnabled) {
		final IRecipientEntry entry = getAdapter().getItem(position);
		if(entry == null)
			return;
		clearComposingText();
		final int end = getSelectionEnd();
		final int start = mTokenizer.findTokenStart(getText(), end);
		final Editable editable = getText();
		QwertyKeyListener.markAsReplaced(editable, start, end, "");
		final CharSequence chip = createChip(entry, false);
		if(chip != null && start >= 0 && end >= 0)
			editable.replace(start, end, chip);
		sanitizeBetween();
		if(mChipListener != null && callbackEnabled)
			mChipListener.onDataChanged();
	}


	/**
	 * Show specified chip as selected. If the RecipientChip is just an email address, selecting the chip will take the
	 * contents of the chip and place it at the end of the RecipientEditTextView for inline editing. If the
	 * RecipientChip is a complete contact, then selecting the chip will change the background color of the chip, show
	 * the delete icon, and a popup window with the address in use highlighted and any other alternate addresses for the
	 * contact.
	 *
	 * @param currentChip Chip to select.
	 * @return A RecipientChip in the selected state or null if the chip just contained an email address.
	 */
	private DrawableRecipientChip selectChip(final DrawableRecipientChip currentChip) {
		final int start = getChipStart(currentChip);
		final int end = getChipEnd(currentChip);
		getSpannable().removeSpan(currentChip);
		DrawableRecipientChip newChip;
		try {
			if(mNoChips)
				return null;
			newChip = constructChipSpan(currentChip.getEntry(), true, false);
		} catch(final NullPointerException e) {
			Log.e(TAG, e.getMessage(), e);
			return null;
		}
		final Editable editable = getText();
		QwertyKeyListener.markAsReplaced(editable, start, end, "");
		if(start == -1 || end == -1)
			Log.d(TAG, "The chip being selected no longer exists but should.");
		else {
			editable.setSpan(newChip, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		newChip.setSelected(true);
		scrollLineIntoView(getLayout().getLineForOffset(getChipStart(newChip)));
		showAddress(newChip, mAddressPopup);
		return newChip;
	}


	private void showAddress(final DrawableRecipientChip currentChip, final ListPopupWindow popup) {
		if(!mAttachedToWindow)
			return;
		final int line = getLayout().getLineForOffset(getChipStart(currentChip));
		final int bottom = calculateOffsetFromBottom(line);
		// Align the alternates popup with the left side of the View,
		// regardless of the position of the chip tapped.
		popup.setWidth(Math.min(2 * currentChip.getBounds().width(), getWidth()));
		popup.setAnchorView(this);
		popup.setVerticalOffset(bottom);
		popup.setAdapter(createSingleAddressAdapter(currentChip, new SingleCustomRecipientArrayAdapter.OnDeleteItemListener() {
			@Override
			public void onDeleteItem(View view, IRecipientEntry entry) {

				removeRecipient(entry, true);
				popup.dismiss();
			}
		}));
		popup.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				clearSelectedChip();
				popup.dismiss();
			}
		});
		popup.show();
	}


	/**
	 * Remove selection from this chip. Unselecting a RecipientChip will render the chip without a delete icon and with
	 * an unfocused background. This is called when the RecipientChip no longer has focus.
	 */
	private void unselectChip(final DrawableRecipientChip chip) {
		final int start = getChipStart(chip);
		final int end = getChipEnd(chip);
		if(start == -1 || end == -1) {
			Log.e(TAG, "Chip that should be unselected doesn't exist");
			return;
		}
		final Editable editable = getText();
		mSelectedChip = null;
		getSpannable().removeSpan(chip);
		QwertyKeyListener.markAsReplaced(editable, start, end, "");
		editable.removeSpan(chip);
		try {
			if(!mNoChips)
				editable.setSpan(constructChipSpan(chip.getEntry(), false, false), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		} catch(final NullPointerException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		setSelection(editable.length());
	}


	// The following methods are used to provide some functionality on older versions of Android
	// These methods were copied out of JB MR2's TextView
	// ///////////////////////////////////////////////
	private int supportGetOffsetForPosition(final float x, final float y) {
		if(getLayout() == null)
			return -1;
		final int line = supportGetLineAtCoordinate(y);
		final int offset = supportGetOffsetAtCoordinate(line, x);
		return offset;
	}


	private float supportConvertToLocalHorizontalCoordinate(float x) {
		x -= getTotalPaddingLeft();
		// Clamp the position to inside of the view.
		x = Math.max(0.0f, x);
		x = Math.min(getWidth() - getTotalPaddingRight() - 1, x);
		x += getScrollX();
		return x;
	}


	private int supportGetLineAtCoordinate(float y) {
		y -= getTotalPaddingLeft();
		// Clamp the position to inside of the view.
		y = Math.max(0.0f, y);
		y = Math.min(getHeight() - getTotalPaddingBottom() - 1, y);
		y += getScrollY();
		return getLayout().getLineForVertical((int) y);
	}


	private int supportGetOffsetAtCoordinate(final int line, float x) {
		x = supportConvertToLocalHorizontalCoordinate(x);
		return getLayout().getOffsetForHorizontal(line, x);
	}


	private void deleselectAdapterItem(DrawableRecipientChip drawableRecipientChip) {
		List<IRecipientEntry> allItems = getAdapter().getObjects() != null ? getAdapter().getObjects() : new ArrayList<>();
		for(int i = 0; i < allItems.size(); i++) {
			if(drawableRecipientChip.getDataId() == allItems.get(i).getDataId()) {
				getAdapter().setSelected(i, false);
				if(mChipListener != null) {
					mChipListener.onDataChanged();
				}
				break;
			}
		}
	}


	private void removeChipById(long dataId, final boolean alsoNotifyAboutDataChanges) {
		final DrawableRecipientChip[] chips = getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
		final List<DrawableRecipientChip> chipsToRemove = new ArrayList<DrawableRecipientChip>();
		for(final DrawableRecipientChip chip : chips)
			if(chip.getDataId() == dataId)
				chipsToRemove.add(chip);
		for(final DrawableRecipientChip chip : chipsToRemove)
			removeChip(chip, alsoNotifyAboutDataChanges, true);
	}


	public interface IChipListener {
		void onDataChanged();
	}


	public static class CustomCommaTokenizer implements Tokenizer {
		@Override
		public int findTokenStart(CharSequence text, int cursor) {
			int i = cursor;

			while(i > 0 && text.charAt(i - 1) != ',') {
				i = skipIfNeccesary(text, i, true);
				if(i > 0)
					i--;

			}
			while(i < cursor && text.charAt(i) == ' ') {
				i++;
			}

			return i;
		}


		@Override
		public int findTokenEnd(CharSequence text, int cursor) {
			int i = cursor;
			int len = text.length();

			while(i < len) {
				i = skipIfNeccesary(text, i, false);
				if(text.charAt(i) == ',') {
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

			while(i > 0 && text.charAt(i - 1) == ' ') {
				i--;
			}

			if(i > 0 && text.charAt(i - 1) == ',') {
				return text;
			} else {
				if(text instanceof Spanned) {
					SpannableString sp = new SpannableString(text + ", ");
					TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
							Object.class, sp, 0);
					return sp;
				} else {
					return text + ", ";
				}
			}
		}


		private int skipIfNeccesary(CharSequence text, int cursor, boolean forward) {
			if(forward) {
				if(text.charAt(cursor - 1) == '"')
					return findQuotationMark(text, cursor - 1, forward);
			} else {
				if(text.charAt(cursor) == '"')
					return findQuotationMark(text, cursor, forward);
			}

			return cursor;
		}


		private int findQuotationMark(CharSequence text, int cursor, boolean forward) {
			int i = cursor;
			if(forward) {
				while((i > 0 && text.charAt(i - 1) != '"') ||
						(i > 1 && text.charAt(i - 1) == '"' && text.charAt(i - 2) == '\\')) {
					i--;
				}
				return i != 0 ? i - 1 : cursor;
			} else {
				i++;
				while((i < text.length() && text.charAt(i) != '"') ||
						(i < text.length() && i > 0 && text.charAt(i) == '"' && text.charAt(i - 1) == '\\')) {
					i++;
				}
				return i != text.length() ? i : cursor;
			}
		}
	}


	// //////////////////////////////////////////////////////////////////
	// RecipientTextWatcher //
	// ///////////////////////
	private class RecipientTextWatcher implements TextWatcher {
		private String tempText;


		@Override
		public void afterTextChanged(final Editable s) {
			// If the text has been set to null or empty, make sure we remove
			// all the spans we applied.
			if(TextUtils.isEmpty(s)) {
				// Remove all the chips spans.
				final Spannable spannable = getSpannable();
				final DrawableRecipientChip[] chips = spannable.getSpans(0, getText().length(), DrawableRecipientChip.class);
				for(final DrawableRecipientChip chip : chips) {
					spannable.removeSpan(chip);
				}
				getAdapter().deselectAll();
				return;
			}
		}


		@Override
		public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
			// The user deleted some text OR some text was replaced; check to
			// see if the insertion point is on a space
			// following a chip.
			scrollBottomIntoView();
			if(mAddressPopup != null && mAddressPopup.isShowing()) {
				mAddressPopup.dismiss();
			}
			clearSelectedChip();
			if(count != before) {
				final DrawableRecipientChip[] chips = getSpannable().getSpans(0, getText().length(), DrawableRecipientChip.class);
				final int chipsCount = chips.length;
				if(mPreviousChipsCount != chipsCount)
					mPreviousChipsCount = chipsCount;
			}
			if(before - count == 1) {
				// If the item deleted is a space, and the thing before the
				// space is a chip, delete the entire span.
				final int selStart = getSelectionStart();
				final DrawableRecipientChip[] repl = getSpannable().getSpans(selStart, s.length(), DrawableRecipientChip.class);
				if(repl.length > 0) {
					deleselectAdapterItem(repl[0]);
					// There is a chip there! Just remove it.
					final Editable editable = getText();
					// Add the separator token.
					final int tokenStart = mTokenizer.findTokenStart(editable, selStart > 0 ? selStart - 1 : selStart);
					int tokenEnd = mTokenizer.findTokenEnd(editable, tokenStart);
					tokenEnd = tokenEnd + 1;
					if(tokenEnd > editable.length())
						tokenEnd = editable.length();
					editable.delete(tokenStart, tokenEnd);
					getSpannable().removeSpan(repl[0]);
				}
			}
		}


		@Override
		public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
			// Do nothing.
			// final DrawableRecipientChip[] chips = getSpannable().getSpans(0, getText().length(),
			// DrawableRecipientChip.class);
			// previousChipsCount = chips.length;
		}
	}
}
