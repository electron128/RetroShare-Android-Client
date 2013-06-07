package org.retroshare.android;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class AddAccountActivity extends RsActivityBase
{
	private static final String TAG="AddAccountActivity";
	
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_add_server);
    }
    
    public void addServer(View v)
    {
    	if(mBound)
    	{
    		EditText editTextName     = (EditText) findViewById(R.id.editTextName);
    		EditText editTextHostname = (EditText) findViewById(R.id.editTextHostname);
    		EditText editTextPort     = (EditText) findViewById(R.id.editTextPort);
    		EditText editTextUser     = (EditText) findViewById(R.id.editTextUser);
    		
    		RsServerData sd = new RsServerData();
    		
    		sd.name=editTextName.getText().toString();
	    	sd.hostname=editTextHostname.getText().toString();
	    	sd.port=Integer.parseInt(editTextPort.getText().toString());
	    	sd.user=editTextUser.getText().toString();
	    	mRsService.mRsCtrlService.setServerData(sd);
    	}
    	finish();
    }
}
