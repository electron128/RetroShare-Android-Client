package org.retroshare.android.authenticator;

import org.retroshare.android.R;
import org.retroshare.android.R.id;
import org.retroshare.android.R.layout;
import org.retroshare.android.R.string;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class AddAccountActivity extends AccountAuthenticatorActivity
{
	private static final String TAG="AddAccountActivity";
	
	AccountAuthenticatorResponse response;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	Log.d(TAG, "onCreate(...) begin");
    	
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate(...) inflating layout");
        setContentView(R.layout.activity_add_server);
        
        Log.d(TAG, "onCreate(...) saving reponce reference");
        response = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        
        Log.d(TAG, "onCreate(...) end");
    }
    
    public void onAddAccountPressed(View v)
    {
		String mAccountType = getString(R.string.ACCOUNT_TYPE);
		String mAccountName = ((EditText) findViewById(R.id.editTextName)).getText().toString();

		/*//save account to retroshare
		RsServerData sd = new RsServerData();
		sd.name         = mAccountName;
    	sd.hostname     = mHostName;
    	sd.port         = Integer.parseInt(mPort);
    	sd.user         = mUsername;
    	*/
		
    	Account account = new Account(mAccountName, mAccountType);
		AccountManager am = AccountManager.get(this);
		
		boolean accountCreated = am.addAccountExplicitly(account, null, null);
		
		if(accountCreated)
		{
			final Intent intent = new Intent();  
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccountName);  
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);  
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAccountType); // TODO double check this... http://www.finalconcept.com.au/article/view/android-account-manager-step-by-step-2
            setAccountAuthenticatorResult(intent.getExtras());  
            setResult(RESULT_OK, intent);  
			
            Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            response.onResult(bundle);
            
			Log.d(TAG, "saveAccount(View v) reponse with account " + mAccountType + "%%" + mAccountName );
		}
		
		Log.d(TAG, am.getAccounts().toString());

    	finish();
    }
}
