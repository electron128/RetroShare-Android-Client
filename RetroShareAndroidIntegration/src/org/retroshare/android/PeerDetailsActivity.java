package org.retroshare.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;


public class PeerDetailsActivity extends ProxiedActivityBase
{
	private static final String TAG="PeerDetailsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peerdetails);
        
        TextView textViewName=(TextView) findViewById(R.id.peerNameTextView);
        TextView textViewLocation=(TextView) findViewById(R.id.peerNameTextView);
        
        Intent i = getIntent();
        textViewName.setText(i.getStringExtra("GpgId"));
    }
}
