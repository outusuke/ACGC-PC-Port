#ifndef PC_ANDROID_ANSI_DECLS_H
#define PC_ANDROID_ANSI_DECLS_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* The decomp calls these without declaring them; Clang rejects implicit declarations. */
int bcmp(const void* s1, const void* s2, size_t n);
void bcopy(const void* src, void* dst, size_t n);
void bzero(void* s, size_t n);

#ifdef __cplusplus
}
#endif

#endif
