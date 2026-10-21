#ifndef PC_ANDROID_H
#define PC_ANDROID_H

#include "dolphin/pad.h"

void pc_android_init(void);
void pc_android_apply_touch(PADStatus* pad);
void pc_android_touch_state(int* buttons, int* stickX, int* stickY);

#endif
