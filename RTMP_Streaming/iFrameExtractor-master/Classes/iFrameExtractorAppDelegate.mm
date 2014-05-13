//
//  iFrameExtractorAppDelegate.m
//  iFrameExtractor
//
//  Created by lajos on 1/8/10.
//
//  Copyright 2010 Lajos Kamocsay
//
//  lajos at codza dot com
//
//  iFrameExtractor is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
// 
//  iFrameExtractor is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//

#import "iFrameExtractorAppDelegate.h"
#import "VideoFrameExtractor.h"
#import "Utilities.h"
#import <AVFoundation/AVFoundation.h>

#import "utils.h"
#import "ffmpeg_ext.h"
#import "beseyeplayer.h"
#import "beseye_audio_streamer.h"

#include <zxing/common/reedsolomon/GenericGF.h>
#include <zxing/common/reedsolomon/ReedSolomonEncoder.h>
#include "sp_config.h"
#include "beseye_sound_pairing.h"
#include "FreqGenerator.h"
#include "soundpairing_error.h"

#import "SDL_config.h"
/* import the SDL main definition header */
#import "SDL_main.h"

#import <libavcodec/avcodec.h>
#import <libavformat/avformat.h>
#import <libswscale/swscale.h>
#import <libavformat/url.h>
#import <librtmp/log.h>

//struct CPPMembers {
//    CBeseyePlayer member1;
//};

@implementation iFrameExtractorAppDelegate

@synthesize window, imageView, imageView2, label, playButton, playButton2, talkButton, stopButton, stopButton2, stopButton3,addButton,playToneButton, video;

- (void)dealloc {
	[video release];
	[imageView release];
    [imageView2 release];
	[label release];
	[playButton release];
    [playButton2 release];
    [talkButton release];
    [stopButton release];
    [stopButton2 release];
    [stopButton3 release];
    [addButton release];
    [playToneButton release];
    [window release];
    [super dealloc];
}

void rtmp_log_internal2(int level, const char *msg){
	if(NULL != msg && level <=RTMP_LOGINFO)
        NSLog(@"%s",msg);
}

- (void)applicationDidFinishLaunching:(UIApplication *)application {    
//	self.video = [[VideoFrameExtractor alloc] initWithVideo:@"rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/sample.mp4"];
//    //@"http://live.3gv.ifeng.com/live/zixun.m3u8?fmt=x264_512k_mpegts&size=320x240"];
//                  //@"rtsp://media2.tripsmarter.com/LiveTV/BTV/"];
//                  //@"rtmp://live.goldia.cn/live/livestream"];
//                  //[Utilities bundlePath:@"sophie.mov"]];
//	[video release];
//
//	// set output image size
//	video.outputWidth = 320;
//	video.outputHeight = 240;
//	
//	// print some info about the video
//	NSLog(@"video duration: %f",video.duration);
//	NSLog(@"video size: %d x %d", video.sourceWidth, video.sourceHeight);
	
	// video images are landscape, so rotate image view 90 degrees
	//[imageView setTransform:CGAffineTransformMakeRotation(M_PI/2)];

    av_log_set_level(AV_LOG_INFO);
    
    RTMP_LogSetLevel(RTMP_LOGINFO);
	RTMP_LogSetCallback2(rtmp_log_internal2);
    
    [playButton setEnabled:YES];
    [stopButton setEnabled:NO];
    [addButton setEnabled:NO];
    
    [playButton2 setEnabled:YES];
    [stopButton2 setEnabled:NO];

    [talkButton setEnabled:YES];
    [stopButton3 setEnabled:NO];

    [window makeKeyAndVisible];
}

- (void)enableButton:(id)button {
    [button setEnabled:YES];
}

- (void)disableButton:(id)button {
    [button setEnabled:NO];
}

CBeseyePlayer* player1, *player2;
const char* vod_path = "mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_%d.mp4";
int idx = 0;

-(void) dispatchUI:(NSArray*)arr{
    [self performSelectorOnMainThread:@selector(drawImage:) withObject:arr waitUntilDone:FALSE];
}

void videoCallback(void* anw, uint8_t* srcbuf, uint32_t iFormat, uint32_t linesize, uint32_t iWidth, uint32_t iHeight){
    CGBitmapInfo bitmapInfo = kCGBitmapByteOrderDefault;
	CFDataRef data = CFDataCreateWithBytesNoCopy(kCFAllocatorDefault, srcbuf,linesize*iHeight,kCFAllocatorNull);
    
	CGDataProviderRef provider = CGDataProviderCreateWithCFData(data);
	CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
	CGImageRef cgImage = CGImageCreate(iWidth,
									   iHeight,
									   8,
									   24,
									   linesize,
									   colorSpace,
									   bitmapInfo,
									   provider,
									   NULL,
									   NO,
									   kCGRenderingIntentDefault);
	CGColorSpaceRelease(colorSpace);
    
	UIImage *image = [UIImage imageWithCGImage:cgImage];
	
    CGImageRelease(cgImage);
	CGDataProviderRelease(provider);
	CFRelease(data);
    
    UIImageView *iv = (UIImageView *)anw;
    //!!! Large memory usage here, need find another way to copy the image buffer instead of setImage
    [iv performSelectorOnMainThread:@selector(setImage:) withObject:image waitUntilDone:FALSE];
}

//Called from destructor
void videoDeinitCallback(void* anw){
    //handle display window destruction here if necessary
}

void rtmpStreamStatusCb(CBeseyeRTMPObserver * obj, CBeseyeRTMPObserver::Player_Callback cbType, const char * msg, int iMainType, int iDetailType){
    if(obj == player1 || obj == player2){
        NSLog(@"rtmpStreamStatusCb(), %s cbType:%d, msg:%s, iMainType:%d, iDetailType:%d",(obj == player1?"player1":"player2"),cbType, (msg?msg:""),iMainType, iDetailType);
        if(cbType == CBeseyeRTMPObserver::STREAM_STATUS_CB){
            
        }else if(cbType == CBeseyeRTMPObserver::ERROR_CB){
            
        }
    }else if(obj == &CBeseyeAudioStreamer::getInstance()){
        NSLog(@"rtmpStreamStatusCb(), audio Streamer cbType:%d, msg:%s, iMainType:%d, iDetailType:%d",cbType, (msg?msg:""),iMainType, iDetailType);
    }
}

- (void) playToneCBWrapper:(FreqGenerator::Play_Tone_Status) status withCode:(const char *) code withType:(int) iType{

}

void playToneCB(void* userData, FreqGenerator::Play_Tone_Status status, const char * code, unsigned int iType){
    NSLog(@"playToneCB(), Play_Tone_Status:%d, code:%s",status, code);
    UIButton *playToneButton = (UIButton *)userData;
    if(status == FreqGenerator::PLAY_TONE_BEGIN){
        [playToneButton setEnabled:NO];
        //[playToneButton.self performSelectorOnMainThread:@selector(disableButton:) withObject:playToneButton waitUntilDone:NO];
    }else if(status == FreqGenerator::PLAY_TONE_END){
        [playToneButton setEnabled:YES];
        //[playToneButton. performSelectorOnMainThread:@selector(enableButton:) withObject:playToneButton waitUntilDone:NO];
    }
}

-(IBAction)playButtonAction:(id)sender {
    
    if (sender == playButton) {
        NSThread* ticketsThreadone = [[NSThread alloc] initWithTarget:self selector:@selector(playLive1) object:nil];
        [ticketsThreadone setName:@"Thread-Live1"];
        [ticketsThreadone start];
        
        [stopButton setEnabled:YES];
    }else if (sender == playButton2) {
        NSThread* ticketsThreadone = [[NSThread alloc] initWithTarget:self selector:@selector(playLive2) object:nil];
        [ticketsThreadone setName:@"Thread-Live2"];
        [ticketsThreadone start];
    }else if (sender == addButton) {
        if(player2){
            char path[256]={};
            sprintf(path, vod_path, ++idx);
            player2->addStreamingPath(path);
        }
    }else if (sender == talkButton) {
        [talkButton setEnabled:NO];
        NSThread* ticketsThreadtwo = [[NSThread alloc] initWithTarget:self selector:@selector(runAudioStreamer) object:nil];
        [ticketsThreadtwo setName:@"Thread-Talk"];
        [ticketsThreadtwo start];
    }else if (sender == stopButton) {
        if(player1){
            player1->closeStreaming();
        }
    }else if (sender == stopButton2) {
        if(player2){
            player2->closeStreaming();
        }
    }else if (sender == stopButton3) {
        CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
        if(!audioStreamer.checkExit()){
            audioStreamer.closeAudioStreaming();
        }
    }else if(sender == playToneButton){
        SoundPair_Config::init();
        FreqGenerator::getInstance()->setOnPlayToneCallback(playToneCB, playToneButton);
        const char* code = "0123456789abcdef";
        //if(FreqGenerator::getInstance()->playCode2(code, true)){
        
        //macAddr => Hex values w/o ':' in lower case (ex. ef01cd45ab89)
        //wifiKey => ASCII values (for WEP: 5 or 13 digits; WPA/WPA2: more than 8 digits)
        //secType => 0:none; 1:WEP; 2:WPA; 3:WPA2
        //tmpUserToken => temp user token from Account BE
        
        unsigned int iRet =FreqGenerator::getInstance()->playPairingCode("313233343536", "0630BesEye", 12345);
        if(R_OK == iRet){
            //[playToneButton setEnabled:NO];
            
            //important: call functions below to release resources
            //FreqGenerator::getInstance()->stopPlay2();
            //SoundPair_Config::uninit();
        }else if(E_FE_MOD_SP_INVALID_MACADDR == iRet){
            NSLog(@"invalid mac addr");
        }else if(E_FE_MOD_SP_INVALID_WIFI_KEY == iRet){
            NSLog(@"invalid wifi key");
        }else if(E_FE_MOD_SP_INVALID_SEC_TYPE == iRet){
            NSLog(@"invalid sec type");
        }else if(E_FE_MOD_SP_PLAY_CODE_ERR == iRet){
            NSLog(@"error to play tone");
        }else{
            NSLog(@"failed to play code, code:%s, iRet:%d", code, iRet);
        }
    }

//    
//	// seek to 0.0 seconds
//	[video seekTime:0.0];
//
//	[NSTimer scheduledTimerWithTimeInterval:1.0/30
//									 target:self
//								   selector:@selector(displayNextFrame:)
//								   userInfo:nil
//									repeats:YES];
}

- (void)playLive1{
    if(!player1){
        player1 = new CBeseyePlayer(imageView, PIX_FMT_RGB24, imageView.bounds.size.width, imageView.bounds.size.height);
        player1->registerVideoCallback(videoCallback, videoDeinitCallback);
        player1->registerCallback(rtmpStreamStatusCb);
        
        [self performSelectorOnMainThread:@selector(disableButton:) withObject:playButton waitUntilDone:NO];
        [self performSelectorOnMainThread:@selector(enableButton:) withObject:stopButton waitUntilDone:NO];
        
        player1->createStreaming(//"rtsp://54.250.149.50:554/live-origin/_definst_/mystream7_aac"
                                 //"rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/sample.mp4"
                                 "rtsp://admin:password@192.168.3.100/h264"
                                 );
        player1->unregisterVideoCallback();
        delete player1;
        player1= NULL;

        [self performSelectorOnMainThread:@selector(disableButton:) withObject:stopButton waitUntilDone:NO];
        [self performSelectorOnMainThread:@selector(enableButton:) withObject:playButton waitUntilDone:NO];
    }else{
        NSLog(@"playLive1(), player1 is not null");
    }
}

- (void)playLive2{
    if(!player2){
        player2 = new CBeseyePlayer(imageView2, PIX_FMT_RGB24, imageView2.bounds.size.width, imageView2.bounds.size.height);
        player2->registerVideoCallback(videoCallback, videoDeinitCallback);
        player2->registerCallback(rtmpStreamStatusCb);
        idx=0;
        
        [self performSelectorOnMainThread:@selector(disableButton:) withObject:playButton2 waitUntilDone:NO];
        [self performSelectorOnMainThread:@selector(enableButton:) withObject:stopButton2 waitUntilDone:NO];
        [self performSelectorOnMainThread:@selector(enableButton:) withObject:addButton waitUntilDone:NO];
        
        player2->createStreaming("rtsp://54.250.149.50:554/live-origin/_definst_/mystream7_aac"
                                 //"rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4"
                                 );
        player2->unregisterVideoCallback();
        delete player2;
        player2= NULL;

        [self performSelectorOnMainThread:@selector(enableButton:) withObject:playButton2 waitUntilDone:NO];
        [self performSelectorOnMainThread:@selector(disableButton:) withObject:stopButton2 waitUntilDone:NO];
        [self performSelectorOnMainThread:@selector(disableButton:) withObject:addButton waitUntilDone:NO];

    }else{
        NSLog(@"playLive2(), player2 is not null");
    }
}

NSFileHandle *writeHandle;
NSPipe* mPipe=NULL;

void pipeDeinitCallback(void){
    NSLog(@"pipeDeinitCallback");
    if(mPipe){
        [mPipe release];
        mPipe = NULL;
    }
}

- (void)runAudioStreamer{
    
//    AVAudioRecorder *audioRecorder;
//    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
//    [audioSession setCategory:AVAudioSessionCategoryRecord error:nil];
//    
//    NSMutableDictionary *recordSettings = [[NSMutableDictionary alloc] initWithCapacity:10];
//    [recordSettings setObject:[NSNumber numberWithInt: kAudioFormatLinearPCM] forKey: AVFormatIDKey];
//    [recordSettings setObject:[NSNumber numberWithFloat:16000.0] forKey: AVSampleRateKey];
//    [recordSettings setObject:[NSNumber numberWithInt:1] forKey:AVNumberOfChannelsKey];
//    [recordSettings setObject:[NSNumber numberWithInt:16] forKey:AVLinearPCMBitDepthKey];
//    [recordSettings setObject:[NSNumber numberWithBool:NO] forKey:AVLinearPCMIsBigEndianKey];
//    [recordSettings setObject:[NSNumber numberWithBool:NO] forKey:AVLinearPCMIsFloatKey];
//    
//    //NSURL *url = [NSURL fileURLWithPath:[NSString stringWithFormat:@"%@/recordTest.caf", [[NSBundle mainBundle] resourcePath]]];
//    NSURL *url = [NSURL fileURLWithPath:[NSString stringWithFormat:@"%@/recordTest.caf", [[NSBundle mainBundle] resourcePath]]];
//
//    NSError *error = nil;
//    audioRecorder = [[ AVAudioRecorder alloc] initWithURL:url settings:recordSettings error:&error];
//    
//    if ([audioRecorder prepareToRecord] == YES){
//        [audioRecorder record];
//    }else {
//        int errorCode = CFSwapInt32HostToBig ([error code]);
//        NSLog(@"Error: %@ [%4.4s])" , [error localizedDescription], (char*)&errorCode);
//        
//    }
//    NSLog(@"recording");

    CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
    if(CBeseyeAudioStreamer::checkExit()){
        mPipe = [[NSPipe alloc] init];
        NSFileHandle *writeHandle = [mPipe fileHandleForWriting];
        NSFileHandle *readHandle = [mPipe fileHandleForReading];
        
        int    mPipeRFD = [readHandle fileDescriptor];
        int    mPipeWFD = [writeHandle fileDescriptor];
        NSLog(@"runAudioStreamer: %d, %d",mPipeRFD, mPipeWFD);
        
            SDL_AudioSpec wanted_spec, spec;
            wanted_spec.channels = 1;
            wanted_spec.freq = 16000;
            wanted_spec.format = AUDIO_S16SYS;
            wanted_spec.silence = 0;
            wanted_spec.samples = SDL_AUDIO_BUFFER_SIZE;
            wanted_spec.callback = sdl_audio_record_callback;
            wanted_spec.userdata = writeHandle;//opaque;
        
            SDL_Init(SDL_INIT_AUDIO);
            if (!SDL_WasInit(SDL_INIT_AUDIO)) {
                if (SDL_InitSubSystem(SDL_INIT_AUDIO) < 0) {
                    NSLog(@"Failed to SDL_INIT_AUDIO: %s\n", SDL_GetError());
                }
            }
        
            int dev = SDL_OpenAudioDevice(NULL, 1, &wanted_spec, &wanted_spec, SDL_AUDIO_ALLOW_FORMAT_CHANGE);
            if (dev == 0) {
                //printf("Failed to open audio: %s\n", SDL_GetError());
                NSLog(@"Failed to open audio: %s\n", SDL_GetError());
            } else {
        
           }
        
        
        if(audioStreamer.setStreamingInfo("rtmp://192.168.2.224:1935/myapp/audiostream", mPipeRFD, mPipeWFD, pipeDeinitCallback)){
            NSThread* audioFeedThread = [[NSThread alloc] initWithTarget:self selector:@selector(runAudioFeed:) object:(writeHandle)];
            [audioFeedThread setName:@"audioFeedThread"];
            [audioFeedThread start];
            
            audioStreamer.registerCallback(rtmpStreamStatusCb);
            [self performSelectorOnMainThread:@selector(disableButton:) withObject:talkButton waitUntilDone:NO];
            [self performSelectorOnMainThread:@selector(enableButton:) withObject:stopButton3 waitUntilDone:NO];
            audioStreamer.startAudioStreaming();
            
            [self performSelectorOnMainThread:@selector(enableButton:) withObject:talkButton waitUntilDone:NO];
            [self performSelectorOnMainThread:@selector(disableButton:) withObject:stopButton3 waitUntilDone:NO];
        }
    }
}

 - (void)runAudioFeed:(id)arg {
    //NSFileHandle *writeHandle = (NSFileHandle *)arg;
     int iSize = 512;
    char* msg = (char*)malloc(sizeof(char)*iSize);
    //memset(msg, 128, 512);
    //NSData* data = [[NSData alloc] initWithBytes:(msg) length:(100)];
    while (TRUE) {
        //[writeHandle writeData:(data)];
        //NSLog(@"runAudioFeed() in\n");
        CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
        if(STREAM_CLOSE == audioStreamer.get_Stream_Status()){
            break;
        }
        audioStreamer.writeAudioBuffer(msg, iSize);
        //NSLog(@"runAudioFeed() sleep\n");
        //sleep(10);
        SDL_Delay(10);
        //NSLog(@"runAudioFeed() out\n");
    }
}

void sdl_audio_record_callback(void *opaque, Uint8 *stream, int len){
    NSLog(@"sdl_audio_record_callback(), %d\n", len);
    CBeseyeAudioStreamer& audioStreamer = CBeseyeAudioStreamer::getInstance();
    audioStreamer.writeAudioBuffer((char*)stream, len);
}

- (IBAction)showTime:(id)sender {
    NSLog(@"current time: %f s",video.currentTime);
}

#define LERP(A,B,C) ((A)*(1.0-C)+(B)*C)

-(void)displayNextFrame:(NSTimer *)timer {
	NSTimeInterval startTime = [NSDate timeIntervalSinceReferenceDate];
	if (![video stepFrame]) {
		[timer invalidate];
		[playButton setEnabled:YES];
		return;
	}
	imageView.image = video.currentImage;
	float frameTime = 1.0/([NSDate timeIntervalSinceReferenceDate]-startTime);
	if (lastFrameTime<0) {
		lastFrameTime = frameTime;
	} else {
		lastFrameTime = LERP(frameTime, lastFrameTime, 0.8);
	}
	[label setText:[NSString stringWithFormat:@"%.0f",lastFrameTime]];
}

@end
