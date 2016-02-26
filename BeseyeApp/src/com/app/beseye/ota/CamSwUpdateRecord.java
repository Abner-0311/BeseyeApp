package com.app.beseye.ota;

import static com.app.beseye.util.BeseyeConfig.TAG;
import android.util.Log;

import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_ERROR;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_GROUP;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_STATUS;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_VER_CHECK_STATUS;

public class CamSwUpdateRecord{
	static private final int MAX_RETRY_CNT_FOR_POOR_NETWORK = 1;
	static final private long TIME_TO_SHOW_OTA_FINISH = 60*1000; // 1 min
	static final private long TIME_OF_CAM_NO_RESPONSE = 1*60*1000; //25 mins

	String mStrVCamId;
	private String mStrCamName;
	CAM_UPDATE_STATUS meUpdateStatus;
	private CAM_UPDATE_STATUS mePrevUpdateStatus;
	private CAM_UPDATE_ERROR meUpdateErrType;
	CAM_UPDATE_GROUP meUpdateGroup;
	CAM_UPDATE_VER_CHECK_STATUS meVerCheckStatus;
	
	boolean mbUpdateTriggerred;
	
	int miErrCode;
	int miUpdatePercentage;
	long mlVerCheckTs;
	long mlBeginUpdateTs;
	long mlLastCamReportTs;
	long mlLastOTAErrorTs;
	
	private int miRetryCntForPoorNetwork;
	
	private long mlLastUserFeedbackTs;
	private long mlCamOnlineAfterOTATs;
	
	BeseyeCamBEHttpTask.UpdateCamSWTask mUpdateCamSWTask;
	BeseyeCamBEHttpTask.GetCamUpdateStatusTask mGetCamUpdateStatusTask;
	
	public CamSwUpdateRecord(String strVCamId, CAM_UPDATE_GROUP eUpdateGroup, String strCamName){
		this.mStrVCamId = strVCamId;
		this.mStrCamName = strCamName;
		this.meUpdateGroup = eUpdateGroup;
		init();
	}
	
	private void init(){
		this.meUpdateStatus = mePrevUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_INIT;
		this.meUpdateErrType = CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_NONE;
		this.meVerCheckStatus = CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_INIT;
		this.mbUpdateTriggerred = false;
		this.miErrCode = 0;
		this.miUpdatePercentage = 0;
		this.miRetryCntForPoorNetwork= 0;
		this.mlVerCheckTs = this.mlBeginUpdateTs = this.mlLastCamReportTs = this.mlLastUserFeedbackTs = this.mlLastOTAErrorTs = mlCamOnlineAfterOTATs = -1;
		this.mUpdateCamSWTask = null;
		this.mGetCamUpdateStatusTask = null;
	}
	
	public int getRetryCntForPoorNetwork(){
		return miRetryCntForPoorNetwork;
	}
	
	public boolean doRetryForPoorNetwork(){
		boolean bRet = false;
		if(MAX_RETRY_CNT_FOR_POOR_NETWORK > miRetryCntForPoorNetwork){
			miRetryCntForPoorNetwork++;
			bRet = true;
		}
		return bRet;
	}
	
	public boolean isReachOTANoResponseTime(){
		final long lDelta = System.currentTimeMillis() - mlLastCamReportTs;
		boolean bRet = -1 != mlLastCamReportTs && lDelta > TIME_OF_CAM_NO_RESPONSE;
		Log.i(TAG, "isReachOTANoResponseTime()["+mStrVCamId+"], lDelta = "+lDelta+", bRet:"+bRet);
		return bRet;
	}
	
	public boolean isInOTAFinishPeriod(){//Within 1 min since finishing ota
		final long lDelta = System.currentTimeMillis() - mlLastCamReportTs;
		boolean bRet = lDelta < TIME_TO_SHOW_OTA_FINISH;
		Log.i(TAG, "isInOTAFinishPeriod()["+mStrVCamId+"], lDelta = "+lDelta+", bRet:"+bRet);
		return bRet;
	}
	
	public CAM_UPDATE_STATUS getUpdateStatus(){
		return meUpdateStatus;
	}
	
	public CAM_UPDATE_STATUS getPrevUpdateStatus(){
		return mePrevUpdateStatus;
	}
	
	public boolean changeUpdateStatus(CAM_UPDATE_STATUS newUpdateStatus){
		boolean bRet = !newUpdateStatus.equals(meUpdateStatus);
		if(bRet){
			mePrevUpdateStatus = meUpdateStatus;
			meUpdateStatus = newUpdateStatus;
			//Log.i(TAG, "changeUpdateStatus()["+mStrVCamId+"], from = "+mePrevUpdateStatus+" to "+meUpdateStatus);
			
			if(meUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING)){
				setCamOnlineAfterOTATs(-1);
				//setLastOTAErrorTs(-1);
			}else if(meUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_FINISH)){
				miRetryCntForPoorNetwork = 0;
			}
			
			BeseyeCamSWVersionMgr.getInstance().broadcastOnCamUpdateStatusChanged(mStrVCamId, meUpdateStatus, mePrevUpdateStatus, this);
		}
		return bRet;
	}

	public String getVCamId() {
		return mStrVCamId;
	}

	public void setVCamId(String mStrVCamId) {
		this.mStrVCamId = mStrVCamId;
	}

	public String getCamName() {
		return mStrCamName;
	}

	public void setCamName(String mStrCamName) {
		this.mStrCamName = mStrCamName;
	}

	public CAM_UPDATE_ERROR getUpdateErrType() {
		return meUpdateErrType;
	}

	public void setUpdateErrType(CAM_UPDATE_ERROR meUpdateErrType) {
		this.meUpdateErrType = meUpdateErrType;
	}

	public CAM_UPDATE_GROUP getUpdateGroup() {
		return meUpdateGroup;
	}

	public void setUpdateGroup(CAM_UPDATE_GROUP meUpdateGroup) {
		this.meUpdateGroup = meUpdateGroup;
	}

	public CAM_UPDATE_VER_CHECK_STATUS getVerCheckStatus() {
		return meVerCheckStatus;
	}

	public void setVerCheckStatus(CAM_UPDATE_VER_CHECK_STATUS meVerCheckStatus) {
		this.meVerCheckStatus = meVerCheckStatus;
	}
	
	public boolean isOTATriggerredByThisDev(){
		return mbUpdateTriggerred;
	}

	public void setUpdateTriggerred(boolean mbUpdateTriggerred) {
		this.mbUpdateTriggerred = mbUpdateTriggerred;
	}

	public int getErrCode() {
		return miErrCode;
	}

	public void setErrCode(int miErrCode) {
		this.miErrCode = miErrCode;
	}

	public int getUpdatePercentage() {
		return miUpdatePercentage;
	}

	public void setUpdatePercentage(int miUpdatePercentage) {
		this.miUpdatePercentage = miUpdatePercentage;
	}

	public long getVerCheckTs() {
		return mlVerCheckTs;
	}

	public void setVerCheckTs(long mlVerCheckTs) {
		this.mlVerCheckTs = mlVerCheckTs;
	}

	public long getBeginUpdateTs() {
		return mlBeginUpdateTs;
	}

	public void setBeginUpdateTs(long mlBeginUpdateTs) {
		this.mlBeginUpdateTs = mlBeginUpdateTs;
	}

	public long getLastCamReporteTs() {
		return mlLastCamReportTs;
	}

	public void setLastCamReporteTs(long mlLastCamReporteTs) {
		this.mlLastCamReportTs = mlLastCamReporteTs;
	}

	public long getLastOTAErrorTs() {
		return mlLastOTAErrorTs;
	}

	public void setLastOTAErrorTs(long mlLastOTAErrorTs) {
		this.mlLastOTAErrorTs = mlLastOTAErrorTs;
	}

	public long getLastUserFeedbackTs() {
		return mlLastUserFeedbackTs;
	}

	public void setLastUserFeedbackTs(long mlLastUserFeedbackTs) {
		this.mlLastUserFeedbackTs = mlLastUserFeedbackTs;
	}
	
	public void setOTAFeedbackSent(){
		setLastUserFeedbackTs(System.currentTimeMillis());
		setErrCode(0);
		miRetryCntForPoorNetwork = 0;
	}
	
	public void resetErrorInfo(){
		setErrCode(0);
	}
	
	public boolean isOTAFeedbackSent(){
		return -1 != mlLastUserFeedbackTs && mlLastCamReportTs < mlLastUserFeedbackTs;
	}
	
	public boolean isCamOnlineAfterOTA() {
		return -1 != mlCamOnlineAfterOTATs && mlLastCamReportTs < mlCamOnlineAfterOTATs;
	}

	public void setCamOnlineAfterOTATs(long mlCamOnlineAfterOTATs) {
		this.mlCamOnlineAfterOTATs = mlCamOnlineAfterOTATs;
	}
	
	public boolean isRebootErrWhenOTAPrepare(){
		return miErrCode == BeseyeError.E_REBOOT_DURING_PREPARING_STAGE;
	}
	
	public boolean isPoorNetworkErrWhenOTA(){
		return miErrCode == BeseyeError.E_CURL_NETWORK_DEAD || 
			   miErrCode == BeseyeError.E_CURL_NETWORK_ERROR || 
			   miErrCode == BeseyeError.E_NETWORK_ERROR_WHEN_DOWNLOADING_FILE;
	}

	@Override
	public String toString() {
		return "CamSwUpdateRecord [mStrVCamId=" + mStrVCamId + ", mStrCamName="
				+ mStrCamName + ", meUpdateStatus=" + meUpdateStatus
				+ ", mePrevUpdateStatus=" + mePrevUpdateStatus
				+ ", meUpdateErrType=" + meUpdateErrType + ", meUpdateGroup="
				+ meUpdateGroup + ", meVerCheckStatus=" + meVerCheckStatus
				+ ", mbUpdateTriggerred=" + mbUpdateTriggerred + ", miErrCode="
				+ miErrCode + ", miUpdatePercentage=" + miUpdatePercentage
				+ ", mlVerCheckTs=" + mlVerCheckTs + ", mlBeginUpdateTs="
				+ mlBeginUpdateTs + ", mlLastCamReportTs=" + mlLastCamReportTs
				+ ", mlLastOTAErrorTs=" + mlLastOTAErrorTs
				+ ", miRetryCntForPoorNetwork=" + miRetryCntForPoorNetwork
				+ ", mlLastUserFeedbackTs=" + mlLastUserFeedbackTs
				+ ", mlCamOnlineAfterOTATs=" + mlCamOnlineAfterOTATs
				+ ", mUpdateCamSWTask=" + mUpdateCamSWTask
				+ ", mGetCamUpdateStatusTask=" + mGetCamUpdateStatusTask + "]";
	}
}