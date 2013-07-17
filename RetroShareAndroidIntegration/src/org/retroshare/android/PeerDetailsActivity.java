package org.retroshare.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import rsctrl.core.Core.Person;
import rsctrl.peers.Peers;


public class PeerDetailsActivity extends ProxiedActivityBase implements RsPeersService.PeersServiceListener
{
	public String TAG() { return "PeerDetailsActivity"; }

	public final static String PGP_ID_EXTRA = "pgpId";
	private String pgpId;
	private String name;
	private boolean fromExtIntent = false;

    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
		if (serverName == null)
		{
			fromExtIntent = true;
			Intent i = new Intent(this, ServerChooserActivity.class);
			startActivityForResult(i, 0);
		}

		setContentView(R.layout.activity_peerdetails);

		Intent i = getIntent();
		if(fromExtIntent)
		{
			Uri uri = i.getData();
			pgpId = uri.getQueryParameter(getString(R.string.hash_uri_query_param));
			name = uri.getQueryParameter(getString(R.string.name_uri_query_param));
		}
		else pgpId = i.getStringExtra(PGP_ID_EXTRA);
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			serverName = data.getStringExtra(SERVER_NAME_EXTRA);
			updateViews();
		}
	}

	@Override
	public void onServiceConnected() { updateViews(); }

	public void onToggleFriendshipButtonPressed(View v)
	{
		if(isBound())
		{
			RsPeersService prs = getConnectedServer().mRsPeersService;

			Person p;
			if(fromExtIntent) p = Person.newBuilder().setGpgId(pgpId).setName(name).setRelation(Person.Relationship.UNKNOWN).build();
			else p = prs.getPersonByPgpId(pgpId);

			boolean makeFriend = ! ( p.getRelation() == Person.Relationship.FRIEND );
			prs.requestSetFriend(p, makeFriend);

			prs.requestPersonsUpdate(Peers.RequestPeers.SetOption.ALL, Peers.RequestPeers.InfoOption.ALLINFO);

			finish();
		}
	}

	private void updateViews()
	{
		if (serverName == null) return;

		Person.Relationship r;

		if(fromExtIntent)
		{
			r = Person.Relationship.UNKNOWN;
		}
		else
		{
			Person p = getConnectedServer().mRsPeersService.getPersonByPgpId(pgpId);
			name = p.getName();
			r = p.getRelation();
		}

		TextView nameTextView = (TextView) findViewById(R.id.peerNameTextView);
		TextView pgpIdTextView = (TextView) findViewById(R.id.pgpIdTextView);
		Button toggleFriendshipButton = (Button) findViewById(R.id.buttonToggleFriendship);

		nameTextView.setText(name);
		pgpIdTextView.setText(pgpId);

		if ( r.equals(Person.Relationship.YOURSELF) ) toggleFriendshipButton.setVisibility(View.GONE);
		else if ( r.equals(Person.Relationship.FRIEND) ) toggleFriendshipButton.setText(R.string.block_friend);
		else toggleFriendshipButton.setText(R.string.add_as_friend);
	}

	@Override
	public void update()
	{
		updateViews();
	}

	public void onShowPeerQrCodeButtonPressed(View v)
	{
		RsPeersService prs = getConnectedServer().mRsPeersService;

		Person p;
		if(fromExtIntent) p = Person.newBuilder().setGpgId(pgpId).setName(name).setRelation(Person.Relationship.UNKNOWN).build();
		else p = prs.getPersonByPgpId(pgpId);

		Intent i = new Intent();
		i.putExtra(ShowQrCodeActivity.PGP_ID_EXTRA, p.getGpgId());
		i.putExtra(ShowQrCodeActivity.NAME_EXTRA, p.getName());
		startActivity(ShowQrCodeActivity.class, i);
	}
}
