/*****************************************************************************
 * error.h  error code head file
 *
 *   COPYRIGHT (C) 2013 BesEye Co.
 *   ALL RIGHTS RESERVED.
 *
 *   Revision History:
 *    03/25/2014 - Abner Huang - Created.
 *
 *****************************************************************************/
#ifndef _ERROR_H_
#define _ERROR_H_
#ifdef __cplusplus
extern "C" {
#endif


////Return Code definitions begin
static const int RET_CODE_OK 					= 0;
static const int RET_CODE_UNKNOWN_ERR 			= 0x00200001;
static const int RET_CODE_INTERNAL_ERR 			= 0x00200002;
static const int RET_CODE_INVALID_INPUT_ERR 	= 0x00200003;
static const int RET_CODE_NETWORK_ERR 			= 0x00200004;
static const int RET_CODE_AUTHEN_ERR 			= 0x00200005;
static const int RET_CODE_NOT_IMPLEMENT_ERR 	= 0x00200006;
static const int RET_CODE_SERVICE_ALREADY_ON 	= 0x00200007;

////Return Code definitions end

////API_ERR_MODULE definitions begin
static const int API_ERR_MODULE_NONE			=  0;

static const int API_ERR_MODULE_CAM_BE			=  1001;

static const int API_ERR_MODULE_NET_CURL		=  3001;
static const int API_ERR_MODULE_NET_OTHER		=  3002;
////API_ERR_MODULE definitions end

#ifdef __cplusplus
}
#endif
#endif /* _ERROR_H_ */
