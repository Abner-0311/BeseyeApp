package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import com.app.beseye.setting.CamSettingMgr;
import com.app.beseye.util.NetworkMgr;

import android.app.Application;

@ReportsCrashes(formKey= HOCKEY_APP_ID,
				logcatArguments = { "-t", "2500", "-v", "long", "iKalaApp:W", "*:S" },
				mode = ReportingInteractionMode.TOAST,
				forceCloseDialogAfterToast = false,
				resToastText = R.string.crash_toast_text,
				customReportContent = { ReportField.PACKAGE_NAME, ReportField.APP_VERSION_CODE, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.BUILD, ReportField.TOTAL_MEM_SIZE,
										ReportField.AVAILABLE_MEM_SIZE, ReportField.STACK_TRACE, ReportField.THREAD_DETAILS, ReportField.LOGCAT, ReportField.EVENTSLOG, ReportField.DUMPSYS_MEMINFO})

public class BeseyeApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		
		ACRA.init(this);
		ACRA.getErrorReporter().setReportSender(new HockeySender());
		
		NetworkMgr.createInstance(getApplicationContext());
		CamSettingMgr.createInstance(getApplicationContext());
		CamSettingMgr.getInstance().addCamID(getApplicationContext(), TMP_CAM_ID);
		
		//startService(new Intent(this,BeseyeNotificationService.class));
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

}
