package com.strv.chipssample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.ex.chips.SelectableArrayAdapter;

import java.util.List;


/**
 * Created by Roman on 08/07/2015.
 */
public class CheeseAdapter extends SelectableArrayAdapter<Cheese> {
	private final Context mContext;
	private final LayoutInflater mLayoutInflater;


	public CheeseAdapter(Context context, List<Cheese> list) {
		super(list);
		mContext = context;
		mLayoutInflater = LayoutInflater.from(context);
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView == null) {
			holder = new ViewHolder();
			convertView = mLayoutInflater.inflate(R.layout.fragment_main_item, parent, false);
			holder.titleTextView = (TextView) convertView.findViewById(R.id.title_text_view);
			holder.subTitleTextView = (TextView) convertView.findViewById(R.id.subtitle_text_view);
			holder.iconView = (ImageView) convertView.findViewById(R.id.icon_image_view);
			holder.tickImageView = (ImageView) convertView.findViewById(R.id.tick_image_view);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Cheese cheese = getItem(position);

		holder.titleTextView.setText(cheese.name);
		holder.subTitleTextView.setText(cheese.otherName);
		holder.iconView.setImageResource(cheese.iconId);
		holder.tickImageView.setImageResource(isSelected(position) ?
				R.drawable.ic_ic_checkmark_active :
				R.drawable.ic_ic_checkmark_default);

		return convertView;
	}


	private static class ViewHolder {
		TextView titleTextView;
		TextView subTitleTextView;
		ImageView tickImageView;
		ImageView iconView;
	}
}
