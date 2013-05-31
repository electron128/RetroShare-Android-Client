package com.example.retroshare.remote;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;


public class PeerDetailsActivity extends RsActivityBase {
	private static final String TAG="PeerDetailsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peerdetails);
        
        // TODO
        
        TextView textViewName=(TextView) findViewById(R.id.textViewName);
        TextView textViewLocation=(TextView) findViewById(R.id.textViewName);
        
        Intent i=getIntent();
        textViewName.setText(i.getStringExtra("GpgId"));
    }
    
    @Override
    protected void onServiceConnected(){
        
    }
}
