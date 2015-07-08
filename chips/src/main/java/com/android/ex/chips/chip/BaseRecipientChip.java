package com.android.ex.chips.chip;

import com.android.ex.chips.IRecipientEntry;


/**
 * Created by Roman on 4/12/2015.
 */
public interface BaseRecipientChip {

	/**
	 * Return true if the chip is selected.
	 */
	boolean isSelected();
	/**
	 * Set the selected state of the chip.
	 */
	void setSelected(boolean selected);
	/**
	 * Get the text displayed in the chip.
	 */
	CharSequence getDisplay();

	/**
	 * Get the text value this chip represents.
	 */
	CharSequence getValue();

	/**
	 * Get the id of the data associated with this chip.
	 */
	long getDataId();

	/**
	 * Get associated RecipientEntry.
	 */
	IRecipientEntry getEntry();
	/**
	 * Set the text in the edittextview originally associated with this chip before any reverse lookups.
	 */
	CharSequence getOriginalText();
	/**
	 * Set the text in the edittextview originally associated with this chip before any reverse lookups.
	 */
	void setOriginalText(String text);
}
