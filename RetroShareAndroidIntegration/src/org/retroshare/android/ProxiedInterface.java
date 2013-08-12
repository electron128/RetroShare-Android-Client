package org.retroshare.android;

/**
 * @author G10h4ck
 * This interface should be implemented by Proxied Components
 */
public interface ProxiedInterface
{
	/**
	 * @return True if the proxy is bound to the backend false otherwise
	 */
	public boolean isBound();

	/**
	 * @return RetroShare backend proxy
	 */
	public RetroShareAndroidProxy getRsProxy();

	/**
	 * @return The actual server if bound, null otherwise
	 */
	public RsCtrlService getConnectedServer();
}
