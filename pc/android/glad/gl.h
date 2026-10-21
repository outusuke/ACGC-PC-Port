#ifndef PC_ANDROID_GLAD_GL_H
#define PC_ANDROID_GLAD_GL_H

#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>

#ifdef __cplusplus
extern "C" {
#endif
void pc_android_shader_source(GLuint shader, GLsizei count, const GLchar* const* string, const GLint* length);
#ifdef __cplusplus
}
#endif

#define glShaderSource pc_android_shader_source

#define glClearDepth(d)    glClearDepthf((GLfloat)(d))
#define glDepthRange(n, f) glDepthRangef((GLfloat)(n), (GLfloat)(f))
/* GLES has no fixed point size; points are sized by gl_PointSize in the shader. */
#define glPointSize(s)     ((void)(s))

#define glad_glUniform1i        glUniform1i
#define glad_glUniform2i        glUniform2i
#define glad_glUniform3i        glUniform3i
#define glad_glUniform4i        glUniform4i
#define glad_glUniform1f        glUniform1f
#define glad_glUniform2f        glUniform2f
#define glad_glUniform3f        glUniform3f
#define glad_glUniform4f        glUniform4f
#define glad_glUniform1iv       glUniform1iv
#define glad_glUniform2iv       glUniform2iv
#define glad_glUniform3iv       glUniform3iv
#define glad_glUniform4iv       glUniform4iv
#define glad_glUniform3fv       glUniform3fv
#define glad_glUniform4fv       glUniform4fv
#define glad_glUniformMatrix3fv glUniformMatrix3fv
#define glad_glUniformMatrix4fv glUniformMatrix4fv

#endif
