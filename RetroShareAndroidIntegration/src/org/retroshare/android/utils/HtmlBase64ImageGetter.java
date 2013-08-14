package org.retroshare.android.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;

/**
 * @author G10h4ck
 */
public class HtmlBase64ImageGetter implements Html.ImageGetter
{
	public String TAG() { return "HtmlBase64ImageGetter"; }

	Resources resources;

	public HtmlBase64ImageGetter(Resources res) { resources = res; }

	@Override
	public Drawable getDrawable(String src) //format of accepted src: "data:image/png;base64,iVBORw0KGgoAAA........."
	{
		BitmapDrawable bd = null;

		if( src.split(":",2)[0].startsWith("data") )
		{
			bd = new BitmapDrawable(resources, util.decodeBase64(src.split(",",2)[1]));
			bd.setBounds(0,0,bd.getIntrinsicWidth(),bd.getIntrinsicHeight());
		}

		return bd;
	}
}
