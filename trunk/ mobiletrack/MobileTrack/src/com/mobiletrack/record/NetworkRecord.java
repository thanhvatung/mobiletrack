package com.mobiletrack.record;

import java.util.Date;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import com.mobiletrack.config.Config;

public class NetworkRecord extends Record {
	protected int _cellId;
	protected String _mcc;
	protected String _mnc;
	protected int _lac;
	protected String _network;
	protected String _networkOperator;
	protected String _localNumber;
	public double _latitude;
	public double _longitude;

	private static final String NETWORK_HOME = "HOME";
	private static final String NETWORK_ROAM = "ROAM";
	private static final String NETWORK_ROAM_NATIONAL = "ROAMN";

	public NetworkRecord(Context context) {
		super();

		Config.takeSnapShot(context);
		_localNumber = Config.getConfig().getPhoneNumber();
		_mcc = "mcc_unknown";
		_mnc = "mnc_unknown";
		_cellId = -1;
		_lac = -1;
		_networkOperator = "Unknown";
		generateGeneralInfo(context);
		try
		{
			_localNumber = Config.getConfig().getPhoneNumber();
		}
		catch (Exception e)
		{
			Config.takeSnapShot(context);
			_localNumber = Config.getConfig().getPhoneNumber();
		}
		_latitude = 0;
		_longitude = 0;
	}

	// only generate the general information
	// if the phone call has been established
	public void generateGeneralInfo(Context context) {
		//ServiceState servState = new ServiceState();
		//_network = (servState.getRoaming() ? NETWORK_ROAM : NETWORK_HOME);

		TelephonyManager teleMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		
		if (teleMgr != null) {
			//_network = (teleMgr.isNetworkRoaming() ? NETWORK_ROAM : NETWORK_HOME);
			if(!teleMgr.isNetworkRoaming()){
				_network = NETWORK_HOME;
			// roaming is off
			} else if (teleMgr.getSimCountryIso().equals(teleMgr.getNetworkCountryIso())) {
				_network = NETWORK_ROAM_NATIONAL;
			// national roaming
			} else {
				_network = NETWORK_ROAM;
			// international roaming
			}

			
			if (teleMgr.getCellLocation() == null)
			{
				_cellId = -1;
				_lac = -1;
				_mnc = "Unknown";
				_mcc = "Unknown";
				return;
			}
			if (teleMgr.getCellLocation().getClass().equals(GsmCellLocation.class))
			{
				GsmCellLocation loc = (GsmCellLocation)teleMgr.getCellLocation();
				_cellId = loc.getCid();
				_lac = loc.getLac();
			}
			else
			{
				CdmaCellLocation loc = (CdmaCellLocation) teleMgr.getCellLocation();
				_cellId = loc.getBaseStationId();
				_lac = loc.getNetworkId();
			}
			/*List<NeighboringCellInfo> list = teleMgr.getNeighboringCellInfo();
			if (list != null && list.size() > 0) {
				NeighboringCellInfo nCellInfo = list.get(0);
				_cellId = nCellInfo.getCid();
				_lac = nCellInfo.getLac();
			}*/

			String networkOperator = teleMgr.getNetworkOperator();
			_networkOperator = teleMgr.getNetworkOperatorName();
			if ((networkOperator != null) && (networkOperator.length() >= 4)) {
				_mcc = networkOperator.substring(0, 3);
				_mnc = networkOperator.substring(3);
			}
		}
	}
	public String getOperator()
	{
		if (_networkOperator == null)
			_networkOperator = "Unknown";
		return _networkOperator;
	}
	public String toString()
	{
		return "NET" + VERTICAL_BAR + _dateFormat.format(new Date())
		+ VERTICAL_BAR + _localNumber + VERTICAL_BAR + _networkOperator
		+ VERTICAL_BAR + _latitude + VERTICAL_BAR + _longitude
		+ VERTICAL_BAR + _cellId + VERTICAL_BAR + _mcc + VERTICAL_BAR + _mnc
		+ VERTICAL_BAR + _lac + VERTICAL_BAR;
	}
}
