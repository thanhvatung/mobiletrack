package com.mobiletrack.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mobiletrack.R;
import com.mobiletrack.record.LocationRecord;
import com.mobiletrack.service.LocationTrackService;
import com.mobiletrack.ui.CustomerCodeActivity;

public class Config {
	private static Config _config = null;
	private Context _context = null;
	private Hashtable<String, String> _types = null;

	// type defines
	private static String TYPE_BOOL;
	private static String TYPE_INT;
	private static String TYPE_STR;
	private static String TYPE_LIST;
	private static String TYPE_TABLE;
	private static String TYPE_TABLE_IN_LIST;
	private static String TYPE_STR_IN_LIST;

	private static String TYPE_LIST_IN_TABLE;

	// phone number style defines
	private static String STARTS_WITH;
	private static String ENDS_WITH;
	private static String EXACT;
	private static String CONTAINS;

	// tiReceiverType definitions
	public static final int TI_RECEIVER_TYPE_FTP = 0;
	public static final int TI_RECEIVER_TYPE_WEBSERVICE = 1;

	// GPS Interval definitions (defined in scheduler)
	// 0: Do not use interval, we use a huge interval
	// 1: Full time tracking, we use 1 minute (60 * 1000 milliseconds) as
	// interval
	// >1: Set interval as the value indicates, in minutes, we will calculate it
	// into milliseconds
	public static final long GPS_INTERVAL_DO_NOT_USE = 0;// Long.MAX_VALUE;
	public static final long GPS_INTERVAL_FULL_TIME_TRACKING = 60 * 1000;
	public static final long MILLISECONDS_IN_A_WEEK = 7 * 24 * 60 * 60 * 1000;

	// Account Code definitions
	public static final int ACCOUNT_CODE_DO_NOT_USE = 0;
	public static final int ACCOUNT_CODE_ALWAYS_USE = 1;
	public static final int ACCOUNT_CODE_USE_ON_MATCH_ONLY = 2;
	public static final int ACCOUNT_CODE_AUTOMATIC = 3;

	// Special Numbers definitions
	public static final String SPECIAL_NUMBER_911 = "911";

	private static final String TYPE_ATTR = "type";
	//	private static final String NAME_ATTR = "name";
	// The configurations will be save in 2 places:
	// [1] the simple key-value pairs will be saved in getSharedPreferences
	// [2] the list of complex elements will be saved in persistent file
	// the Object could be:
	// <1> a hash table
	// <2> a list of hash tables
	private Hashtable<String, Object> _settingsLists = null;
	private static String _generalSettingsListFileName = null;

	// Snapshot objects
	private Map<String, Object> _prefSnapShot = null;
	private Hashtable<String, Object> _generalSettingsListsSnapShot = null;

	private Config(Context context) {
		_context = context;
		Log.i("FamilyTrack",_context.toString());
		if (_generalSettingsListFileName == null)
			_generalSettingsListFileName = getString(R.string.conf_general_settings_lists_file_name);
		loadLists();
		initElementsTypes();
		initUtilityStrings();
	}

	private void setContext(Context context) {
		_context = context;
	}

	/**
	 * Get the current Config object
	 */
	public static Config getConfig() {
		if (_config == null)
			throw new RuntimeException(
			"need to take a snap shot before getting the Config object");
		return _config;
	}

	/**
	 * Take a snap shot of the current <code>Config</code> object, 2 parts: the
	 * preference and the lists
	 * 
	 * @param context
	 */
	public static void takeSnapShot(Context context) {
		if (_config == null)
			_config = new Config(context);
		else
			_config.setContext(context);
		_config.takeSnapShot();
	}

	@SuppressWarnings("unchecked")
	private void takeSnapShot() {
		_prefSnapShot = (Map<String, Object>) getPref().getAll();
		_generalSettingsListsSnapShot = cloneHashtable(_settingsLists);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Hashtable<String, Object> cloneHashtable(
			Hashtable<String, Object> table) {
		Hashtable<String, Object> newTable = new Hashtable<String, Object>();
		Enumeration<String> keys = table.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			Object elem = table.get(key);
			if (elem instanceof Hashtable)
				newTable.put(key,
						cloneHashtable((Hashtable<String, Object>) elem));
			else if (elem instanceof List)
				newTable.put(key, cloneList((List) elem));
			else if (elem instanceof Boolean)
				newTable.put(key, new Boolean((Boolean) elem));
			else if (elem instanceof Integer)
				newTable.put(key, new Integer((Integer) elem));
			else if (elem instanceof String)
				newTable.put(key, new String((String) elem));
		}
		return newTable;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List cloneList(List list) {
		List newList = new ArrayList();
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Object elem = iter.next();
			if (elem instanceof Hashtable)
				newList.add(cloneHashtable((Hashtable<String, Object>) elem));
			else if (elem instanceof List)
				newList.add(cloneList((List) elem));
			else if (elem instanceof Boolean)
				newList.add(new Boolean((Boolean) elem));
			else if (elem instanceof Integer)
				newList.add(new Integer((Integer) elem));
			else if (elem instanceof String)
				newList.add(new String((String) elem));
		}
		return newList;
	}

	public boolean isAppTerminated() {
		return getBoolean(getString(R.string.conf_dev_app_terminated), false);
	}

	public String getKey() {
		return getString(getString(R.string.conf_dev_key), "");
	}

	public int getTiReceiverType() {
		return getInt(getString(R.string.conf_dev_ti_receiver_type), 0);
	}

	public String getPhoneNumber() {
		String toReturn = CustomerCodeActivity.PHONE_NUMBER;
		if (toReturn == null)
		{
			CustomerCodeActivity.GET_NUMBER(_context);
			toReturn = CustomerCodeActivity.PHONE_NUMBER;
		}
		else
			return toReturn;
		if (toReturn == null)
		{
			CustomerCodeActivity.PHONE_NUMBER = ((TelephonyManager)_context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
			toReturn = CustomerCodeActivity.PHONE_NUMBER;
		}
		else
			return toReturn;
		if (toReturn == null)
		{
			toReturn = "UNKNOWN LOCAL NUMBER";
		}
		return toReturn;
		//		return ((TelephonyManager)_context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
	}

	public String getReceiverServer() {
		return getString(getString(R.string.conf_dev_receiver_server),
		"www.google.com");
	}

	public String getUserName() {
		return getString(getString(R.string.conf_dev_user_name), "username");
	}

	public String getPassword() {
		return getString(getString(R.string.conf_dev_password), "password");
	}

	public String getFTPRemoteDir() {
		return getString(getString(R.string.conf_dev_ftp_remove_dir),
		"ftp_remote_dir");
	}

	public int getConnectionTimeout() {
		return getInt(getString(R.string.conf_dev_connection_timeout), 0);
	}

	public int getTriesBeforeFail() {
		return getInt(getString(R.string.conf_dev_tries_before_fail), 0);
	}

	public int canOverride()
	{
		return getInt ((getString (R.string.conf_dev_override) + getString (R.string.conf_dev_override_state)),  0);
	}

	public boolean isGPSEnabled() {
		return getBoolean(getString(R.string.conf_dev_enable_gps), false);
	}

	public boolean isDataEnabled() {
		return getBoolean(getString(R.string.conf_dev_enable_data), false);
	}

	public boolean isWipeOnSimEnabled() {
		return getBoolean (getString(R.string.conf_dev_enable_sim_wipe), false);
	}
	public boolean networkNotify()
	{
		return getBoolean (getString (R.string.conf_dev_enable_network_notify), false);
	}

	public boolean isSMSEnabled() {
		return getBoolean(getString(R.string.conf_dev_enable_sms), false);
	}

	public boolean isVoiceEnabled() {
		return getBoolean(getString(R.string.conf_dev_enable_voice), false);
	}

	public boolean isCallLocationEnabled() {
		
		return getBoolean(getString(R.string.conf_dev_enable_call_location),
				false);
	}

	public int getCallLocationTimeout() {
		
		return getInt(getString(R.string.conf_dev_call_location_timeout), 0);
	}

	public boolean DisableRoamingUpload() {
		return getBoolean(getString(R.string.conf_dev_disable_roaming_upload),
				true);
	}

	public int getMaxHDilution() {
		return getInt(getString(R.string.conf_dev_max_hdilution), 0);
	}

	public int getMaxVDilution() {
		return getInt(getString(R.string.conf_dev_max_vdilution), 0);
	}

	public boolean isVIGenericAllowed() {
		if (isSpeeding())
			return isVISAllowed() && isVIAllowed();
		else
			return isVIAllowed();
	}

	public boolean isVOGenericAllowed() {
		if (isSpeeding())
			return isVOSAllowed() && isVOAllowed ();
		else
			return isVOAllowed();
	}

	public boolean isTXTGenericAllowed() {
		if (isSpeeding())
			return isTXTSAllowed() && isTXTAllowed();
		else
			return isTXTAllowed();
	}

	public boolean isVIAllowed() {
		return checkGenericDOWItem(R.string.conf_dev_dow_item_vi);
	}

	public boolean isVOAllowed() {

		boolean value = checkGenericDOWItem(R.string.conf_dev_dow_item_vo);
		if (value)
			return true;
		else
			return isHeadSetPlugIn();
	}

	public boolean isTXTAllowed() {
		boolean toReturn = checkGenericDOWItem(R.string.conf_dev_dow_item_txt);
		return toReturn;
	}

	private boolean isVISAllowed() {
		boolean value = getBoolean(getString(R.string.conf_dev_dow_item_vis), true);
		if (value)
			return true;
		else
			return isHeadSetPlugIn();
	}

	private boolean isVOSAllowed() {
		boolean toReturn = getBoolean(getString (R.string.conf_dev_dow_item_vos), true);
		return toReturn; 
	}

	private boolean isTXTSAllowed() {
		boolean toReturn = getBoolean(getString (R.string.conf_dev_dow_item_txts), true);
		return toReturn;
	}

	@SuppressWarnings("unchecked")
	public boolean isNumberInAllowedNumberDisabled(String phoneNumber) {
		if (phoneNumber == null)
			return false;
		List <String> list = (List <String>) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_dev_allowed_number_disabled));
		return isNumberIncluded(phoneNumber, list);
	}

	public synchronized String getQueuedEventRecords() {
		return getString(getString(R.string.event_records), "");
	}

	public synchronized void queueEventRecord(String record) {
		String oldRecords = getQueuedEventRecords();
		if (oldRecords.equals(""))
			setString(getString(R.string.event_records), record);
		else if (oldRecords.contains(record))
			return;
		else
			setString(getString(R.string.event_records), oldRecords
					+ getString(R.string.record_delimiter) + record);
		takeSnapShot(); // this is a write operation, so we need to take a snap
		// shot immediately
	}

	public synchronized void clearEventRecordsQueue() {
		setString(getString(R.string.event_records), "");
		takeSnapShot(); // this is a write operation, so we need to take a snap
		// shot immediately
	}

	/**
	 * Get the interval in millisecond to read GPS information
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public long getGPSInterval() {
		List  list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_dev_dow_list));
		if (list == null || list.size() == 0)
			return GPS_INTERVAL_DO_NOT_USE;
		Hashtable table = getRulingDOWItemTable(list);
		Integer intValue = (Integer) table
		.get(getString(R.string.conf_dev_dow_item_gps));
		if (intValue == null || intValue == 0)
			return GPS_INTERVAL_DO_NOT_USE;
		else if (intValue.intValue() == 1)
			return GPS_INTERVAL_FULL_TIME_TRACKING;
		else
			return (long) (intValue.intValue() * 60 * 1000);
	}

	@SuppressWarnings("rawtypes")
	private boolean checkGenericDOWItem(int rid) {
		if (_generalSettingsListsSnapShot == null)
		{
			return true;
		}
		List list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_dev_dow_list));
		// [1] If there is no entry, everything is allowed
		if (list == null || list.size() == 0)
			return true;

		// [2] Get the ruling entry and check the ruling item
		Hashtable table = getRulingDOWItemTable(list);
		if (table == null)
			return true;
		Boolean boolValue = (Boolean) table.get(_context.getString(rid));
		// [2.1] return true if the item does not exist
		if (boolValue == null)
			return true;
		// [2.2] return the item's value
		else
			return boolValue.booleanValue();
	}

	@SuppressWarnings("rawtypes")
	private Hashtable getRulingDOWItemTable(List list) {
		Iterator iter = list.iterator();
		Hashtable table1 = null;
		Hashtable table2 = null;
		while (iter.hasNext()) {
			table1 = ((table2 == null) ? ((Hashtable) iter.next()) : table2);
			table2 = ((iter.hasNext()) ? ((Hashtable) iter.next()) : null);

			// [2.1] Use table1 if there's only 1 table
			if (table2 == null)
				return table1;

			int dow1 = ((Integer) table1
					.get(getString(R.string.conf_dev_dow_item_dow))).intValue();
			int t1 = ((Integer) table1
					.get(getString(R.string.conf_dev_dow_item_t))).intValue();
			int dow2 = ((Integer) table2
					.get(getString(R.string.conf_dev_dow_item_dow))).intValue();
			int t2 = ((Integer) table2
					.get(getString(R.string.conf_dev_dow_item_t))).intValue();

			int totalMinutes1 = (dow1 * 24 * 60) + t1;
			int totalMinutes2 = (dow2 * 24 * 60) + t2;
			int totalMinutesCur = getCurTimeInMinutes();

			if (totalMinutesCur < totalMinutes1) {
				Hashtable lastTable = (Hashtable) list.get(list.size() - 1);
				return lastTable;
			} else if ((totalMinutes1 <= totalMinutesCur)
					&& (totalMinutesCur < totalMinutes2)) {
				return table1;
			} else {
				if (iter.hasNext())
					continue;
				else
					return table2;
			}
		}
		// Impossible to reach here!!!
		Log.e(_context.getString(R.string.logging_tag),
		"checkGenericDOWItem() Impossible to reach here!!!");
		assert (false);
		return null;
	}

	public static int getCurTimeInMinutes() {
		Calendar c = Calendar.getInstance();
		int day = c.get(Calendar.DAY_OF_WEEK) - 1;
		/** system Sun:1, Mon:2, ... , Sat:7 **/
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);

		int minutes = (day * 24 * 60) + (hour * 60) + minute;
		Log.i("MobileTracker", "getCurTimeInMinutes() " + "day[" + day + "] "
				+ "hour[" + hour + "] " + "minute[" + minute + "] return ("
				+ minutes + ")");

		return minutes;
	}

	public static long getCurTimeInMilliSeconds() {
		long curMilliseconds = (((long) getCurTimeInMinutes()) * 60L * 1000L);
		Log.i("MobileTracker", "getCurTimeInMilliSeconds() return ("
				+ curMilliseconds + ")");
		return curMilliseconds;
	}

	public int getUseAccountCodes() {
		return getInt(getString(R.string.conf_dev_use_account_codes), 0);
	}

	public int getSpeedThreshold() {
		int i = getInt(getString(R.string.conf_dev_speed_threshold), 0);
		return i;
	}

	public int getCurrentSpeed() {
		int i = (int) (LocationTrackService.SPEED *LocationRecord.CONVERSION_FACTOR);
		return i;
	}

	public boolean isSpeeding() {
		return (getCurrentSpeed() > getSpeedThreshold());
		//return true;
	}

	public void setCurrentSpeed(int value) {
		setInt(getString(R.string.current_speed), value);
	}

	public void setHeadSetPlugIn(boolean value) {
		setBoolean(getString(R.string.headset_plugIn), value);
	}

	public boolean isHeadSetPlugIn() {
		return getBoolean(getString(R.string.headset_plugIn), false);
	}

	/**
	 * Check if the given phone number is in the restrict list
	 * 
	 * @param phoneNumber
	 * @return true if it is in the restrict list false if it is not in the
	 *         restrict list
	 */
	@SuppressWarnings("rawtypes")
	public boolean isNumberToRestrict(String phoneNumber) {
		if (phoneNumber.equals(SPECIAL_NUMBER_911))
			return false;

		List list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_dev_restricted_numbers));
		return isNumberIncluded(phoneNumber, list);
	}

	/**
	 * Check if this phone number is in the redirect list.
	 * 
	 * @param phoneNumber
	 * @return the redirect number if this phone number is in the redirect list
	 * @return null if this phone number is not in the redirect list
	 */
	@SuppressWarnings("rawtypes")
	public String getNumberToRedirect(String phoneNumber) {
		List list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_dev_redirect_list));
		if (list == null)
			return null;

		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Hashtable table = (Hashtable) iter.next();
			String style = (String) table
			.get(getString(R.string.conf_dev_phone_number_item_style));
			String baseNumber = (String) table
			.get(getString(R.string.conf_dev_phone_number_item_number));
			baseNumber = baseNumber.replace("-", "");
			boolean result = isNumberIncluded(phoneNumber, style, baseNumber);
			if (result)
				return (String) table
				.get(getString(R.string.conf_dev_redirect_to));
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public boolean isNumToIgnoreByAcctCode(String phoneNumber) {
		List list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_acct_ignore));
		if (list == null)
			return false;

		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			String number = (String) iter.next();
			number = number.replace("-", "");
			// In U.S. people do not need to enter the area code
			// So we check if phoneNumber ends with number or vice versa
			boolean result = (phoneNumber.endsWith(number) || number
					.endsWith(phoneNumber));
			if (result)
				return true;
		}
		return false;
	}

	// check if the phone number matches any phone numbers of the
	// account code items and return the related account code if
	// the match exists
	@SuppressWarnings("rawtypes")
	public String isNumMatchAcctCode(String phoneNumber) {
		List list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_acct_codes));
		if (list == null)
			return null;

		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Hashtable table = (Hashtable) iter.next();
			String acctCode = (String) table
			.get(getString(R.string.conf_acct_code_item_code));
			List numberList = (List) table
			.get(getString(R.string.conf_acct_numbers));
			if (numberList != null && numberList.size() > 0) {
				Iterator iter2 = numberList.iterator();
				while (iter2.hasNext()) {
					String number = (String) iter2.next();
					number = number.replace("-", "");
					boolean result = (phoneNumber.endsWith(number) || number
							.endsWith(phoneNumber));
					if (result) {
						return acctCode;
					}
				}
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	public String nameOfMatchAcctCode(String phoneNumber) {
		List list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_acct_codes));
		if (list == null)
			return null;

		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Hashtable table = (Hashtable) iter.next();
			String acctName = (String) table
			.get(getString(R.string.conf_acct_code_item_name));
			List numberList = (List) table
			.get(getString(R.string.conf_acct_numbers));
			if (numberList != null && numberList.size() > 0) {
				Iterator iter2 = numberList.iterator();
				while (iter2.hasNext()) {
					String number = (String) iter2.next();
					boolean result = (phoneNumber.endsWith(number) || number
							.endsWith(phoneNumber));
					if (result) {
						return acctName;
					}
				}
			}
		}
		return null;
	}

	public static class AccountCodeItem {
		public static class MatterCode {
			String _code;
			String _name;

			public String getName() {
				return _name;
			}

			public String getCode() {
				return _code;
			}
		}

		String _name;
		String _code;
		List<String> _numbers;
		List<MatterCode> _matterCodes;

		public String getName() {
			return _name;
		}

		public String getCode() {
			return _code;
		}

		public List<MatterCode> getMatterCodeList() {
			return _matterCodes;
		}
	}

	@SuppressWarnings("rawtypes")
	public List getTimeCategories() {
		return ((List) (this._settingsLists
				.get(getString(R.string.conf_dev_time_category))));
		// Iterator itr = this._settingsLists.keySet().iterator();
		// Iterator itr = this._types.keySet().iterator();
		// List li = new ArrayList();
		// while (itr.hasNext())
		// li.add(itr.next());
		// return li;
	}

	@SuppressWarnings("rawtypes")
	public List getExpenseCategories() {
		return (List) _settingsLists
		.get(getString(R.string.conf_dev_expense_category));
	}



	@SuppressWarnings({ "rawtypes", "unused" })
	public Hashtable<String, AccountCodeItem> getAcctCodeItemMap() {
		List list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_acct_codes));
		if (list == null)
			return null;

		int size = list.size();
		Hashtable<String, AccountCodeItem> accountCodeItemMap = new Hashtable<String, AccountCodeItem>();
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Hashtable table = (Hashtable) iter.next();
			AccountCodeItem acctCodeItem = new AccountCodeItem();
			acctCodeItem._code = (String) table
			.get(getString(R.string.conf_acct_code_item_code));
			acctCodeItem._name = (String) table
			.get(getString(R.string.conf_acct_code_item_name));

			// copy numbers:
			acctCodeItem._numbers = new ArrayList<String>();
			List numberList = (List) table
			.get(getString(R.string.conf_acct_numbers));
			if (numberList != null && numberList.size() > 0) {
				Iterator iterNumber = numberList.iterator();
				while (iterNumber.hasNext()) {
					String number = (String) iterNumber.next();
					acctCodeItem._numbers.add(number);
				}
			}

			// copy matter codes
			acctCodeItem._matterCodes = new ArrayList<AccountCodeItem.MatterCode>();
			List matterCodeList = (List) table
			.get(getString(R.string.conf_acct_matter_codes));
			if (matterCodeList != null && matterCodeList.size() > 0) {
				Iterator iterMatterCode = matterCodeList.iterator();
				while (iterMatterCode.hasNext()) {
					Hashtable matterCodeTable = (Hashtable) iterMatterCode
					.next();
					AccountCodeItem.MatterCode matterCode = new AccountCodeItem.MatterCode();
					matterCode._code = (String) matterCodeTable
					.get(getString(R.string.conf_acct_matter_code_item_code));
					matterCode._name = (String) matterCodeTable
					.get(getString(R.string.conf_acct_matter_code_item_name));
					acctCodeItem._matterCodes.add(matterCode);
				}
			}

			accountCodeItemMap.put(acctCodeItem._name, acctCodeItem);
		}

		return accountCodeItemMap;
	}

	@SuppressWarnings("rawtypes")
	private boolean isNumberIncluded(String number, List list) {
		if (list == null)
			return false;

		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Hashtable table = (Hashtable) iter.next();
			String style = (String) table
			.get(getString(R.string.conf_dev_phone_number_item_style));
			String baseNumber = (String) table
			.get(getString(R.string.conf_dev_phone_number_item_number));
			baseNumber = baseNumber.replace("-", "");
			boolean result = isNumberIncluded(number, style, baseNumber);
			if (result)
				return result;
		}
		return false;
	}

	private boolean isNumberIncluded(String number, String style,
			String baseNumber) {
		if (style.equals(EXACT)) {
			return number.equals(baseNumber);
		} else if (style.equals(CONTAINS)) {
			return number.contains(baseNumber);
		} else if (style.equals(STARTS_WITH)) {
			return number.startsWith(baseNumber);
		} else if (style.equals(ENDS_WITH)) {
			return number.endsWith(baseNumber);
		} else {
			return false;
		}
	}

	/**
	 * Terminate: Make the application irresponsible
	 */
	public void setTerminated() {
		setBoolean(getString(R.string.conf_dev_app_terminated), true);
	}

	/**
	 * Update the Config with the configuration file indicated by the urlStr
	 * 
	 * @param urlStr
	 *            the url of the configuration file
	 */
	public void update(String urlStr) {
		try {
			CustomizedDefaultHandler handler = new CustomizedDefaultHandler();
			SAXParserFactory.newInstance().newSAXParser()
			.parse(urlStr, handler);
			Log.i("WebService","url: "+urlStr);
		} catch (Exception e) {
			Log.i("WebService",
					"Got exp while parsing the xml [" + urlStr + "]");
		}
	}

	public void update(InputStream is) {
		try {
			CustomizedDefaultHandler handler = new CustomizedDefaultHandler();
			SAXParserFactory.newInstance().newSAXParser().parse(is, handler);
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got exp while parsing the xml inputstream [" + is + "]");
		}
	}

	/**
	 * Initialize the types of each element by parsing the strings.xml from the
	 * assets folder, note that the assets\strings.xml should be identical to
	 * the res\values\strings.xml, we could not take it from the
	 * "res\values\strings.xml"
	 */
	private void initElementsTypes() {
		_types = new Hashtable<String, String>();
		TYPE_BOOL = getString(R.string.type_boolean);
		TYPE_INT = getString(R.string.type_int);
		TYPE_STR = getString(R.string.type_string);
		TYPE_TABLE = getString(R.string.type_table);
		TYPE_LIST = getString(R.string.type_list);
		TYPE_TABLE_IN_LIST = getString(R.string.type_table_in_list);
		TYPE_STR_IN_LIST = getString(R.string.type_str_in_list);
		TYPE_LIST_IN_TABLE = getString(R.string.type_list_in_table);

		InputStream stringsIS = null;
		try {
//			Log.i("FamilyTrack",);
			stringsIS = _context.getAssets().open("strings.xml");
			if (stringsIS != null) {
				TypeDefaultHandler handler = new TypeDefaultHandler();
				SAXParserFactory.newInstance().newSAXParser()
				.parse(stringsIS, handler);
			}
			stringsIS.close();
		} catch (Exception e) {
			Log.e("FamilyTrack",
			"Got exp while parsing the xml [R.xml.strings]"+e.toString());
			try {
				if (stringsIS != null)
					stringsIS.close();
			} catch (Exception ee) {
			}
		}
	}

	/**
	 * The xml default handler to parse the type information from the
	 * strings.xml
	 */
	private class TypeDefaultHandler extends DefaultHandler {
		private String _currentType;

		public TypeDefaultHandler() {
			super();
			_currentType = "";
		}

		public void endDocument() {
			_currentType = "";
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) {
			try {
				if (attributes.getIndex(TYPE_ATTR) != -1) {
					_currentType = attributes.getValue(TYPE_ATTR);
				}
			} catch (Exception e) {
				Log.e(getString(R.string.logging_tag),
						"Got exp in startElement() localName [" + localName
						+ "] of [" + this.getClass().getName() + "]");
			}
		}

		public void characters(char[] ch, int start, int length) {
			try {
				if (!_currentType.equals("")) {
					String currentValue = new String(ch, start, length);
					_types.put(currentValue, _currentType);
				}
			} catch (Exception e) {
				Log.e(getString(R.string.logging_tag),
						"Got exp in characters() value ["
						+ new String(ch, start, length) + "] of ["
						+ this.getClass().getName() + "]");

			}
		}

		public void endElement(String uri, String localName, String qName) {
			_currentType = "";
		}
	}

	private void initUtilityStrings() {
		if (EXACT == null)
			EXACT = getString(R.string.conf_dev_phone_number_item_style_exact);
		if (STARTS_WITH == null)
			STARTS_WITH = getString(R.string.conf_dev_phone_number_item_style_starts_with);
		if (ENDS_WITH == null)
			ENDS_WITH = getString(R.string.conf_dev_phone_number_item_style_ends_with);
		if (CONTAINS == null)
			CONTAINS = getString(R.string.conf_dev_phone_number_item_style_contains);
	}

	private SharedPreferences.Editor getEditor() {
		SharedPreferences pref = getPref();
		return pref.edit();
	}

	private SharedPreferences getPref() {
		return _context.getSharedPreferences(
				getString(R.string.key_shared_pref), Activity.MODE_PRIVATE);
	}

	private void setInt(String key, int value) {
		SharedPreferences.Editor editor = getEditor();
		editor.putInt(key, value);
		editor.commit();
	}

	private int getInt(String key, int defValue) {
		if (_prefSnapShot == null)
			return defValue;
		Integer intValue = (Integer) _prefSnapShot.get(key);
		if (intValue != null)
			return intValue.intValue();
		else
			return defValue;
	}

	private void setString(String key, String value) {
		SharedPreferences.Editor editor = getEditor();
		editor.putString(key, value);
		editor.commit();
	}

	private String getString(String key, String defValue) {
		if (_prefSnapShot == null)
			return defValue;
		String strValue = (String) _prefSnapShot.get(key);
		if (strValue != null)
			return strValue;
		else
			return defValue;
	}

	private void setBoolean(String key, boolean value) {
		SharedPreferences.Editor editor = getEditor();
		editor.putBoolean(key, value);
		editor.commit();
	}

	private boolean getBoolean(String key, boolean defValue) {
		if (_prefSnapShot == null)
			return defValue;
		Boolean boolValue = (Boolean) _prefSnapShot.get(key);
		if (boolValue != null)
			return boolValue.booleanValue();
		else
			return defValue;
	}

	/**
	 * The default xml handler to parse the values from the configuration file
	 */
	private class CustomizedDefaultHandler extends DefaultHandler {
		private String _currentElem;
		@SuppressWarnings({ "rawtypes" })
		private List _currentList;
		@SuppressWarnings({ "rawtypes" })
		private Hashtable _currentTable;
		@SuppressWarnings("rawtypes")
		private Stack _containerStack;
		StringBuffer str = null;

		@SuppressWarnings({ "rawtypes" })
		private CustomizedDefaultHandler() {
			super();
			_currentElem = "";
			_currentList = null;
			_currentTable = null;
			_containerStack = new Stack();
		}

		public void startDocument() {
		}

		public void endDocument() {
			saveLists();
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) {
			str = new StringBuffer();
			try {
				_currentElem = localName;
				if (_currentElem.equals("DOWItem"))
				{
					System.out.println ("Got here!!!");
				}

				String type = _types.get(localName);
				if (type == null)
					return;

				if (type.equals(TYPE_LIST)) {
					List list = new ArrayList();
					_settingsLists.put(localName, list);
					_currentList = list;
					_containerStack.push(_currentList);
				}
				else if (type.equals(TYPE_LIST_IN_TABLE)) {
					List list = new ArrayList();
					if (_currentTable != null)
						_currentTable.put(localName, list);
					_currentList = list;
					_containerStack.push(_currentList);
				}
				else if (type.equals(TYPE_TABLE_IN_LIST))
				{
					Hashtable<String, Object> table = new Hashtable<String, Object>();
					int numOfAttr = attributes.getLength();
					for (int i = 0; i < numOfAttr; i++) {
						String attrName = attributes.getLocalName(i);
						String valueStr = attributes.getValue(attrName);
						String attrType = _types.get(attrName);
						if (attrName.equals("TXT"))
						{
							System.out.println ("Got here!!!");
						}
						if (attrType != null)
						{
							if (attrType.equals(TYPE_BOOL)) {
								boolean value = Boolean.parseBoolean(valueStr);
								table.put(attrName, new Boolean(value));
							} else if (attrType.equals(TYPE_INT)) {
								int value = Integer.parseInt(valueStr);
								table.put(attrName, new Integer(value));
							} else if (attrType.equals(TYPE_STR)) {
								String value = valueStr;
								table.put(attrName, value);
							}
						}
					}
					if (_currentList != null)
						_currentList.add(table);
					_currentTable = table;
					_containerStack.push(_currentTable);
				}
				else if (type.equals(TYPE_TABLE))
				{
					Hashtable<String, Object> table = new Hashtable<String, Object>();
					int numOfAttr = attributes.getLength();
					for (int i = 0; i < numOfAttr; i++) {
						String attrName = attributes.getLocalName(i);
						String valueStr = attributes.getValue(attrName);
						String attrType = _types.get(attrName);
						if (attrType.equals(TYPE_BOOL)) {
							boolean value = Boolean.parseBoolean(valueStr);
							table.put(attrName, new Boolean(value));
							setBoolean (_currentElem+attrName, value);
						} else if (attrType.equals(TYPE_INT)) {
							int value = Integer.parseInt(valueStr);
							table.put(attrName, new Integer(value));
							setInt (_currentElem+attrName, value);
						} else if (attrType.equals(TYPE_STR)) {
							String value = valueStr;
							table.put(attrName, value);
							setString (_currentElem+attrName, value);
						}
					}
					_currentTable = table;
					//					_settingsLists.put(localName, _currentTable);
				}
			} catch (Exception e) {
				Log.e(getString(R.string.logging_tag),
						"got exception in startElement() tag <" + localName
						+ ">");
				Log.e(getString(R.string.logging_tag),
						"got exception while parsing xml exp ["
						+ e.getClass().getName() + "] msg ["
						+ e.getMessage() + "]");
			}
		}

		@SuppressWarnings("unchecked")
		public void characters(char[] ch, int start, int length) {
			try {
				String type = _types.get(_currentElem);
				if (type == null)
					return;
				
				str.append(ch,start,length);
				String valueStr = str.toString();
				
				if (type.equals(TYPE_BOOL)) {
					boolean value = Boolean.parseBoolean(valueStr);
					setBoolean(_currentElem, value);
				} else if (type.equals(TYPE_INT)) {
					int value = Integer.parseInt(valueStr);
					setInt(_currentElem, value);
				} else if (type.equals(TYPE_STR)) {
					setString(_currentElem, valueStr);
				} else if (type.equals(TYPE_LIST)) {
				} else if (type.equals(TYPE_STR_IN_LIST)) {
					if (_currentList != null)
						_currentList.add(valueStr);
				}
			} catch (Exception e) {
				Log.e(getString(R.string.logging_tag),
						"got exception in characters() tag <" + _currentElem
						+ ">");
				Log.e(getString(R.string.logging_tag),
						"got exception while parsing xml exp ["
						+ e.getClass().getName() + "] msg ["
						+ e.getMessage() + "]");
			}
		}

		public void endElement(String uri, String localName, String qName) {
			try {
				_currentElem = "";

				String type = _types.get(localName);
				if ((type != null)
						&& (type.equals(TYPE_LIST)
								|| type.equals(TYPE_TABLE_IN_LIST) || type
								.equals(TYPE_LIST_IN_TABLE))) {
					popContainer();
				}
				
			} catch (Exception e) {
				Log.e(getString(R.string.logging_tag),
						"got exception in endElement() tag <" + localName + ">");
				Log.e(getString(R.string.logging_tag),
						"got exception while parsing xml exp ["
						+ e.getClass().getName() + "] msg ["
						+ e.getMessage() + "]");
			}
		}

		@SuppressWarnings("rawtypes")
		private void popContainer() {
			_currentTable = null;
			_currentList = null;

			_containerStack.pop();
			Object obj = null;
			if (_containerStack.size() > 0)
				obj = _containerStack.peek();
			if (obj != null) {
				if (obj instanceof List) {
					_currentList = (List) obj;

				} else if (obj instanceof Hashtable) {
					_currentTable = (Hashtable) obj;
				}
			}
		}
	}

	/**
	 * Save list information to the persistent file
	 */
	private void saveLists() {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = _context.openFileOutput(_generalSettingsListFileName,
					Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(_settingsLists);
			oos.close();
			fos.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while saving the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
			try {
				if (fos != null)
					fos.close();
			} catch (Exception e1) {
			}
			try {
				if (oos != null)
					oos.close();
			} catch (Exception e2) {
			}
		}
	}

	/**
	 * Load the list information from the persistent file
	 */
	@SuppressWarnings("unchecked")
	private void loadLists() {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = _context.openFileInput(_generalSettingsListFileName);
			ois = new ObjectInputStream(fis);
			_settingsLists = (Hashtable<String, Object>) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while loading the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
			try {
				if (fis != null)
					fis.close();
			} catch (Exception e1) {
			}
			try {
				if (ois != null)
					ois.close();
			} catch (Exception e2) {
			}
		}

		if (_settingsLists == null)
			_settingsLists = new Hashtable<String, Object>();
	}

	/**
	 * Util method getString()
	 */
	private String getString(int rid) {
		return _context.getString(rid);
	}

	@SuppressWarnings("rawtypes")
	public Vector<GPSItemInfo> getGPSOnTime() {
		// return me the time needed when gps is on (interval >=1)
		// return value: time to next gps on time
		// for example: now tuesday 8:00am GPS: 10 on wednesday 9:00 return 25 *
		// 60 * 60* 1000
		// now tuesday: 8:AM Gps on monday 8:AM return: 6 * 24 * 60 * 60 * 1000
		List list = (List) _generalSettingsListsSnapShot
		.get(getString(R.string.conf_dev_dow_list));
		if (list == null || list.size() == 0) {
			// nothing to do
			return null;
		}

		// [1] collect the GPS Item information
		Vector<GPSItemInfo> gpsItems = new Vector<GPSItemInfo>();
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Hashtable table = (Hashtable) iter.next();
			long interval = 0;
			int DOW = ((Integer) table
					.get(getString(R.string.conf_dev_dow_item_dow))).intValue();
			int T = ((Integer) table
					.get(getString(R.string.conf_dev_dow_item_t))).intValue();
			int startUpTime = (DOW * 24 * 60 + T) * 60 * 1000;
			Integer gpsDuration = ((Integer) table
					.get(getString(R.string.conf_dev_dow_item_gps))).intValue();

			if (gpsDuration == 0)
				interval = GPS_INTERVAL_DO_NOT_USE;
			if (gpsDuration == 1)
				interval = GPS_INTERVAL_FULL_TIME_TRACKING;
			else
				interval = (long) (gpsDuration * 60 * 1000);

			GPSItemInfo curInfo = new GPSItemInfo(startUpTime,
					MILLISECONDS_IN_A_WEEK, interval);
			gpsItems.add(curInfo);
		}
		sortGPSItems(gpsItems); // sort it in case the server send bad
		// information

		// [2] adjust GPS Items' durations
		int gpsItemsSize = gpsItems.size();
		if (gpsItemsSize > 1) {
			for (int i = 0; i < gpsItemsSize; i++) {
				GPSItemInfo curInfo = gpsItems.elementAt(i);
				if (i == 0) {
					GPSItemInfo lastInfo = gpsItems.lastElement();
					long duration = MILLISECONDS_IN_A_WEEK
					- lastInfo.getStartupTime()
					+ curInfo.getStartupTime();
					lastInfo.setDuration(duration);
				} else if (i > 0) {
					GPSItemInfo prevInfo = gpsItems
					.elementAt(i - 1);
					long duration = curInfo.getStartupTime()
					- prevInfo.getStartupTime();
					prevInfo.setDuration(duration);
				}
			}
		}

		// [3] remove the GPS Items with interval 0s
		for (int i = 0; i < gpsItems.size();) {
			GPSItemInfo info = gpsItems.elementAt(i);
			if (info.getInterval() == 0)
				gpsItems.remove(i);
			else
				i++;
		}

		if (gpsItems.size() == 0)
			return null;
		else
			return gpsItems;
	}

	public static void sortGPSItems(Vector<GPSItemInfo> gpsItems) {
		if (gpsItems == null || gpsItems.size() <= 1)
			return;

		for (int i = 0; i < gpsItems.size(); i++) {
			GPSItemInfo info1 = gpsItems.elementAt(i);
			for (int j = i; j < gpsItems.size(); j++) {
				GPSItemInfo info2 = gpsItems.elementAt(j);
				if (info1.getStartupTime() > info2.getStartupTime()) {
					gpsItems.setElementAt(info2, i);
					gpsItems.setElementAt(info1, j);
					info1 = info2;
				}
			}
		}
	}

	public static class GPSItemInfo {
		// everything is in millisecond
		private int _startupTime;
		private long _duration;
		private long _interval;

		public GPSItemInfo(int startupTime, long duration, long interval) {
			_startupTime = startupTime;
			_duration = duration;
			_interval = interval;
		}

		public int getStartupTime() {
			return _startupTime;
		}

		public long getDuration() {
			return _duration;
		}

		public long getInterval() {
			return _interval;
		}

		public void setDuration(long duration) {
			_duration = duration;
		}
	}

	public String getOverridePassword() {
		return getString(getString (R.string.conf_dev_override) + getString (R.string.conf_dev_override_password), "");
	}

	public String getAutoReplyMessage() {
		return getString (getString (R.string.conf_dev_autoreply), "");
	}
}
