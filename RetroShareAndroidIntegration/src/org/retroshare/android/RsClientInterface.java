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

/**
 * This interface should be implemented by components that needs to interact with rscore
 */
public interface RsClientInterface //TODO: find a better name for this interface
{
	public static final String SERVER_NAME_EXTRA_KEY = "org.retroshare.android.intent_extra_keys.serverName";
	/**
	 * @returns The actual server if bound
	 * @throws RuntimeException if is not bound
	 */
	public RsCtrlService getConnectedServer();
}
