#include <stdio.h>
#include <string.h>

FILE *fopen$UNIX2003( const char *filename, const char *mode )
{
    return fopen(filename, mode);
}

size_t fwrite$UNIX2003( const void *a, size_t b, size_t c, FILE *d )
{
    return fwrite(a, b, c, d);
}

void fputs$UNIX2003(const char *restrict c, FILE *restrict f)
{
    fputs(c, f);
}

char *strerror$UNIX2003(int errnum)
{
    return strerror(errnum);
}