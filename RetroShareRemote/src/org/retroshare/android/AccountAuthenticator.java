package org.retroshare.android;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class AccountAuthenticator extends AbstractAccountAuthenticator
{
	private static final String TAG="AccountAuthenticator";
	
	private final Context mContext;

    public AccountAuthenticator(Context context)
    {
        super(context);
        mContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options)
    {
    	
    	Log.d(TAG, "addAccount(...) begin");
    	
    	final Bundle result;
        final Intent intent;
        
        intent = new Intent(mContext, AddAccountActivity.class);
        intent.putExtra(mContext.getString(R.string.authenticator_auth_token_type), authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        
        result = new Bundle();
        result.putParcelable(AccountManager.KEY_INTENT, intent);
        
        Log.d(TAG, "addAccount(...) end");
        
        return result;

    }

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException { return null; }

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) { return null; }

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException { return null; }

	@Override
	public String getAuthTokenLabel(String authTokenType) {	return null; }

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException { return null; }

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException { return null; }

}
