package org.retroshare.android.authenticator;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.retroshare.android.R;

/**
 * @author G10h4ck
 */
public class AccountPreferenceActivity extends PreferenceActivity
{
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.account_preferences);
	}
}
