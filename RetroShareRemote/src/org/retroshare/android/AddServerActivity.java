package org.retroshare.android;

import org.retroshare.java.RsServerData;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class AddServerActivity extends RsActivityBase
{
	private static final String TAG="AddServerActivity";
	
	EditText editTextName;
   	EditText editTextHostname;
   	EditText editTextPort;
   	EditText editTextUser;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_add_server);
        
        editTextName     = (EditText) findViewById(R.id.editTextName);
        editTextHostname = (EditText) findViewById(R.id.editTextHostname);
        editTextPort     = (EditText) findViewById(R.id.editTextPort);
    	editTextUser     = (EditText) findViewById(R.id.editTextUser);
    }
    
    public void addServer(View v)
    {
    	if(mBound)
    	{
    		RsServerData sd = new RsServerData();
    		sd.name     = editTextName.getText().toString();
	    	sd.hostname = editTextHostname.getText().toString();
	    	sd.port     = Integer.parseInt(editTextPort.getText().toString());
	    	sd.user     = editTextUser.getText().toString();
	    	mRsService.mRsCtrlService.setServerData(sd);
    	}
    	finish();
    }
    
    @Override protected void onServiceConnected(){}
    @Override public void onResume(){ super.onResume(); } // TODO if we doesn't override this is not super called automatically ?
    @Override public void onPause(){ super.onPause(); } // TODO if we doesn't override this is not super called automatically ?

}
