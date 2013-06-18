package org.retroshare.android;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class AddServerActivity extends ProxiedActivityBase
{
	private static final String TAG = "AddServerActivity";
	
	EditText editTextName;
   	EditText editTextHostname;
   	EditText editTextPort;
   	EditText editTextUser;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
		Log.d(TAG, "onCreate(Bundle savedInstanceState)");
		super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_add_server);
        
        editTextName     = (EditText) findViewById(R.id.editTextName);
        editTextHostname = (EditText) findViewById(R.id.editTextHostname);
        editTextPort     = (EditText) findViewById(R.id.editTextPort);
    	editTextUser     = (EditText) findViewById(R.id.editTextUser);
    }
    
    public void addServer(View v)
    {
		Log.d(TAG, "addServer(View v)");
        if(mBound)
		{
			Log.d(TAG, "addServer(View v) is bound");
            RsServerData sd = new RsServerData();
            sd.name = editTextName.getText().toString();
            sd.hostname = editTextHostname.getText().toString();
            if(util.hasContent(editTextPort)) sd.port = Integer.parseInt(editTextPort.getText().toString());
            sd.user = editTextUser.getText().toString();

			if(!util.hasContent(editTextPort) || !util.hasContent(editTextName) || !util.hasContent(editTextHostname) || !util.hasContent(editTextUser))
			{
                Toast.makeText(getApplicationContext(), "Please fill in all fields.", Toast.LENGTH_LONG).show();
                return;
            }

			Log.d(TAG, "addServer(View v) fields ok saving data");
            rsProxy.addServer(sd);
        }
        startActivity(MainActivity.class);
	}
}