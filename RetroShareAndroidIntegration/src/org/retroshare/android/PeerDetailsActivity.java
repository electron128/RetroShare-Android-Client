package org.retroshare.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import rsctrl.core.Core.Person;
import rsctrl.peers.Peers;


public class PeerDetailsActivity extends ProxiedActivityBase
{
	public String TAG() { return "PeerDetailsActivity"; }

	public final static String PGP_ID_EXTRA = "pgpId";
	private String pgpId;
	private String name;

    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
		Intent i = getIntent();
		if( i.hasExtra(PGP_ID_EXTRA) ) pgpId = i.getStringExtra(PGP_ID_EXTRA);
		else
		{
			Uri uri = i.getData();
			pgpId = uri.getQueryParameter(getString(R.string.hash_uri_query_param));
			name = uri.getQueryParameter(getString(R.string.name_uri_query_param));
		}

		setContentView(R.layout.activity_peerdetails);
    }

	@Override
	public void onServiceConnected()
	{
		Person p = getConnectedServer().mRsPeersService.getPersonByPgpId(pgpId);

		if( p != null ) name = p.getName();

		TextView nameTextView = (TextView) findViewById(R.id.peerNameTextView);
		TextView pgpIdTextView = (TextView) findViewById(R.id.pgpIdTextView);
		Button toggleFriendshipButton = (Button) findViewById(R.id.buttonToggleFriendship);

		nameTextView.setText(name);
		pgpIdTextView.setText(pgpId);

		Person.Relationship r = p.getRelation();
		if ( r.equals(Person.Relationship.YOURSELF) ) toggleFriendshipButton.setVisibility(View.GONE);
		else if ( r.equals(Person.Relationship.FRIEND) ) toggleFriendshipButton.setText(R.string.block_friend);
		else toggleFriendshipButton.setText(R.string.add_as_friend);
	}

	public void onToggleFriendshipButtonPressed(View v)
	{
		if(isBound())
		{
			RsPeersService prs = getConnectedServer().mRsPeersService;

			Person p = prs.getPersonByPgpId(pgpId);
			if(p == null) p = Person.newBuilder().setGpgId(pgpId).setName(name).setRelation(Person.Relationship.UNKNOWN).build();

			prs.requestToggleFriendShip(p);
			prs.requestPersonsUpdate(Peers.RequestPeers.SetOption.ALL, Peers.RequestPeers.InfoOption.ALLINFO);

			finish();
		}
	}
}
