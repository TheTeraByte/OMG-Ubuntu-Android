package com.ohso.omgubuntu;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AuthorsFragmentTab extends BaseFragment {
	public AuthorsFragmentTab() {
		// TODO Auto-generated constructor stub
		setTitle("Authors");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		if (container == null) {
			//If the fragment frame doesn't exist, don't waste time inflating the view
			return null;
		}
		//return super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.tab_fragment_authors, container, false);
	}
//	public static AuthorsFragmentTab newInstance () {
//		AuthorsFragmentTab fragmentPage = new AuthorsFragmentTab();
////		Bundle bundle = new Bundle();
////		bundle.putString("title", title);
////		fragmentPage.setArguments(bundle);
//		return fragmentPage;
//	}

}
