#ifndef MAIN_H
#define MAIN_H

#include "types.h"
#include "dolphin/os/OSThread.h"

#ifdef __cplusplus
extern "C" {
#endif

#define SCREEN_WIDTH 320
#define SCREEN_HEIGHT 240

#define SCREEN_WIDTH_F ((f32)SCREEN_WIDTH)
#define SCREEN_HEIGHT_F ((f32)SCREEN_HEIGHT)

extern int ScreenWidth;
extern int ScreenHeight;

extern OSThread graphThread;
extern u8 SegmentBaseAddress[0x40];

/* SDL remaps main to SDL_main on Android, which clashes with this prototype */
#if !(defined(SDL_main_h_) && defined(main))
void main(void);
#endif
extern void mainproc(void* val);

#ifdef __cplusplus
}
#endif

#endif
