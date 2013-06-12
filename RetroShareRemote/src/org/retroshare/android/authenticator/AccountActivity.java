package org.retroshare.android.authenticator;

import java.util.ArrayList;

import org.retroshare.android.AddServerActivity;
import org.retroshare.android.R;
import org.retroshare.android.RsActivityBaseNG;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


public class AccountActivity extends RsActivityBaseNG
{
	private static final String TAG = "AccountActivity";
	
	AccountAuthenticatorResponse response;

	@Override
    protected void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
        Log.d(TAG, "onCreateBeforeConnectionInit(Bundle savedInstanceState)");

		response = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

		setContentView(R.layout.activity_account);
	}

    @Override
	protected void onServiceConnected()
    {
		Log.d(TAG, "onServiceConnected()");
        // Get RetroShare servers
        ArrayList<String> rsAvailableServers = new ArrayList<String>();
        rsAvailableServers.addAll(mRsService.getServers().keySet());
        if(rsAvailableServers.size() < 1) return;

        // Remove servers already associated with an android account
        for(Account account : (AccountManager.get(this)).getAccountsByType(getString(R.string.ACCOUNT_TYPE))) rsAvailableServers.remove(account.name);
        if(rsAvailableServers.size() < 1) return;

        // Put available servers inside the Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(AccountActivity.this, R.layout.text_view, rsAvailableServers);
        Spinner accountSpinner = (Spinner) this.findViewById(R.id.available_account_spinner);
        accountSpinner.setAdapter(spinnerAdapter);
    }
	
	public void saveAccount(View v)
	{
		Log.d(TAG, "saveAccount(View v)");

		final Spinner accountSpinner = (Spinner) findViewById(R.id.available_account_spinner);
		Object selectedItem = accountSpinner.getSelectedItem();
		if(selectedItem != null)
		{
			String accountName = selectedItem.toString();

			String mAccountType = getString(R.string.ACCOUNT_TYPE);
			Account account = new Account(accountName, mAccountType);
			AccountManager am = AccountManager.get(this);

			Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
			result.putString(AccountManager.KEY_ACCOUNT_NAME, accountName );

			boolean accountCreated = am.addAccountExplicitly(account, null, result);

			if(accountCreated)
			{
				// Now we tell our caller, could be the Android Account Manager or even our own application that the process was successful
				final Intent intent = new Intent();
				intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
				intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);

				setResult(RESULT_OK, intent);

				response.onResult(result);
			}

			finish();
        }
        else
		{
			new AlertDialog.Builder(this)
					.setTitle(R.string.authenticator_activity_accounts_not_selected_title)
					.setMessage(R.string.authenticator_activity_accounts_not_selected_message)
					.setPositiveButton
							(
									android.R.string.yes,
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which)
										{
											if (accountSpinner.getCount() < 1)
											{
												Intent intent = new Intent(AccountActivity.this, AddServerActivity.class);
												startActivity(intent);
											}
										}
									}
							)
                    .setNegativeButton
							(android.R.string.no,
									new DialogInterface.OnClickListener()
									{
										public void onClick(DialogInterface dialog, int which) {
											AccountActivity.this.finish();
										}
									}
							)
                    .show();
		}
	}
}
