//
//  iFrameExtractorAppDelegate.h
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

#import <UIKit/UIKit.h>
#include "FreqGenerator.h"

@class VideoFrameExtractor;

//struct CPPMembers;

@interface iFrameExtractorAppDelegate : NSObject <UIApplicationDelegate> {
    UIWindow *window;
	IBOutlet UIImageView *imageView;
    IBOutlet UIImageView *imageView2;
	IBOutlet UILabel *label;
	IBOutlet UIButton *playButton;
    IBOutlet UIButton *playButton2;
    IBOutlet UIButton *talkButton;
    IBOutlet UIButton *stopButton;
    IBOutlet UIButton *stopButton2;
    IBOutlet UIButton *stopButton3;
    IBOutlet UIButton *addButton;
    IBOutlet UIButton *playToneButton;
	VideoFrameExtractor *video;
	float lastFrameTime;
    //struct CPPMembers *_cppMembers;
}

@property (nonatomic, retain) IBOutlet UIWindow *window;
@property (nonatomic, retain) IBOutlet UIImageView *imageView;
@property (nonatomic, retain) IBOutlet UIImageView *imageView2;
@property (nonatomic, retain) IBOutlet UILabel *label;
@property (nonatomic, retain) IBOutlet UIButton *playButton;
@property (nonatomic, retain) IBOutlet UIButton *playButton2;
@property (nonatomic, retain) IBOutlet UIButton *talkButton;
@property (nonatomic, retain) IBOutlet UIButton *stopButton;
@property (nonatomic, retain) IBOutlet UIButton *stopButton2;
@property (nonatomic, retain) IBOutlet UIButton *stopButton3;
@property (nonatomic, retain) IBOutlet UIButton *addButton;
@property (nonatomic, retain) IBOutlet UIButton *playToneButton;
@property (nonatomic, retain) VideoFrameExtractor *video;

-(IBAction)playButtonAction:(id)sender;
- (IBAction)showTime:(id)sender;
- (void) playToneCBWrapper:(FreqGenerator::Play_Tone_Status) status withCode:(const char *) code withType:(int) iType;

@end

