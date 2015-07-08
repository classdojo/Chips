package com.strv.chipssample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.ex.chips.CustomRecipientEditTextView;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Roman on 08/07/2015.
 */
public class MainFragment extends Fragment {

	public static final int IMAGE_POST_DELAY = 1000;

	private View mRootView;
	private CheeseAdapter mAdapter;
	private ListView mListView;
	private CustomRecipientEditTextView mEtSearch;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		setHasOptionsMenu(true);
	}


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_main, container, false);
		loadData();
		renderView();
		return mRootView;
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_main, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.action_use_list:
				new Preferences(getActivity()).setUseList(true);
				setListView();
				return true;
			case R.id.action_use_popup:
				new Preferences(getActivity()).setUseList(false);
				setListView();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private void loadData() {
		List<Cheese> list = new ArrayList<>();
		for(int i = 1; i < Cheeses.sCheeseStrings.length; i += 2) {
			list.add(new Cheese(Cheeses.sCheeseStrings[i - 1], Cheeses.sCheeseStrings[i], i, Cheeses.getRandomCheeseDrawable()));
		}
		mAdapter = new CheeseAdapter(getActivity(), list);
	}


	private void renderView() {

		mListView = (ListView) mRootView.findViewById(R.id.list_view);
		mListView.setAdapter(mAdapter);

		mEtSearch = (CustomRecipientEditTextView) mRootView.findViewById(R.id.et_chips);
		mEtSearch.setTokenizer(new CustomRecipientEditTextView.CustomCommaTokenizer());
		mEtSearch.setAdapter(mAdapter);
		setListView();
	}


	private void setListView() {
		boolean useList = new Preferences(getActivity()).useList();
		mEtSearch.setListView(useList ? mListView : null);
		mListView.setVisibility(useList ? View.VISIBLE : View.GONE);
	}
}
