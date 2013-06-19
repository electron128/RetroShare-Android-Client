package org.retroshare.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
			CheckBox resetSshSrvKey  = (CheckBox) findViewById(R.id.resetSshServerKeyCheckBox);

			if(!util.hasContent(editTextPort) || !util.hasContent(editTextName) || !util.hasContent(editTextHostname) || !util.hasContent(editTextUser))
			{
				Toast.makeText(getApplicationContext(), "Please fill in all fields.", Toast.LENGTH_LONG).show(); // TODO HARDCODED string
				return;
			}

			RsServerData sd;
			if(editServer) sd = rsProxy.getSavedServers().get(serverName);
			else sd = new RsServerData();

			sd.name = editTextName.getText().toString();
			sd.hostname = editTextHostname.getText().toString();

			if(util.hasContent(editTextPort)) sd.port = Integer.parseInt(editTextPort.getText().toString());

			sd.user = editTextUser.getText().toString();

			if(resetPwdChkb.isChecked())
			{
				sd.password = null;
				sd.savePassword = false;
			}

			if(resetSshSrvKey.isChecked()) sd.hostkey = null;

			rsProxy.addServer(sd);
			Toast.makeText(getApplicationContext(), "Server saved", Toast.LENGTH_SHORT).show(); // TODO HARDCODED string
			startActivity(MainActivity.class);
		}
	}

	public void onDeleteButtonPressed(View v) { showDialog(DIALOG_DELETE_SERVER); }

	private static final int DIALOG_DELETE_SERVER = 0;
	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case DIALOG_DELETE_SERVER:
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.are_you_sure)
						.setMessage(R.string.do_you_want_delete_server)
						.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) { _deleteServer(); } })
						.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {} });
				return builder.create();
			}
		}

		return null;
	}

	private final void _deleteServer()
	{
		if(mBound)
		{
			rsProxy.delServer(serverName);
			Toast.makeText(getApplicationContext(), "Server deleted", Toast.LENGTH_SHORT).show(); // TODO HARDCODED string
			startActivity(MainActivity.class);
		}
	}
}