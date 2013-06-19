package org.retroshare.android;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class AddServerActivity extends ProxiedActivityBase
{
	private static final String TAG = "AddServerActivity";

	boolean editServer = false;
	
    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
    {
        setContentView(R.layout.activity_add_server);

		editServer = getIntent().getBooleanExtra(getString(R.string.editServer), false);

		View deleteBtn = findViewById(R.id.deleteServerButton);
		if(editServer)
		{
			deleteBtn.setVisibility(View.VISIBLE);
			findViewById(R.id.editTextSshKey).setVisibility(View.VISIBLE);
			findViewById(R.id.resetPasswordCheckBox).setVisibility(View.VISIBLE);
			findViewById(R.id.resetSshServerKeyCheckBox).setVisibility(View.VISIBLE);
			findViewById(R.id.editTextName).setEnabled(false);
		}
		else
		{
			findViewById(R.id.resetPasswordCheckBox).setVisibility(View.GONE);
			findViewById(R.id.editTextSshKey).setVisibility(View.GONE);
			findViewById(R.id.resetSshServerKeyCheckBox).setVisibility(View.GONE);
			deleteBtn.setVisibility(View.GONE);
		}
    }

	protected void onServiceConnected()
	{
		if(mBound && editServer)
		{
			EditText editTextName     = (EditText) findViewById(R.id.editTextName);
			EditText editTextHostname = (EditText) findViewById(R.id.editTextHostname);
			EditText editTextPort     = (EditText) findViewById(R.id.editTextPort);
			EditText editTextUser     = (EditText) findViewById(R.id.editTextUser);
			EditText editTextSshKey   = (EditText) findViewById(R.id.editTextSshKey);

			RsServerData sd = rsProxy.getSavedServers().get(serverName);
			editTextName.setText(sd.name);
			editTextHostname.setText(sd.hostname);
			editTextPort.setText(String.valueOf(sd.port));
			editTextUser.setText(sd.user);
			try { editTextSshKey.setText(sd.getHostkeyFingerprint()); } catch (Exception e) {}
		}
	}
    
	public void onSaveButtonPressed(View v)
	{
		if(mBound)
		{
			EditText editTextName     = (EditText) findViewById(R.id.editTextName);
			EditText editTextHostname = (EditText) findViewById(R.id.editTextHostname);
			EditText editTextPort     = (EditText) findViewById(R.id.editTextPort);
			EditText editTextUser     = (EditText) findViewById(R.id.editTextUser);
			CheckBox resetPwdChkb     = (CheckBox) findViewById(R.id.resetPasswordCheckBox);
			CheckBox reseteSshSrvKey  = (CheckBox) findViewById(R.id.resetSshServerKeyCheckBox);

			if(!util.hasContent(editTextPort) || !util.hasContent(editTextName) || !util.hasContent(editTextHostname) || !util.hasContent(editTextUser))
			{
				Toast.makeText(getApplicationContext(), "Please fill in all fields.", Toast.LENGTH_LONG).show(); // TODO HARDCODED string
				return;
			}

			RsServerData sd = rsProxy.getSavedServers().get(serverName);

			sd.hostname = editTextHostname.getText().toString();

			if(util.hasContent(editTextPort)) sd.port = Integer.parseInt(editTextPort.getText().toString());

			sd.user = editTextUser.getText().toString();

			if(resetPwdChkb.isChecked())
			{
				sd.password = null;
				sd.savePassword = false;
			}

			if(reseteSshSrvKey.isChecked()) sd.hostkey = null;

			rsProxy.addServer(sd);
		}
		startActivity(MainActivity.class);
	}

	public void onDeleteButtonPressed(View v)
	{
		// TODO ask for confirmation before deleting the server
		_deleteServer();
	}

	private void _deleteServer()
	{
		if(mBound) rsProxy.delServer(serverName);
		startActivity(MainActivity.class);
	}
}