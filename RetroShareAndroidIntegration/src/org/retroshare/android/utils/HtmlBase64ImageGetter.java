/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.retroshare.android.utils;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;

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
			bd = new BitmapDrawable(resources, Util.decodeBase64(src.split(",", 2)[1]));
			bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
		}

		return bd;
	}
}
