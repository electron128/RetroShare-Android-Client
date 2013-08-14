package org.retroshare.android.tests;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.retroshare.android.ConversationFragmentActivity;
import org.retroshare.android.R;

/**
 * @author G10h4ck
 */
public class TestActivity extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tester);
	}

	public void onButtonTestQrReadPressed(View v)
	{
		Intent i = new Intent();
		i.setAction(Intent.ACTION_VIEW);
		i.addCategory(Intent.CATEGORY_DEFAULT);
		i.addCategory(Intent.CATEGORY_BROWSABLE);

		Uri uri = new Uri.Builder()
				.scheme(getString(R.string.retroshare_uri_scheme))
				.authority(getString(R.string.person_uri_authority))
				.appendQueryParameter(getString(R.string.name_uri_query_param), "Just Relay It")
				.appendQueryParameter(getString(R.string.hash_uri_query_param), "AA3BFD5CEEE7EC17")
				.build();

		i.setData(uri);

		startActivity(i);
	}

	public void onButtonTestFragmentChatActivityPressed(View v)
	{
		Intent i = new Intent(this, ConversationFragmentActivity.class);
		i.putExtra(ConversationFragmentActivity.SERVER_NAME_EXTRA, "testServer");
		startActivity(i);
	}

	public void onButtonTestHtmlBase64ImageGetterPressed(View v)
	{
		Intent i = new Intent(this, HtmlBase64ImageGetterTestActivity.class);
		startActivity(i);
	}
}
