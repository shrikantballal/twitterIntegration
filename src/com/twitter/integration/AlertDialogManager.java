package com.twitter.integration;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * The AlertDialog class.
 * 
 * @author Shrikant Ballal
 * 
 */
public class AlertDialogManager {

	/**
	 * Shows the alertDialog with corresponding message.
	 * 
	 * @param context
	 *            the context on which dialog will be shown
	 * @param title
	 *            the title of dialog
	 * @param message
	 *            the body of dialog
	 */
	public void showAlertDialog(Context context, String title, String message) {
		AlertDialog alertDialog = new AlertDialog.Builder(context).create();

		// Setting Dialog Title
		alertDialog.setTitle(title);

		// Setting Dialog Message
		alertDialog.setMessage(message);

		alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				});
		alertDialog.show();
	}
}
