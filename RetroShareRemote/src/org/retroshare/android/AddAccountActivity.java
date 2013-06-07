package org.retroshare.android;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class AddAccountActivity extends RsActivityBase
{
	private static final String TAG="AddAccountActivity";
	
	AccountAuthenticatorResponse response;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_server);
        response = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
    }
    
    public void onAddAccountPressed(View v)
    {
    	if(mBound)
    	{
    		String mAccountType = getString(R.string.ACCOUNT_TYPE);
    		String mAccountName = ((EditText) findViewById(R.id.editTextName)).getText().toString();
    		String mHostName    = ((EditText) findViewById(R.id.editTextHostname)).getText().toString();
    		String mPort        = ((EditText) findViewById(R.id.editTextPort)).getText().toString();
    		String mUsername    = ((EditText) findViewById(R.id.editTextUser)).getText().toString();

    		
    		RsServerData sd = new RsServerData();
    		sd.name         = mAccountName;
	    	sd.hostname     = mHostName;
	    	sd.port         = Integer.parseInt(mPort);
	    	sd.user         = mUsername;
	    	mRsService.mRsCtrlService.setServerData(sd);
	    	
	    	Account account = new Account(mAccountName, mAccountType);
			AccountManager am = AccountManager.get(this);
			
			Bundle result = new Bundle();
			boolean accountCreated = am.addAccountExplicitly(account, null, result);
			
			if(accountCreated)
			{
				Log.d(TAG, "saveAccount(View v) reponse with account " + mAccountType + "%%" + mAccountName );
				response.onResult(result);
			}
			
			Log.d(TAG, am.getAccounts().toString());
    	}
    	finish();
    }
}
