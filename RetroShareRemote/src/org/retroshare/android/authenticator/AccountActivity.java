package org.retroshare.android.authenticator;

import org.retroshare.android.R;
import org.retroshare.android.RsActivityBase;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class AccountActivity extends RsActivityBase
{
	String TAG = "AccountActivity";
	
	AccountAuthenticatorResponse response;
	
	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        
        response = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
    }
	
	public void saveAccount(View v)
	{
		Log.d(TAG, "saveAccount(View v)");
		
		String mAccountType = getString(R.string.ACCOUNT_TYPE);
		String mHostName    = ((EditText) findViewById(R.id.hostname_edit)).getText().toString();
		String mPort        = ((EditText) findViewById(R.id.port_edit)).getText().toString();
		String mUsername    = ((EditText) findViewById(R.id.userid_edit)).getText().toString();
		String mPassword    = ((EditText) findViewById(R.id.password_edit)).getText().toString();
		
		Account account = new Account(mUsername, mAccountType);
		AccountManager am = AccountManager.get(this);
		
		Bundle result = new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
		result.putString(AccountManager.KEY_ACCOUNT_NAME, mUsername + "@" + mHostName + ":" + mPort );
		result.putString(AccountManager.KEY_AUTHTOKEN, "Ci si puo' mettere una chiave ssl qui?");
		
		boolean accountCreated = am.addAccountExplicitly(account, mPassword, result);

		if(accountCreated)
		{
			Log.d(TAG, "saveAccount(View v) reponse with account " + mAccountType + "%%" + mUsername + "%%" + mPassword);
			
			// Now we tell our caller, could be the Android Account Manager or even our own application that the process was successful
			final Intent intent = new Intent();
			intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mHostName);
			intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);

			setResult(RESULT_OK, intent);
			
			response.onResult(result);
		}
		
		Log.d(TAG, am.getAccounts().toString());
		
		finish();
	}

}
