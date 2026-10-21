#include <jni.h>
#include <pthread.h>
#include <stdatomic.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <android/log.h>
#include <SDL.h>

#include "glad/gl.h"
#include "pc_android.h"

#undef glShaderSource

#define LOG_TAG "ACGC"

extern int g_pc_verbose;

typedef struct {
    int fd;
    FILE* file;
} LogSink;

static _Atomic uint64_t s_touch;

JNIEXPORT void JNICALL Java_io_github_outusuke_acgcpcport_TouchControlsView_nativeSetPad(
    JNIEnv* env, jclass cls, jint buttons, jint lx, jint ly, jint cx, jint cy) {
    uint64_t v = (uint64_t)(buttons & 0xFFFF) | (uint64_t)(lx & 0xFF) << 16 | (uint64_t)(ly & 0xFF) << 24 |
                 (uint64_t)(cx & 0xFF) << 32 | (uint64_t)(cy & 0xFF) << 40;

    (void)env;
    (void)cls;
    atomic_store(&s_touch, v);
}

void pc_android_apply_touch(PADStatus* pad) {
    uint64_t v = atomic_load(&s_touch);
    u16 buttons = (u16)(v & 0xFFFF);
    s8 lx = (s8)((v >> 16) & 0xFF);
    s8 ly = (s8)((v >> 24) & 0xFF);
    s8 cx = (s8)((v >> 32) & 0xFF);
    s8 cy = (s8)((v >> 40) & 0xFF);

    pad->button |= buttons;
    if (lx || ly) {
        pad->stickX = lx;
        pad->stickY = ly;
    }
    if (cx || cy) {
        pad->substickX = cx;
        pad->substickY = cy;
    }
    if (buttons & PAD_TRIGGER_L) {
        pad->triggerLeft = 255;
    }
    if (buttons & PAD_TRIGGER_R) {
        pad->triggerRight = 255;
    }
}

void pc_android_touch_state(int* buttons, int* stickX, int* stickY) {
    uint64_t v = atomic_load(&s_touch);

    *buttons = (int)(v & 0xFFFF);
    *stickX = (s8)((v >> 16) & 0xFF);
    *stickY = (s8)((v >> 24) & 0xFF);
}

static const char k_desktop_version[] = "#version 330 core\n";
static const char k_es_header[] =
    "#version 300 es\n"
    "precision highp float;\n"
    "precision highp int;\n"
    "precision highp sampler2D;\n";

void pc_android_shader_source(GLuint shader, GLsizei count, const GLchar* const* string, const GLint* length) {
    size_t prefix = sizeof(k_desktop_version) - 1;
    const char* src = (count == 1 && string && !length) ? string[0] : NULL;

    if (src && strncmp(src, k_desktop_version, prefix) == 0) {
        size_t header = sizeof(k_es_header) - 1;
        size_t rest = strlen(src + prefix);
        char* buf = malloc(header + rest + 1);

        if (buf) {
            const char* p = buf;

            memcpy(buf, k_es_header, header);
            memcpy(buf + header, src + prefix, rest + 1);
            glShaderSource(shader, 1, &p, NULL);
            free(buf);
            return;
        }
    }
    glShaderSource(shader, count, string, length);
}

static void* log_thread(void* arg) {
    LogSink* sink = arg;
    int fd = sink->fd;
    char buf[1024];
    size_t len = 0;
    ssize_t n;

    while ((n = read(fd, buf + len, sizeof(buf) - 1 - len)) > 0) {
        char* start = buf;
        char* nl;

        if (sink->file) {
            fwrite(buf + len, 1, (size_t)n, sink->file);
            fflush(sink->file);
        }
        len += (size_t)n;
        buf[len] = '\0';
        while ((nl = strchr(start, '\n')) != NULL) {
            *nl = '\0';
            __android_log_write(ANDROID_LOG_INFO, LOG_TAG, start);
            start = nl + 1;
        }
        len = strlen(start);
        memmove(buf, start, len + 1);
        if (len == sizeof(buf) - 1) {
            __android_log_write(ANDROID_LOG_INFO, LOG_TAG, buf);
            len = 0;
        }
    }
    return NULL;
}

static void bridge_fd(int target, FILE* file) {
    int fds[2];
    pthread_t thread;
    LogSink* sink;

    if (pipe(fds) != 0) {
        return;
    }
    sink = malloc(sizeof(*sink));
    if (!sink) {
        close(fds[0]);
        close(fds[1]);
        return;
    }
    sink->fd = fds[0];
    sink->file = file;
    dup2(fds[1], target);
    close(fds[1]);
    pthread_create(&thread, NULL, log_thread, sink);
    pthread_detach(thread);
}

static FILE* open_log(const char* dir, const char* name) {
    char path[512];

    snprintf(path, sizeof(path), "%s/%s", dir, name);
    return fopen(path, "w");
}

static void start_log_bridge(void) {
    const char* dir = getenv("ACGC_LOG_DIR");
    FILE* out = dir ? open_log(dir, "stdout.log") : NULL;
    FILE* err = dir ? open_log(dir, "stderr.log") : NULL;

    if (out || err) {
        g_pc_verbose = 1;
    }
    setvbuf(stdout, NULL, _IOLBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
    bridge_fd(STDOUT_FILENO, out);
    bridge_fd(STDERR_FILENO, err);
}

void pc_android_init(void) {
    const char* dir = SDL_AndroidGetInternalStoragePath();

    start_log_bridge();
    if (!dir || chdir(dir) != 0) {
        fprintf(stderr, "[Android] could not enter internal storage\n");
        return;
    }

    mkdir("rom", 0755);
    mkdir("save", 0755);
    mkdir("texture_pack", 0755);

    if (access("settings.ini", F_OK) != 0) {
        FILE* f = fopen("settings.ini", "w");

        if (f) {
            fputs("vsync = 1\nmsaa = 0\n", f);
            fclose(f);
        }
    }
}
