package com.mobiletrack.record;

import java.util.Date;

import com.mobiletrack.config.Config;




public class ExpenseRecord extends Record {

	private static final long serialVersionUID = -5340645990305649522L;

	public boolean Submit = false;

	public boolean Time = false; // 0 = Time; 1 = Expense
	public int RecordId = 0;
	public boolean TimerStarted = false;
	public long EventTimeMS = 0;
	public long Duration = 0;
	public Date StartTime;
	public double Cost = 0.0;

	public String EventCategory = "";
	public Date theDate;
	public String AccountCode = "";
	public String MatterCode = "";
	public String Comment = "";
	public final String TIME = "TIME";
	public final String EXPENSE = "EXP";

	public long LastRecordedTime = 0;

	public String _localNumber;

	// Debugging constructor
	public ExpenseRecord() {
		theDate = new Date();
		EventCategory = "";
		AccountCode = "";
		MatterCode = "";
		Comment = "";
		_localNumber = (Config.getConfig().getPhoneNumber());
	}

	public ExpenseRecord(String category, String account, String matter,
			String theComment) {
		theDate = new Date();
		EventCategory = category;
		AccountCode = account;
		MatterCode = matter;
		Comment = theComment;
		_localNumber = (Config.getConfig().getPhoneNumber());
	}


	// <EventType>|<EventDateTime>|<Chargeable>|<LocalNumber>|<EventCategory>|<ClientCode>|<MatterCode>|<Comment>
	public String toString() {

		if (Duration >= 0) {
			if (TimerStarted) {
				Duration = System.currentTimeMillis() + Duration
						- (EventTimeMS);
			}
		}
		if (Time)
			return TIME + VERTICAL_BAR + _dateFormat.format(theDate)
					+ VERTICAL_BAR + Duration / 1000 + VERTICAL_BAR
					+ _localNumber + VERTICAL_BAR + EventCategory
					+ VERTICAL_BAR + AccountCode + VERTICAL_BAR + MatterCode
					+ VERTICAL_BAR + Comment;
		else
			return EXPENSE + VERTICAL_BAR + _dateFormat.format(theDate)
					+ VERTICAL_BAR + Cost + VERTICAL_BAR + _localNumber
					+ VERTICAL_BAR + EventCategory + VERTICAL_BAR + AccountCode
					+ VERTICAL_BAR + MatterCode + VERTICAL_BAR + Comment;
	}
}
