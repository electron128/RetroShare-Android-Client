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

package org.retroshare.android;


import android.os.Handler;

import org.retroshare.android.RsCtrlService.RsMessage;

/**
 *	This is an abstract class used as base for received messages handlers   
 */
public abstract class RsMessageHandler extends Handler implements Runnable
{
	
	/**
	 * Should be implemented in child class, it is a callback called by run() when a message to handle is received
	 * @param msg The message to handle
	 */
	protected void rsHandleMsg(RsMessage msg) {}
	
	/**
	 * Contain the message to handle
	 */
	private RsMessage mMsg;
	
	/**
	 * Set the message to handle
	 * @param m message to set
	 */
	public void setMsg(RsMessage m) { mMsg = m; }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() { rsHandleMsg(mMsg); }
}
