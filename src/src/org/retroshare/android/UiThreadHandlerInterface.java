package org.retroshare.android;


public interface UiThreadHandlerInterface
{
	/**
	 * Posts a Runnable Object to the UI Thread and calls r.run() in UI Thread
	 * @param r Runnable to run in UI Thread
	 */
	public void postToUiThread(Runnable r);
}
