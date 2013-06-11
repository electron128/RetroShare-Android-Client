package org.retroshare.android.authenticator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.retroshare.android.R;
import org.retroshare.android.RsActivityBaseNG;
import org.retroshare.android.RsService;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
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
	
	ArrayList<String> rsAvailableServers;
    ArrayAdapter<String> spinnerAdapter;

	@Override
    protected void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
        Log.d(TAG, "onCreateBeforeConnectionInit(Bundle savedInstanceState)");

		response = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

		setContentView(R.layout.activity_account);
	}
	
	protected void onServiceConnected()
    {
		Log.d(TAG, "onServiceConnected()");
		if(mBound)
		{
	        // Get RetroShare servers
            rsAvailableServers = new ArrayList<String>();
			rsAvailableServers.addAll(mRsService.getServers().keySet());
            if(rsAvailableServers.size() < 1) return;

	        // Remove servers already associated with an android account
	        for(Account account : (AccountManager.get(this)).getAccountsByType(getString(R.string.ACCOUNT_TYPE))) rsAvailableServers.remove(account.name);
            if(rsAvailableServers.size() < 1) return;

            // Put available servers inside the Spinner
            spinnerAdapter = new ArrayAdapter<String>(AccountActivity.this, R.layout.text_view, R.id.available_account_spinner, rsAvailableServers);
            Spinner accountSpinner = (Spinner) this.findViewById(R.id.available_account_spinner);
            accountSpinner.setAdapter(spinnerAdapter);

            //spinnerAdapter.notifyDataSetChanged();
		}
    }
	
	public void saveAccount(View v)
	{
		Log.d(TAG, "saveAccount(View v)");
		
		String mAccountType = getString(R.string.ACCOUNT_TYPE);
		String accountName  = ((Spinner) findViewById(R.id.available_account_spinner)).getSelectedItem().toString();
		
		Account account = new Account(accountName, mAccountType);
		AccountManager am = AccountManager.get(this);
		
		Bundle result = new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
		result.putString(AccountManager.KEY_ACCOUNT_NAME, accountName );
		
		boolean accountCreated = am.addAccountExplicitly(account, null, result);

		if(accountCreated)
		{
			Log.d(TAG, "saveAccount(View v) response with account " + mAccountType);
			
			// Now we tell our caller, could be the Android Account Manager or even our own application that the process was successful
			final Intent intent = new Intent();
			intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
			intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);

			setResult(RESULT_OK, intent);
			
			response.onResult(result);
		}
		
		Log.d(TAG, am.getAccounts().toString());
		
		finish();
	}
}
