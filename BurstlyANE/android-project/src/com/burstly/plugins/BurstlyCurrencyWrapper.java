/*
 Copyright 2014 Burstly, Inc.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.burstly.plugins;

import android.app.Activity;
import android.util.Log;

import com.adobe.fre.FREContext;

import com.burstly.lib.currency.CurrencyManager;

public class BurstlyCurrencyWrapper {
	
	private static Activity mActivity = null;
	private static FREContext mAIRContext = null;
	
	private static CurrencyManager mCurrencyManager = null;
	
	/*****************************************************************/
	/* ANDROID JAVA METHODS - MUST BE CALLED WITHIN JAVA ENVIRONMENT */
	/*****************************************************************/
	
	/*
	 * Initialises BurstlyCurrencyWrapper. Must be called before any views are created in your activity.
	 * 
	 *  @param	aActivity	The main activity for your app
	 */
	public static void init(Activity aActivity) {
		mActivity = aActivity;
	}
	
	/*
	 * Sets the AIR Context - needed for callbacks
	 * 
	 *  @param	aContext	The context to save
	 */
	public static void setAIRContext(FREContext aContext) {
		mAIRContext = aContext;
	}
	
	
    /*
     * Helper method for error checking and messaging. These are here to prevent null pointer exceptions and crashes if the
     * plugin JNI methods are called without the plugin being initialised. 
     */
	
	private static boolean isPluginInitialised() {
		if (mActivity == null) {
			Log.e("BurstlyCurrency", "ERROR: The plugin has not been initialised with your main activity. BurstlyCurrencyWrapper.init(Activity aActivity) MUST be called before any currency-related methods are called.");
			return false;
		}
		if (mCurrencyManager == null) {
			Log.e("BurstlyCurrency", "ERROR: The plugin has not been initialised with your publisherId. BurstlyCurrency.initialize(string publisherId, string userId) MUST be called before any other BurstlyCurrency methods are called.");
			return false;
		}
		return true;
	}
	
	/*
	 * These methods just update the currency balance on lifecycle methods for convenience. Note that these MUST be called in the app's Activity
	 * lifecycle methods for this to occur. Sample below:
	 * 
	 * 		@Override
	 * 		protected void onPause() {
	 * 			BurstlyCurrencyWrapper.onPauseActivity(this);
	 * 			super.onPause();
	 * 		}
	 * 
	 * 		@Override
	 * 		protected void onResume() {
	 * 			BurstlyCurrencyWrapper.onResumeActivity(this);
	 * 			super.onResume();
	 * 		}
	 * 
	 * 		@Override
	 * 		protected void onDestroy() {
	 * 			BurstlyCurrencyWrapper.onDestroyActivity(this);
	 * 			super.onDestroy();
	 * 		}
	 */
	public static void onPauseActivity(Activity aActivity) 		{	activityLifecycleHelper(aActivity);		}
	public static void onResumeActivity(Activity aActivity) 	{	activityLifecycleHelper(aActivity);		}
	public static void onDestroyActivity(Activity aActivity) 	{	activityLifecycleHelper(aActivity);		}
	private static void activityLifecycleHelper(Activity aActivity)		{	
		if ((mActivity != null) && (mCurrencyManager != null)) 
			updateBalancesFromServer();
	}
	
	
	/*
	 * Initializes the BurstlyCurrency plugin. This method *must* be called before any other BurstlyCurrency method is called. You must pass in 
	 * publisherId. userId may be passed in as an empty string ("") if you would like to use the default userId handled by BurstlyCurrency. DO NOT 
	 * pass in NULL if there is no userId.
	 * 
	 * @param	publisherId		the Burstly publisher ID for the app.
	 * @param	userId			a unique identifier for the user in your app. if left blank it will use Burstly's default user ID.
	 */
	public static void initialize(final String publisherId, final String userId) {
		if (mCurrencyManager != null) return;
		
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mCurrencyManager = new CurrencyManager(); 
				
				if (userId.length() == 0)
					mCurrencyManager.initManager(mActivity, publisherId);
				else
					mCurrencyManager.initManager(mActivity, publisherId, userId); 
				
				mCurrencyManager.addCurrencyListener(new BurstlyCurrencyListener());
				
				BurstlyCurrencyWrapper.updateBalancesFromServer();
			}
			
		});
	}
	
	
	/*
	 * Returns the currency balance for the currency name passed in the parameters held in the local cache. This is updated from the server upon 
	 * calling updateBalancesFromServer().
	 * 
	 * @param	currency	the machine-readable currency name for the currency in question
	 */
	public static int getBalance(String currency) {
		if (!isPluginInitialised()) return 0;
		
		return mCurrencyManager.getBalance(currency);
	}
	
	/*
	 * Increases the currency balance for the passed currency by the passed amount. This updates the local currency cache and also updates the 
	 * Burstly server balance as well.
	 * 
	 * @param	currency	the machine-readable currency name for the currency in question
	 * @param	amount		the amount of currency to deduct from the user's balance
	 */
	public static void increaseBalance(final String currency, final int amount) {
		if (!isPluginInitialised()) return;
		
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mCurrencyManager.increaseBalance(amount, currency);
				BurstlyCurrencyWrapper.updateBalancesFromServer();
			}
			
		});
	}
	
	/*
	 * Decreases the currency balance for the passed currency by the passed amount. This updates the local currency cache and also updates the 
	 * Burstly server balance as well.
	 * 
	 * @param	currency	the machine-readable currency name for the currency in question
	 * @param	amount		the amount of currency to deduct from the user's balance
	 */
	public static void decreaseBalance(final String currency, final int amount) {
		if (!isPluginInitialised()) return;

		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mCurrencyManager.decreaseBalance(amount, currency);
				BurstlyCurrencyWrapper.updateBalancesFromServer();
			}
			
		});
	}
	
	/*
	 * Initiates a request to update the currency balances for all currencies from the Burstly server. You must register a callback using the 
	 * methods below to receive notifications that this method succeeded / failed.
	 */
	public static void updateBalancesFromServer() {
		if (!isPluginInitialised()) return;
		
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				try {
					mCurrencyManager.checkForUpdate();
				} catch (Exception e) {
					BurstlyCurrencyWrapper.sendCallback(false);
				} 
			}
			
		});
	}
	
	
	/********************************************************************************/
	/* INTERNAL JAVA METHODS - CALLED BY INTERNAL LOGIC TO FACILITATE FUNCTIONALITY */
	/********************************************************************************/              
    
	/*
	 * Invokes dispatchStatusEventAsync to send a callback to the AS layer.
	 * 
	 * @param	success		Whether the currency update request was successful or not
	 */
	protected static void sendCallback(final boolean success) {
		if (mAIRContext == null) return;
		
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				
				mAIRContext.dispatchStatusEventAsync(success ? 	"BurstlyCurrencyEventUpdateSucceeded" : 
																"BurstlyCurrencyEventUpdateFailed", "");
				
			}
			
		});
	}
	
	
}
