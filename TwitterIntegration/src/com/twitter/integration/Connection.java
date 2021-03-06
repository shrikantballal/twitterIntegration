package com.twitter.integration;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class Connection {
	private Context _context;

	public Connection(Context context) {
		this._context = context;
	}

	/**
	 * Checks if device is connected to internet
	 * 
	 * @return true if connected, false otherwise
	 */
	public boolean isConnectedToInternet() {
		ConnectivityManager connectivity = (ConnectivityManager) _context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null)
				for (int i = 0; i < info.length; i++)
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						Log.d("Network",
								"NETWORKNAME: " + info[i].getTypeName());
						return true;
					}
		}
		return false;
	}
}
